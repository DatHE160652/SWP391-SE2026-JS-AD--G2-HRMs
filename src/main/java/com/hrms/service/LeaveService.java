package com.hrms.service;

import com.hrms.dao.LeavePolicyDAO;
import com.hrms.dao.LeaveRequestDAO;
import com.hrms.model.LeavePolicy;
import com.hrms.model.LeaveRequest;
import com.hrms.service.security.TenantPrincipal;
import com.hrms.util.DBConnection;
import java.sql.*;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

/** Quota validation and attendance changes share one transaction and tenant lock. */
public class LeaveService {
    public static final ZoneId TIME_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private final LeavePolicyDAO policies;
    private final LeaveRequestDAO requests;
    private final Clock clock;

    public LeaveService() { this(new LeavePolicyDAO(), new LeaveRequestDAO(), Clock.system(TIME_ZONE)); }
    public LeaveService(LeavePolicyDAO policies, LeaveRequestDAO requests, Clock clock) {
        this.policies = policies; this.requests = requests; this.clock = clock;
    }
    public LocalDate tomorrow() { return LocalDate.now(clock).plusDays(1); }
    public LeaveRequestDAO getLeaveRequestDAO() { return requests; }

    public List<LocalDate> validateDates(List<LocalDate> dates, int maximum) {
        if (dates == null || dates.isEmpty()) throw new FieldValidationException("dates", "Vui lòng chọn ngày nghỉ.");
        if (dates.size() > maximum) throw new FieldValidationException("dates", "Số ngày nghỉ vượt giới hạn mỗi đơn (" + maximum + " ngày).");
        if (dates.stream().anyMatch(Objects::isNull)) throw new FieldValidationException("dates", "Ngày nghỉ không hợp lệ.");
        if (new HashSet<>(dates).size() != dates.size()) throw new FieldValidationException("dates", "Ngày nghỉ không được trùng nhau.");
        for (LocalDate date : dates) {
            if (date.isBefore(tomorrow())) throw new FieldValidationException("dates", "Chỉ được chọn ngày nghỉ từ ngày mai.");
            if (date.getDayOfWeek() == DayOfWeek.SATURDAY || date.getDayOfWeek() == DayOfWeek.SUNDAY)
                throw new FieldValidationException("dates", "Không được chọn Thứ Bảy hoặc Chủ Nhật.");
        }
        return dates.stream().sorted().toList();
    }

    public static boolean canReview(TenantPrincipal actor, LeaveRequest request) {
        if (actor == null || request == null || !Objects.equals(actor.getCompanyId(), request.getCompanyId())
                || Objects.equals(actor.getUserId(), request.getRequesterUserId())
                || Objects.equals(actor.getEmployeeId(), request.getEmployeeId())) return false;
        return "MANAGER".equals(actor.getRoleCode()) && "EMPLOYEE".equals(request.getRequesterRole())
                && actor.getEmployeeId() != null && Objects.equals(actor.getEmployeeId(), request.getManagerId())
                || "COMPANY_ADMIN".equals(actor.getRoleCode()) && "MANAGER".equals(request.getRequesterRole());
    }

    public LeavePolicy getPolicy(TenantPrincipal actor) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) { return policies.getPolicy(conn, actor.getCompanyId()); }
    }

    public void savePolicy(TenantPrincipal actor, String mode, int days, int maximum) throws SQLException {
        requireRole(actor, "COMPANY_ADMIN");
        LeavePolicy policy = new LeavePolicy(actor.getCompanyId(), mode, days, maximum);
        transaction(actor.getCompanyId(), conn -> { policies.savePolicy(conn, policy, actor.getUserId()); return null; });
    }

    public LeaveRequest submit(TenantPrincipal actor, List<LocalDate> dates, String reason) throws SQLException {
        requireSubmitter(actor);
        if (reason == null || reason.isBlank() || reason.trim().length() > 500)
            throw new FieldValidationException("reason", "Lý do phải có từ 1 đến 500 ký tự.");
        return transaction(actor.getCompanyId(), conn -> {
            requireEmployee(conn, actor);
            LeavePolicy policy = requirePolicy(conn, actor.getCompanyId());
            List<LocalDate> selected = validateDates(dates, policy.maxDaysPerRequest());
            // Pending requests do not reserve or consume quota. Quota is checked atomically at approval.
            for (LocalDate date : selected) {
                if (requests.hasOverlap(conn, actor.getCompanyId(), actor.getEmployeeId(), date, 0))
                    throw new FieldValidationException("dates", "Ngày " + date + " đã có đơn được duyệt.");
            }
            LeaveRequest request = new LeaveRequest();
            request.setCompanyId(actor.getCompanyId()); request.setEmployeeId(actor.getEmployeeId());
            request.setDates(selected); request.setReason(reason.trim());
            requests.insertRequest(conn, request);
            return requests.getRequest(conn, actor.getCompanyId(), request.getLeaveRequestId());
        });
    }

    public LeaveRequest detail(TenantPrincipal actor, int id) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            LeaveRequest request = requiredRequest(conn, actor, id);
            if (!Objects.equals(actor.getEmployeeId(), request.getEmployeeId()) && !canReview(actor, request)
                    && !"COMPANY_ADMIN".equals(actor.getRoleCode()))
                throw new SecurityException("Bạn không có quyền xem đơn này.");
            return request;
        }
    }

    public List<LeaveRequest> list(TenantPrincipal actor, boolean own, boolean pending, String status,
            LeaveRequestDAO.DateFilter filter, int page) throws SQLException {
        if (own) requireSubmitter(actor);
        else if (!Set.of("MANAGER", "COMPANY_ADMIN").contains(actor.getRoleCode())) throw new SecurityException("Không có quyền xem danh sách duyệt.");
        try (Connection conn = DBConnection.getConnection()) {
            return requests.list(conn, actor.getCompanyId(), own ? actor.getEmployeeId() : null,
                    !own && "MANAGER".equals(actor.getRoleCode()) ? requiredEmployeeId(actor) : null,
                    !own && pending && "COMPANY_ADMIN".equals(actor.getRoleCode()),
                    pending ? "Pending" : status, filter, (page - 1) * 20, 21);
        }
    }

    public void review(TenantPrincipal actor, int id, boolean approved, String comment) throws SQLException {
        if (comment != null && comment.length() > 300) throw new FieldValidationException("comment", "Phản hồi tối đa 300 ký tự.");
        if (!approved && (comment == null || comment.isBlank())) throw new FieldValidationException("comment", "Vui lòng nhập lý do từ chối.");
        transaction(actor.getCompanyId(), conn -> {
            LeaveRequest request = requiredRequest(conn, actor, id);
            if (!canReview(actor, request)) throw new SecurityException("Bạn không được duyệt đơn của nhân viên này.");
            if (!"Pending".equals(request.getStatus())) throw new IllegalArgumentException("Đơn đã được xử lý.");
            if (approved) {
                if (request.isLegacy()) throw new IllegalArgumentException("Đơn lịch sử chỉ để tra cứu. Hãy tạo đơn mới theo quota chung.");
                LeavePolicy policy = requirePolicy(conn, actor.getCompanyId());
                List<LocalDate> dates = request.getDates();
                if (dates.isEmpty() || dates.size() > policy.maxDaysPerRequest()) throw new IllegalArgumentException("Đơn vượt giới hạn hiện tại hoặc thiếu ngày nghỉ.");
                checkQuota(conn, policy, request.getEmployeeId(), dates);
                for (LocalDate date : dates) {
                    requests.ensureTimesheetOpen(conn, actor.getCompanyId(), date);
                    if (requests.hasOverlap(conn, actor.getCompanyId(), request.getEmployeeId(), date, id))
                        throw new IllegalArgumentException("Ngày " + date + " đã có đơn được duyệt.");
                    requests.syncAttendanceOnLeave(conn, actor.getCompanyId(), request.getEmployeeId(), date, request.getTitle());
                }
            }
            if (!requests.updateStatus(conn, actor.getCompanyId(), id, approved ? "Approved" : "Rejected", actor.getUserId(), comment))
                throw new IllegalArgumentException("Đơn đã được xử lý.");
            return null;
        });
    }

    public void cancel(TenantPrincipal actor, int id) throws SQLException {
        requireSubmitter(actor);
        transaction(actor.getCompanyId(), conn -> {
            LeaveRequest request = requiredRequest(conn, actor, id);
            if (!Objects.equals(actor.getEmployeeId(), request.getEmployeeId())) throw new SecurityException("Chỉ được hủy đơn của chính mình.");
            if (!requests.updateStatus(conn, actor.getCompanyId(), id, "Cancelled", actor.getUserId(), null))
                throw new IllegalArgumentException("Chỉ được hủy đơn đang chờ duyệt.");
            return null;
        });
    }

    private void checkQuota(Connection conn, LeavePolicy policy, int employeeId, List<LocalDate> dates) throws SQLException {
        Map<YearMonth, Long> months = dates.stream().collect(Collectors.groupingBy(YearMonth::from, TreeMap::new, Collectors.counting()));
        for (var entry : months.entrySet()) {
            int used = requests.usedDays(conn, policy.companyId(), employeeId, entry.getKey());
            if (used + entry.getValue() > policy.daysForMonth(entry.getKey().getMonthValue()))
                throw new FieldValidationException("dates", "Không đủ quota tháng " + entry.getKey() + ". Còn "
                        + Math.max(0, policy.daysForMonth(entry.getKey().getMonthValue()) - used) + " ngày.");
        }
    }

    private LeavePolicy requirePolicy(Connection conn, int companyId) throws SQLException {
        LeavePolicy policy = policies.getPolicy(conn, companyId);
        if (policy == null) throw new IllegalArgumentException("Công ty chưa cấu hình quota nghỉ phép. Vui lòng liên hệ Company Admin.");
        return policy;
    }
    private LeaveRequest requiredRequest(Connection conn, TenantPrincipal actor, int id) throws SQLException {
        LeaveRequest request = requests.getRequest(conn, actor.getCompanyId(), id);
        if (request == null) throw new IllegalArgumentException("Không tìm thấy đơn nghỉ phép trong công ty.");
        return request;
    }
    private void requireEmployee(Connection conn, TenantPrincipal actor) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("""
            SELECT e.ManagerId FROM Employees e JOIN Users u ON u.UserId=e.UserId AND u.CompanyId=e.CompanyId
            JOIN Roles r ON r.RoleId=u.RoleId
            WHERE e.CompanyId=? AND e.EmployeeId=? AND e.UserId=? AND r.RoleCode=? AND e.Status='Active'
            """)) {
            ps.setInt(1, actor.getCompanyId()); ps.setInt(2, actor.getEmployeeId()); ps.setInt(3, actor.getUserId()); ps.setString(4, actor.getRoleCode());
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new SecurityException("Hồ sơ nhân viên không hợp lệ.");
                if ("EMPLOYEE".equals(actor.getRoleCode()) && rs.getObject(1) == null)
                    throw new IllegalArgumentException("Chưa có quản lý trực tiếp để duyệt đơn. Vui lòng liên hệ Company Admin.");
            }
        }
    }
    private static int requiredEmployeeId(TenantPrincipal actor) {
        if (actor.getEmployeeId() == null) throw new IllegalArgumentException("Tài khoản chưa có hồ sơ nhân viên.");
        return actor.getEmployeeId();
    }
    private static void requireSubmitter(TenantPrincipal actor) {
        if (!Set.of("EMPLOYEE", "MANAGER").contains(actor.getRoleCode())) throw new SecurityException("Chỉ nhân viên và Manager được tạo đơn nghỉ.");
        requiredEmployeeId(actor);
    }
    private static void requireRole(TenantPrincipal actor, String role) {
        if (!role.equals(actor.getRoleCode())) throw new SecurityException("Bạn không có quyền thực hiện thao tác này.");
    }
    @FunctionalInterface private interface Work<T> { T run(Connection conn) throws SQLException; }
    private <T> T transaction(int companyId, Work<T> work) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                policies.lockCompany(conn, companyId);
                T result = work.run(conn);
                conn.commit();
                return result;
            } catch (SQLException | RuntimeException ex) { conn.rollback(); throw ex; }
        }
    }
}

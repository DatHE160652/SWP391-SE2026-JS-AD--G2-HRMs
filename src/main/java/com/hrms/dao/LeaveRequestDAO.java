package com.hrms.dao;

import com.hrms.model.LeaveRequest;
import com.hrms.util.DBConnection;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Lớp truy xuất cơ sở dữ liệu (DAO) cho phân hệ đơn xin nghỉ phép:
 * - Thêm mới, tra cứu chi tiết đơn kèm thông tin nhân viên, phòng ban và chính sách phép.
 * - Phân trang danh sách đơn theo nhân viên, cấp quản lý trực tiếp hoặc toàn công ty.
 * - Cập nhật trạng thái duyệt đơn (Approved / Rejected) và hủy đơn chờ duyệt.
 * - Đồng bộ bản ghi chấm công bằng câu lệnh MERGE tránh xung đột trùng lặp khóa duy nhất.
 * - Tổng hợp dữ liệu hiển thị lịch nghỉ phép và báo cáo thống kê tình hình sử dụng phép.
 */
public class LeaveRequestDAO {

    /**
     * Thêm mới một đơn xin nghỉ phép với trạng thái ban đầu là 'Pending' (Chờ duyệt).
     * Trả về mã định danh (LeaveRequestId) tự sinh trong cơ sở dữ liệu.
     */
    public int insertRequest(LeaveRequest r) throws SQLException {
        String sql = """
                INSERT INTO LeaveRequests (
                    CompanyId, EmployeeId, LeaveTypeId, StartDate, EndDate,
                    TotalDays, Reason, Status, CreatedAt
                ) VALUES (?, ?, ?, ?, ?, ?, ?, 'Pending', SYSUTCDATETIME())
                """;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, r.getCompanyId());
            ps.setInt(2, r.getEmployeeId());
            ps.setInt(3, r.getLeaveTypeId());
            ps.setDate(4, Date.valueOf(r.getStartDate()));
            ps.setDate(5, Date.valueOf(r.getEndDate()));
            ps.setBigDecimal(6, r.getTotalDays());
            ps.setString(7, r.getReason());

            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return 0;
    }

    /**
     * Lấy thông tin chi tiết một đơn xin nghỉ phép theo ID.
     * Liên kết thông tin nhân viên, phòng ban, loại nghỉ phép, người quản lý và người duyệt.
     */
    public LeaveRequest getRequestById(int leaveRequestId) throws SQLException {
        String sql = """
                SELECT lr.LeaveRequestId, lr.CompanyId, lr.EmployeeId, lr.LeaveTypeId,
                       lr.StartDate, lr.EndDate, lr.TotalDays, lr.Reason, lr.Status,
                       lr.ApproverId, lr.ApproverComment, lr.ReviewedAt, lr.CreatedAt, lr.UpdatedAt,
                       e.EmployeeCode, e.FullName AS EmployeeFullName, d.DepartmentName,
                       lt.TypeCode AS LeaveTypeCode, lt.TypeName AS LeaveTypeName, lt.IsPaid,
                       u.DisplayName AS ApproverDisplayName, m.FullName AS ManagerName
                FROM LeaveRequests lr
                JOIN Employees e ON lr.EmployeeId = e.EmployeeId
                LEFT JOIN Departments d ON e.DepartmentId = d.DepartmentId
                JOIN LeaveTypes lt ON lr.LeaveTypeId = lt.LeaveTypeId
                LEFT JOIN Users u ON lr.ApproverId = u.UserId
                LEFT JOIN Employees m ON e.ManagerId = m.EmployeeId
                WHERE lr.LeaveRequestId = ?
                """;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, leaveRequestId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapLeaveRequest(rs);
                }
            }
        }
        return null;
    }

    /**
     * Lấy danh sách đơn nghỉ phép của một nhân viên (hỗ trợ lọc theo trạng thái, năm và phân trang).
     */
    public List<LeaveRequest> getRequestsByEmployee(int employeeId, String status, Integer year, int offset, int limit) throws SQLException {
        StringBuilder sql = new StringBuilder("""
                SELECT lr.LeaveRequestId, lr.CompanyId, lr.EmployeeId, lr.LeaveTypeId,
                       lr.StartDate, lr.EndDate, lr.TotalDays, lr.Reason, lr.Status,
                       lr.ApproverId, lr.ApproverComment, lr.ReviewedAt, lr.CreatedAt, lr.UpdatedAt,
                       e.EmployeeCode, e.FullName AS EmployeeFullName, d.DepartmentName,
                       lt.TypeCode AS LeaveTypeCode, lt.TypeName AS LeaveTypeName, lt.IsPaid,
                       u.DisplayName AS ApproverDisplayName, m.FullName AS ManagerName
                FROM LeaveRequests lr
                JOIN Employees e ON lr.EmployeeId = e.EmployeeId
                LEFT JOIN Departments d ON e.DepartmentId = d.DepartmentId
                JOIN LeaveTypes lt ON lr.LeaveTypeId = lt.LeaveTypeId
                LEFT JOIN Users u ON lr.ApproverId = u.UserId
                LEFT JOIN Employees m ON e.ManagerId = m.EmployeeId
                WHERE lr.EmployeeId = ?
                """);

        if (status != null && !status.isBlank() && !"ALL".equalsIgnoreCase(status)) {
            sql.append(" AND lr.Status = ?");
        }
        if (year != null && year > 0) {
            sql.append(" AND YEAR(lr.StartDate) = ?");
        }
        sql.append(" ORDER BY lr.LeaveRequestId DESC OFFSET ? ROWS FETCH NEXT ? ROWS ONLY");

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            int idx = 1;
            ps.setInt(idx++, employeeId);
            if (status != null && !status.isBlank() && !"ALL".equalsIgnoreCase(status)) {
                ps.setString(idx++, status.trim());
            }
            if (year != null && year > 0) {
                ps.setInt(idx++, year);
            }
            ps.setInt(idx++, Math.max(0, offset));
            ps.setInt(idx, limit > 0 ? limit : 20);

            try (ResultSet rs = ps.executeQuery()) {
                List<LeaveRequest> list = new ArrayList<>();
                while (rs.next()) {
                    list.add(mapLeaveRequest(rs));
                }
                return list;
            }
        }
    }

    /**
     * Đếm tổng số đơn xin nghỉ phép của nhân viên theo điều kiện lọc để phân trang.
     */
    public int countRequestsByEmployee(int employeeId, String status, Integer year) throws SQLException {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM LeaveRequests WHERE EmployeeId = ?");
        if (status != null && !status.isBlank() && !"ALL".equalsIgnoreCase(status)) {
            sql.append(" AND Status = ?");
        }
        if (year != null && year > 0) {
            sql.append(" AND YEAR(StartDate) = ?");
        }

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            int idx = 1;
            ps.setInt(idx++, employeeId);
            if (status != null && !status.isBlank() && !"ALL".equalsIgnoreCase(status)) {
                ps.setString(idx++, status.trim());
            }
            if (year != null && year > 0) {
                ps.setInt(idx, year);
            }
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    /**
     * Lấy danh sách các đơn nghỉ phép đang 'Pending' của nhân viên thuộc nhóm do Manager phụ trách.
     */
    public List<LeaveRequest> getPendingRequestsByManager(int managerEmployeeId, int offset, int limit) throws SQLException {
        String sql = """
                SELECT lr.LeaveRequestId, lr.CompanyId, lr.EmployeeId, lr.LeaveTypeId,
                       lr.StartDate, lr.EndDate, lr.TotalDays, lr.Reason, lr.Status,
                       lr.ApproverId, lr.ApproverComment, lr.ReviewedAt, lr.CreatedAt, lr.UpdatedAt,
                       e.EmployeeCode, e.FullName AS EmployeeFullName, d.DepartmentName,
                       lt.TypeCode AS LeaveTypeCode, lt.TypeName AS LeaveTypeName, lt.IsPaid,
                       u.DisplayName AS ApproverDisplayName, m.FullName AS ManagerName
                FROM LeaveRequests lr
                JOIN Employees e ON lr.EmployeeId = e.EmployeeId
                LEFT JOIN Departments d ON e.DepartmentId = d.DepartmentId
                JOIN LeaveTypes lt ON lr.LeaveTypeId = lt.LeaveTypeId
                LEFT JOIN Users u ON lr.ApproverId = u.UserId
                LEFT JOIN Employees m ON e.ManagerId = m.EmployeeId
                WHERE e.ManagerId = ? AND lr.Status = 'Pending'
                ORDER BY lr.CreatedAt ASC
                OFFSET ? ROWS FETCH NEXT ? ROWS ONLY
                """;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, managerEmployeeId);
            ps.setInt(2, Math.max(0, offset));
            ps.setInt(3, limit > 0 ? limit : 20);

            try (ResultSet rs = ps.executeQuery()) {
                List<LeaveRequest> list = new ArrayList<>();
                while (rs.next()) {
                    list.add(mapLeaveRequest(rs));
                }
                return list;
            }
        }
    }

    /**
     * Đếm số lượng đơn nghỉ phép đang chờ duyệt thuộc quyền quản lý của Manager.
     */
    public int countPendingRequestsByManager(int managerEmployeeId) throws SQLException {
        String sql = """
                SELECT COUNT(*)
                FROM LeaveRequests lr
                JOIN Employees e ON lr.EmployeeId = e.EmployeeId
                WHERE e.ManagerId = ? AND lr.Status = 'Pending'
                """;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, managerEmployeeId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    /**
     * Lấy danh sách đơn nghỉ phép toàn công ty (dành cho Admin/HR) với bộ lọc đa tiêu chí và phân trang.
     */
    public List<LeaveRequest> getAllRequestsByCompany(int companyId, String status, Integer departmentId,
                                                     Integer leaveTypeId, LocalDate fromDate, LocalDate toDate,
                                                     String keyword, int offset, int limit) throws SQLException {
        StringBuilder sql = new StringBuilder("""
                SELECT lr.LeaveRequestId, lr.CompanyId, lr.EmployeeId, lr.LeaveTypeId,
                       lr.StartDate, lr.EndDate, lr.TotalDays, lr.Reason, lr.Status,
                       lr.ApproverId, lr.ApproverComment, lr.ReviewedAt, lr.CreatedAt, lr.UpdatedAt,
                       e.EmployeeCode, e.FullName AS EmployeeFullName, d.DepartmentName,
                       lt.TypeCode AS LeaveTypeCode, lt.TypeName AS LeaveTypeName, lt.IsPaid,
                       u.DisplayName AS ApproverDisplayName, m.FullName AS ManagerName
                FROM LeaveRequests lr
                JOIN Employees e ON lr.EmployeeId = e.EmployeeId
                LEFT JOIN Departments d ON e.DepartmentId = d.DepartmentId
                JOIN LeaveTypes lt ON lr.LeaveTypeId = lt.LeaveTypeId
                LEFT JOIN Users u ON lr.ApproverId = u.UserId
                LEFT JOIN Employees m ON e.ManagerId = m.EmployeeId
                WHERE lr.CompanyId = ?
                """);

        appendFilters(sql, status, departmentId, leaveTypeId, fromDate, toDate, keyword);
        sql.append(" ORDER BY lr.LeaveRequestId DESC OFFSET ? ROWS FETCH NEXT ? ROWS ONLY");

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            int idx = setFilterParams(ps, 1, companyId, status, departmentId, leaveTypeId, fromDate, toDate, keyword);
            ps.setInt(idx++, Math.max(0, offset));
            ps.setInt(idx, limit > 0 ? limit : 20);

            try (ResultSet rs = ps.executeQuery()) {
                List<LeaveRequest> list = new ArrayList<>();
                while (rs.next()) {
                    list.add(mapLeaveRequest(rs));
                }
                return list;
            }
        }
    }

    /**
     * Đếm tổng số đơn xin nghỉ phép toàn công ty thỏa mãn điều kiện lọc.
     */
    public int countAllRequestsByCompany(int companyId, String status, Integer departmentId,
                                        Integer leaveTypeId, LocalDate fromDate, LocalDate toDate,
                                        String keyword) throws SQLException {
        StringBuilder sql = new StringBuilder("""
                SELECT COUNT(*)
                FROM LeaveRequests lr
                JOIN Employees e ON lr.EmployeeId = e.EmployeeId
                WHERE lr.CompanyId = ?
                """);

        appendFilters(sql, status, departmentId, leaveTypeId, fromDate, toDate, keyword);

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            setFilterParams(ps, 1, companyId, status, departmentId, leaveTypeId, fromDate, toDate, keyword);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    /**
     * Cập nhật trạng thái duyệt đơn (Approved / Rejected) kèm người duyệt và lời nhắn trong một giao dịch.
     */
    public boolean updateStatus(Connection conn, int leaveRequestId, String status, Integer approverUserId, String comment) throws SQLException {
        String sql = """
                UPDATE LeaveRequests
                SET Status = ?, ApproverId = ?, ApproverComment = ?, ReviewedAt = SYSUTCDATETIME(), UpdatedAt = SYSUTCDATETIME()
                WHERE LeaveRequestId = ? AND Status = 'Pending'
                """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            if (approverUserId != null) {
                ps.setInt(2, approverUserId);
            } else {
                ps.setNull(2, Types.INTEGER);
            }
            ps.setString(3, comment);
            ps.setInt(4, leaveRequestId);
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * Đồng bộ ngày nghỉ phép đã duyệt sang bảng Chấm công (Attendances) bằng lệnh MERGE (Upsert):
     * Nếu đã có bản ghi chấm công -> Cập nhật trạng thái 'On Leave' và ghi chú.
     * Nếu chưa có bản ghi -> Chèn mới với Status = 'On Leave' và TotalHours = 0.00.
     * Tránh xung đột trùng lặp khóa duy nhất (EmployeeId, AttendanceDate).
     */
    public void syncAttendanceOnLeave(Connection conn, int companyId, int employeeId, LocalDate date, String note) throws SQLException {
        String sql = """
                MERGE Attendances AS target
                USING (SELECT ? AS CompanyId, ? AS EmployeeId, ? AS AttendanceDate, 'On Leave' AS Status, ? AS Note) AS src
                ON target.EmployeeId = src.EmployeeId AND target.AttendanceDate = src.AttendanceDate
                WHEN MATCHED THEN
                    UPDATE SET Status = src.Status, Note = src.Note, UpdatedAt = SYSUTCDATETIME()
                WHEN NOT MATCHED THEN
                    INSERT (CompanyId, EmployeeId, AttendanceDate, TotalHours, Status, Note, CreatedAt)
                    VALUES (src.CompanyId, src.EmployeeId, src.AttendanceDate, 0.00, src.Status, src.Note, SYSUTCDATETIME());
                """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, companyId);
            ps.setInt(2, employeeId);
            ps.setDate(3, Date.valueOf(date));
            ps.setString(4, note);
            ps.executeUpdate();
        }
    }

    /**
     * Cho phép nhân viên tự hủy đơn xin nghỉ phép của mình khi đơn vẫn ở trạng thái 'Pending'.
     */
    public boolean cancelRequestByEmployee(int leaveRequestId, int employeeId) throws SQLException {
        String sql = """
                UPDATE LeaveRequests
                SET Status = 'Cancelled', UpdatedAt = SYSUTCDATETIME()
                WHERE LeaveRequestId = ? AND EmployeeId = ? AND Status = 'Pending'
                """;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, leaveRequestId);
            ps.setInt(2, employeeId);
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * Lấy danh sách các đơn nghỉ phép đã được phê duyệt ('Approved') giao thoa với khoảng thời gian target
     * để vẽ các sự kiện nghỉ phép trên giao diện lịch làm việc.
     */
    public List<LeaveRequest> getApprovedRequestsForCalendar(int companyId, Integer employeeId, Integer departmentId,
                                                            LocalDate startDate, LocalDate endDate) throws SQLException {
        StringBuilder sql = new StringBuilder("""
                SELECT lr.LeaveRequestId, lr.CompanyId, lr.EmployeeId, lr.LeaveTypeId,
                       lr.StartDate, lr.EndDate, lr.TotalDays, lr.Reason, lr.Status,
                       lr.ApproverId, lr.ApproverComment, lr.ReviewedAt, lr.CreatedAt, lr.UpdatedAt,
                       e.EmployeeCode, e.FullName AS EmployeeFullName, d.DepartmentName,
                       lt.TypeCode AS LeaveTypeCode, lt.TypeName AS LeaveTypeName, lt.IsPaid,
                       u.DisplayName AS ApproverDisplayName, m.FullName AS ManagerName
                FROM LeaveRequests lr
                JOIN Employees e ON lr.EmployeeId = e.EmployeeId
                LEFT JOIN Departments d ON e.DepartmentId = d.DepartmentId
                JOIN LeaveTypes lt ON lr.LeaveTypeId = lt.LeaveTypeId
                LEFT JOIN Users u ON lr.ApproverId = u.UserId
                LEFT JOIN Employees m ON e.ManagerId = m.EmployeeId
                WHERE lr.CompanyId = ? AND lr.Status = 'Approved'
                  AND lr.StartDate <= ? AND lr.EndDate >= ?
                """);

        if (employeeId != null && employeeId > 0) {
            sql.append(" AND lr.EmployeeId = ?");
        }
        if (departmentId != null && departmentId > 0) {
            sql.append(" AND e.DepartmentId = ?");
        }
        sql.append(" ORDER BY lr.StartDate ASC");

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            int idx = 1;
            ps.setInt(idx++, companyId);
            ps.setDate(idx++, Date.valueOf(endDate));
            ps.setDate(idx++, Date.valueOf(startDate));
            if (employeeId != null && employeeId > 0) {
                ps.setInt(idx++, employeeId);
            }
            if (departmentId != null && departmentId > 0) {
                ps.setInt(idx, departmentId);
            }

            try (ResultSet rs = ps.executeQuery()) {
                List<LeaveRequest> list = new ArrayList<>();
                while (rs.next()) {
                    list.add(mapLeaveRequest(rs));
                }
                return list;
            }
        }
    }

    /**
     * Tổng hợp dữ liệu báo cáo thống kê tình hình sử dụng ngày phép toàn công ty theo năm:
     * Gom nhóm theo nhân viên, tính tổng ngày phép được cấp, đã sử dụng, còn lại,
     * số ngày nghỉ có lương / không lương và số lượng đơn theo từng trạng thái.
     */
    public List<LeaveReportDTO> getLeaveUtilizationReport(int companyId, int year, Integer departmentId) throws SQLException {
        StringBuilder sql = new StringBuilder("""
                SELECT e.EmployeeId, e.EmployeeCode, e.FullName, d.DepartmentName, p.PositionTitle,
                       ISNULL(SUM(b.EntitledDays), 0) AS TotalEntitled,
                       ISNULL(SUM(b.UsedDays), 0) AS TotalUsed,
                       ISNULL(SUM(b.RemainingDays), 0) AS TotalRemaining,
                       ISNULL(SUM(CASE WHEN lt.IsPaid = 1 THEN b.UsedDays ELSE 0 END), 0) AS PaidUsed,
                       ISNULL(SUM(CASE WHEN lt.IsPaid = 0 THEN b.UsedDays ELSE 0 END), 0) AS UnpaidUsed,
                       (SELECT COUNT(*) FROM LeaveRequests lr WHERE lr.EmployeeId = e.EmployeeId AND YEAR(lr.StartDate) = ?) AS TotalReq,
                       (SELECT COUNT(*) FROM LeaveRequests lr WHERE lr.EmployeeId = e.EmployeeId AND YEAR(lr.StartDate) = ? AND lr.Status = 'Approved') AS ApprovedReq,
                       (SELECT COUNT(*) FROM LeaveRequests lr WHERE lr.EmployeeId = e.EmployeeId AND YEAR(lr.StartDate) = ? AND lr.Status = 'Pending') AS PendingReq,
                       (SELECT COUNT(*) FROM LeaveRequests lr WHERE lr.EmployeeId = e.EmployeeId AND YEAR(lr.StartDate) = ? AND lr.Status = 'Rejected') AS RejectedReq
                FROM Employees e
                LEFT JOIN Departments d ON e.DepartmentId = d.DepartmentId
                LEFT JOIN Positions p ON e.PositionId = p.PositionId
                LEFT JOIN LeaveBalances b ON e.EmployeeId = b.EmployeeId AND b.Year = ?
                LEFT JOIN LeaveTypes lt ON b.LeaveTypeId = lt.LeaveTypeId
                WHERE e.CompanyId = ?
                """);

        if (departmentId != null && departmentId > 0) {
            sql.append(" AND e.DepartmentId = ?");
        }
        sql.append(" GROUP BY e.EmployeeId, e.EmployeeCode, e.FullName, d.DepartmentName, p.PositionTitle ORDER BY e.FullName ASC");

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            ps.setInt(1, year);
            ps.setInt(2, year);
            ps.setInt(3, year);
            ps.setInt(4, year);
            ps.setInt(5, year);
            ps.setInt(6, companyId);
            if (departmentId != null && departmentId > 0) {
                ps.setInt(7, departmentId);
            }

            try (ResultSet rs = ps.executeQuery()) {
                List<LeaveReportDTO> list = new ArrayList<>();
                while (rs.next()) {
                    LeaveReportDTO dto = new LeaveReportDTO();
                    dto.setEmployeeId(rs.getInt("EmployeeId"));
                    dto.setEmployeeCode(rs.getString("EmployeeCode"));
                    dto.setFullName(rs.getString("FullName"));
                    dto.setDepartmentName(rs.getString("DepartmentName"));
                    dto.setPositionTitle(rs.getString("PositionTitle"));
                    dto.setEntitledDays(rs.getBigDecimal("TotalEntitled"));
                    dto.setUsedDays(rs.getBigDecimal("TotalUsed"));
                    dto.setRemainingDays(rs.getBigDecimal("TotalRemaining"));
                    dto.setPaidDays(rs.getBigDecimal("PaidUsed"));
                    dto.setUnpaidDays(rs.getBigDecimal("UnpaidUsed"));
                    dto.setTotalRequests(rs.getInt("TotalReq"));
                    dto.setApprovedRequests(rs.getInt("ApprovedReq"));
                    dto.setPendingRequests(rs.getInt("PendingReq"));
                    dto.setRejectedRequests(rs.getInt("RejectedReq"));
                    list.add(dto);
                }
                return list;
            }
        }
    }

    private void appendFilters(StringBuilder sql, String status, Integer departmentId, Integer leaveTypeId,
                               LocalDate fromDate, LocalDate toDate, String keyword) {
        if (status != null && !status.isBlank() && !"ALL".equalsIgnoreCase(status)) {
            sql.append(" AND lr.Status = ?");
        }
        if (departmentId != null && departmentId > 0) {
            sql.append(" AND e.DepartmentId = ?");
        }
        if (leaveTypeId != null && leaveTypeId > 0) {
            sql.append(" AND lr.LeaveTypeId = ?");
        }
        if (fromDate != null) {
            sql.append(" AND lr.StartDate >= ?");
        }
        if (toDate != null) {
            sql.append(" AND lr.EndDate <= ?");
        }
        if (keyword != null && !keyword.isBlank()) {
            sql.append(" AND (e.FullName LIKE ? OR e.EmployeeCode LIKE ?)");
        }
    }

    private int setFilterParams(PreparedStatement ps, int startIdx, int companyId, String status,
                                Integer departmentId, Integer leaveTypeId, LocalDate fromDate,
                                LocalDate toDate, String keyword) throws SQLException {
        int idx = startIdx;
        ps.setInt(idx++, companyId);
        if (status != null && !status.isBlank() && !"ALL".equalsIgnoreCase(status)) {
            ps.setString(idx++, status.trim());
        }
        if (departmentId != null && departmentId > 0) {
            ps.setInt(idx++, departmentId);
        }
        if (leaveTypeId != null && leaveTypeId > 0) {
            ps.setInt(idx++, leaveTypeId);
        }
        if (fromDate != null) {
            ps.setDate(idx++, Date.valueOf(fromDate));
        }
        if (toDate != null) {
            ps.setDate(idx++, Date.valueOf(toDate));
        }
        if (keyword != null && !keyword.isBlank()) {
            String kw = "%" + keyword.trim() + "%";
            ps.setString(idx++, kw);
            ps.setString(idx++, kw);
        }
        return idx;
    }

    private LeaveRequest mapLeaveRequest(ResultSet rs) throws SQLException {
        LeaveRequest r = new LeaveRequest();
        r.setLeaveRequestId(rs.getInt("LeaveRequestId"));
        r.setCompanyId(rs.getInt("CompanyId"));
        r.setEmployeeId(rs.getInt("EmployeeId"));
        r.setLeaveTypeId(rs.getInt("LeaveTypeId"));

        Date sDate = rs.getDate("StartDate");
        if (sDate != null) r.setStartDate(sDate.toLocalDate());
        Date eDate = rs.getDate("EndDate");
        if (eDate != null) r.setEndDate(eDate.toLocalDate());

        r.setTotalDays(rs.getBigDecimal("TotalDays"));
        r.setReason(rs.getString("Reason"));
        r.setStatus(rs.getString("Status"));

        int approverId = rs.getInt("ApproverId");
        if (!rs.wasNull()) r.setApproverId(approverId);
        r.setApproverComment(rs.getString("ApproverComment"));

        java.sql.Timestamp revTs = rs.getTimestamp("ReviewedAt");
        if (revTs != null) r.setReviewedAt(revTs.toLocalDateTime());
        java.sql.Timestamp cTs = rs.getTimestamp("CreatedAt");
        if (cTs != null) r.setCreatedAt(cTs.toLocalDateTime());
        java.sql.Timestamp uTs = rs.getTimestamp("UpdatedAt");
        if (uTs != null) r.setUpdatedAt(uTs.toLocalDateTime());

        // Display fields
        r.setEmployeeCode(rs.getString("EmployeeCode"));
        r.setEmployeeFullName(rs.getString("EmployeeFullName"));
        r.setDepartmentName(rs.getString("DepartmentName"));
        r.setLeaveTypeCode(rs.getString("LeaveTypeCode"));
        r.setLeaveTypeName(rs.getString("LeaveTypeName"));
        r.setIsPaid(rs.getBoolean("IsPaid"));
        r.setApproverDisplayName(rs.getString("ApproverDisplayName"));
        r.setManagerName(rs.getString("ManagerName"));

        return r;
    }
}

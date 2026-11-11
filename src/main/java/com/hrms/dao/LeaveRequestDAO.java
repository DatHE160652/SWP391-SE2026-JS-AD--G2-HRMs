package com.hrms.dao;

import com.hrms.model.LeaveRequest;
import com.hrms.util.DBConnection;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class LeaveRequestDAO {
    /** Count of approved leave days for one employee in a calendar month. */
    public int usedDays(Connection conn, int companyId, int employeeId, java.time.YearMonth month) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("""
            SELECT COUNT(*) FROM LeaveRequestDates rd
            JOIN LeaveRequests lr ON lr.LeaveRequestId=rd.LeaveRequestId
            WHERE lr.CompanyId=? AND lr.EmployeeId=? AND lr.Status='Approved' AND lr.IsLegacy=0
              AND rd.LeaveDate>=? AND rd.LeaveDate<?
            """)) {
            ps.setInt(1, companyId); ps.setInt(2, employeeId);
            ps.setDate(3, Date.valueOf(month.atDay(1))); ps.setDate(4, Date.valueOf(month.plusMonths(1).atDay(1)));
            try (ResultSet rs = ps.executeQuery()) { rs.next(); return rs.getInt(1); }
        }
    }

    private static final String SELECT = """
        SELECT lr.*,e.EmployeeCode,e.FullName EmployeeFullName,e.ManagerId,e.UserId RequesterUserId,
            d.DepartmentName,p.PositionTitle,r.RoleCode RequesterRole,u.DisplayName ApproverDisplayName
        FROM LeaveRequests lr
        JOIN Employees e ON e.EmployeeId=lr.EmployeeId AND e.CompanyId=lr.CompanyId
        LEFT JOIN Departments d ON d.DepartmentId=e.DepartmentId AND d.CompanyId=e.CompanyId
        LEFT JOIN Positions p ON p.PositionId=e.PositionId AND p.CompanyId=e.CompanyId
        LEFT JOIN Users requester ON requester.UserId=e.UserId AND requester.CompanyId=e.CompanyId
        LEFT JOIN Roles r ON r.RoleId=requester.RoleId
        LEFT JOIN Users u ON u.UserId=lr.ApproverId AND u.CompanyId=lr.CompanyId
        """;

    public LeaveRequest getRequest(Connection conn, int companyId, int id) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(SELECT + " WHERE lr.CompanyId=? AND lr.LeaveRequestId=?")) {
            ps.setInt(1, companyId);
            ps.setInt(2, id);
            List<LeaveRequest> list = readRequests(conn, ps);
            return list.isEmpty() ? null : list.get(0);
        }
    }

    /** Date range used by the request list filters (all optional). */
    public record DateFilter(LocalDate date, Integer month, Integer year) {
        public static final DateFilter NONE = new DateFilter(null, null, null);
    }

    public List<LeaveRequest> list(Connection conn, int companyId, Integer employeeId,
            Integer managerId, boolean managerRequestsOnly, String status, DateFilter filter,
            int offset, int limit) throws SQLException {
        StringBuilder sql = new StringBuilder(SELECT + " WHERE lr.CompanyId=?");
        List<Object> params = new ArrayList<>();
        params.add(companyId);
        if (employeeId != null) { sql.append(" AND lr.EmployeeId=?"); params.add(employeeId); }
        if (managerId != null) { sql.append(" AND e.ManagerId=? AND r.RoleCode='EMPLOYEE'"); params.add(managerId); }
        if (managerRequestsOnly) sql.append(" AND r.RoleCode='MANAGER'");
        if (status != null && !status.isBlank() && !"ALL".equals(status)) {
            sql.append(" AND lr.Status=?"); params.add(status);
        }
        DateFilter f = filter == null ? DateFilter.NONE : filter;
        if (f.date() != null) {
            sql.append(" AND (EXISTS(SELECT 1 FROM LeaveRequestDates rd WHERE rd.LeaveRequestId=lr.LeaveRequestId AND rd.LeaveDate=?)"
                    + " OR (lr.IsLegacy=1 AND ? BETWEEN lr.StartDate AND lr.EndDate))");
            params.add(Date.valueOf(f.date())); params.add(Date.valueOf(f.date()));
        } else {
            if (f.year() != null) {
                sql.append(" AND (EXISTS(SELECT 1 FROM LeaveRequestDates rd WHERE rd.LeaveRequestId=lr.LeaveRequestId AND YEAR(rd.LeaveDate)=?)"
                        + " OR (lr.IsLegacy=1 AND YEAR(lr.StartDate)=?))");
                params.add(f.year()); params.add(f.year());
            }
            if (f.month() != null) {
                sql.append(" AND (EXISTS(SELECT 1 FROM LeaveRequestDates rd WHERE rd.LeaveRequestId=lr.LeaveRequestId AND MONTH(rd.LeaveDate)=?)"
                        + " OR (lr.IsLegacy=1 AND MONTH(lr.StartDate)=?))");
                params.add(f.month()); params.add(f.month());
            }
        }
        sql.append(" ORDER BY lr.LeaveRequestId DESC OFFSET ? ROWS FETCH NEXT ? ROWS ONLY");
        params.add(Math.max(0, offset)); params.add(limit);
        try (PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) ps.setObject(i + 1, params.get(i));
            return readRequests(conn, ps);
        }
    }

    public List<LeaveRequest> getAllRequestsByCompany(int companyId, String status, Integer departmentId,
            LocalDate fromDate, LocalDate toDate, String keyword, int offset, int limit) throws SQLException {
        StringBuilder sql = new StringBuilder(SELECT + " WHERE lr.CompanyId=?");
        List<Object> params = new ArrayList<>();
        params.add(companyId);
        if (status != null && !status.isBlank() && !"ALL".equals(status)) { sql.append(" AND lr.Status=?"); params.add(status); }
        if (departmentId != null) { sql.append(" AND e.DepartmentId=?"); params.add(departmentId); }
        if (fromDate != null) { sql.append(" AND lr.EndDate>=?"); params.add(Date.valueOf(fromDate)); }
        if (toDate != null) { sql.append(" AND lr.StartDate<=?"); params.add(Date.valueOf(toDate)); }
        if (keyword != null && !keyword.isBlank()) {
            sql.append(" AND (e.FullName LIKE ? OR e.EmployeeCode LIKE ?)");
            params.add("%" + keyword + "%"); params.add("%" + keyword + "%");
        }
        sql.append(" ORDER BY lr.LeaveRequestId DESC OFFSET ? ROWS FETCH NEXT ? ROWS ONLY");
        params.add(offset); params.add(limit);
        try (Connection conn = DBConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) ps.setObject(i + 1, params.get(i));
            return readRequests(conn, ps);
        }
    }

    public int insertRequest(Connection conn, LeaveRequest request) throws SQLException {
        String sql = """
            INSERT INTO LeaveRequests(CompanyId,EmployeeId,StartDate,EndDate,TotalDays,Reason,Status)
            VALUES(?,?,?,?,?,?,'Pending')
            """;
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, request.getCompanyId()); ps.setInt(2, request.getEmployeeId());
            ps.setDate(3, Date.valueOf(request.getDates().get(0)));
            ps.setDate(4, Date.valueOf(request.getDates().get(request.getDates().size() - 1)));
            ps.setInt(5, request.getDates().size()); ps.setString(6, request.getReason());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (!rs.next()) throw new SQLException("Không tạo được mã đơn nghỉ.");
                request.setLeaveRequestId(rs.getInt(1));
            }
        }
        try (PreparedStatement ps = conn.prepareStatement("INSERT INTO LeaveRequestDates(LeaveRequestId,LeaveDate) VALUES(?,?)")) {
            for (LocalDate date : request.getDates()) {
                ps.setInt(1, request.getLeaveRequestId()); ps.setDate(2, Date.valueOf(date)); ps.addBatch();
            }
            ps.executeBatch();
        }
        return request.getLeaveRequestId();
    }

    public boolean hasOverlap(Connection conn, int companyId, int employeeId, LocalDate date, int excludingId) throws SQLException {
        String sql = """
            SELECT 1 FROM LeaveRequests lr
            WHERE lr.CompanyId=? AND lr.EmployeeId=? AND lr.Status='Approved' AND lr.LeaveRequestId<>?
                AND (EXISTS(SELECT 1 FROM LeaveRequestDates rd WHERE rd.LeaveRequestId=lr.LeaveRequestId AND rd.LeaveDate=?)
                    OR (lr.IsLegacy=1 AND ? BETWEEN lr.StartDate AND lr.EndDate))
            """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, companyId); ps.setInt(2, employeeId); ps.setInt(3, excludingId);
            ps.setDate(4, Date.valueOf(date)); ps.setDate(5, Date.valueOf(date));
            try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
        }
    }

    public boolean updateStatus(Connection conn, int companyId, int id, String status, int actorId, String comment) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("""
            UPDATE LeaveRequests SET Status=?,ApproverId=?,ApproverComment=?,ReviewedAt=SYSUTCDATETIME(),UpdatedAt=SYSUTCDATETIME()
            WHERE CompanyId=? AND LeaveRequestId=? AND Status='Pending'
            """)) {
            ps.setString(1, status); ps.setInt(2, actorId); ps.setString(3, comment);
            ps.setInt(4, companyId); ps.setInt(5, id);
            return ps.executeUpdate() == 1;
        }
    }

    public void syncAttendanceOnLeave(Connection conn, int companyId, int employeeId, LocalDate date, String note) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("""
            SELECT Status,CheckInTime,CheckOutTime,IsManualAdjusted FROM Attendances WITH(UPDLOCK,HOLDLOCK)
            WHERE CompanyId=? AND EmployeeId=? AND AttendanceDate=?
            """)) {
            ps.setInt(1, companyId); ps.setInt(2, employeeId); ps.setDate(3, Date.valueOf(date));
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next() && (rs.getTime("CheckInTime") != null || rs.getTime("CheckOutTime") != null
                        || rs.getBoolean("IsManualAdjusted") || !"Absent".equals(rs.getString("Status"))))
                    throw new IllegalArgumentException("Ngày " + date + " đã có chấm công. Không thể ghi đè bằng đơn nghỉ.");
            }
        }
        try (PreparedStatement ps = conn.prepareStatement("""
            UPDATE Attendances SET Status='On Leave',TotalHours=0,Note=?,UpdatedAt=SYSUTCDATETIME()
                WHERE CompanyId=? AND EmployeeId=? AND AttendanceDate=?;
            IF @@ROWCOUNT=0 INSERT INTO Attendances(CompanyId,EmployeeId,AttendanceDate,TotalHours,Status,Note)
                VALUES(?,?,?,0,'On Leave',?);
            """)) {
            ps.setString(1, note); ps.setInt(2, companyId); ps.setInt(3, employeeId); ps.setDate(4, Date.valueOf(date));
            ps.setInt(5, companyId); ps.setInt(6, employeeId); ps.setDate(7, Date.valueOf(date)); ps.setString(8, note);
            ps.executeUpdate();
        }
    }

    public void ensureTimesheetOpen(Connection conn, int companyId, LocalDate date) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT 1 FROM TimesheetLocks WITH(UPDLOCK,HOLDLOCK) WHERE CompanyId=? AND [Year]=? AND [Month]=? AND IsLocked=1")) {
            ps.setInt(1, companyId); ps.setInt(2, date.getYear()); ps.setInt(3, date.getMonthValue());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) throw new IllegalArgumentException("Tháng " + date.getMonthValue() + "/" + date.getYear() + " đã chốt công.");
            }
        }
    }

    private List<LeaveRequest> readRequests(Connection conn, PreparedStatement ps) throws SQLException {
        List<LeaveRequest> list = new ArrayList<>();
        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                LeaveRequest r = new LeaveRequest();
                r.setLeaveRequestId(rs.getInt("LeaveRequestId")); r.setCompanyId(rs.getInt("CompanyId"));
                r.setEmployeeId(rs.getInt("EmployeeId")); r.setManagerId(rs.getObject("ManagerId", Integer.class));
                r.setRequesterUserId(rs.getObject("RequesterUserId", Integer.class));
                r.setRequesterRole(rs.getString("RequesterRole")); r.setStartDate(rs.getDate("StartDate").toLocalDate());
                r.setEndDate(rs.getDate("EndDate").toLocalDate()); r.setTotalDays(rs.getBigDecimal("TotalDays"));
                r.setReason(rs.getString("Reason")); r.setStatus(rs.getString("Status")); r.setLegacy(rs.getBoolean("IsLegacy"));
                r.setApproverId(rs.getObject("ApproverId", Integer.class)); r.setApproverComment(rs.getString("ApproverComment"));
                r.setEmployeeCode(rs.getString("EmployeeCode")); r.setEmployeeFullName(rs.getString("EmployeeFullName"));
                r.setDepartmentName(rs.getString("DepartmentName")); r.setPositionTitle(rs.getString("PositionTitle"));
                r.setApproverDisplayName(rs.getString("ApproverDisplayName"));
                Timestamp reviewed = rs.getTimestamp("ReviewedAt"), created = rs.getTimestamp("CreatedAt");
                if (reviewed != null) r.setReviewedAt(reviewed.toLocalDateTime());
                if (created != null) r.setCreatedAt(created.toLocalDateTime());
                list.add(r);
            }
        }
        try (PreparedStatement dates = conn.prepareStatement("SELECT LeaveDate FROM LeaveRequestDates WHERE LeaveRequestId=? ORDER BY LeaveDate")) {
            for (LeaveRequest r : list) {
                dates.setInt(1, r.getLeaveRequestId());
                List<LocalDate> selected = new ArrayList<>();
                try (ResultSet rs = dates.executeQuery()) { while (rs.next()) selected.add(rs.getDate(1).toLocalDate()); }
                r.setDates(selected);
            }
        }
        return list;
    }
}

package com.hrms.dao;

import com.hrms.util.DBConnection;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Time;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public class AttendanceDAO {

    public AttendanceDTO getAttendanceByEmployeeAndDate(int employeeId, LocalDate date) throws SQLException {
        String sql = """
                SELECT a.AttendanceId, a.CompanyId, a.EmployeeId, a.AttendanceDate,
                       a.CheckInTime, a.CheckOutTime, a.TotalHours, a.Status, a.Note,
                       a.IsManualAdjusted, a.AdjustedByUserId,
                       e.EmployeeCode, e.FullName, e.DepartmentId, d.DepartmentName,
                       p.PositionTitle, e.ManagerId, m.FullName AS ManagerName
                FROM Attendances a
                JOIN Employees e ON a.CompanyId=e.CompanyId AND a.EmployeeId = e.EmployeeId
                LEFT JOIN Departments d ON e.CompanyId=d.CompanyId AND e.DepartmentId = d.DepartmentId
                LEFT JOIN Positions p ON e.CompanyId=p.CompanyId AND e.PositionId = p.PositionId
                LEFT JOIN Employees m ON e.CompanyId=m.CompanyId AND e.ManagerId = m.EmployeeId
                WHERE a.EmployeeId = ? AND a.AttendanceDate = ?
                """;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, employeeId);
            ps.setDate(2, Date.valueOf(date));
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapAttendanceDTO(rs);
                }
            }
        }
        return null;
    }

    public long insertCheckIn(int employeeId, int companyId, LocalDate date, LocalTime checkInTime, String status) throws SQLException {
        String sql = """
                INSERT INTO Attendances (CompanyId, EmployeeId, AttendanceDate, CheckInTime, TotalHours, Status, CreatedAt)
                VALUES (?, ?, ?, ?, 0.00, ?, SYSUTCDATETIME())
                """;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, companyId);
            ps.setInt(2, employeeId);
            ps.setDate(3, Date.valueOf(date));
            ps.setTime(4, Time.valueOf(checkInTime));
            ps.setString(5, status);
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
        }
        return 0;
    }

    public boolean updateCheckIn(long attendanceId, LocalTime checkInTime, String status) throws SQLException {
        String sql = """
                UPDATE Attendances
                SET CheckInTime = ?, Status = ?, UpdatedAt = SYSUTCDATETIME()
                WHERE AttendanceId = ?
                """;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setTime(1, Time.valueOf(checkInTime));
            ps.setString(2, status);
            ps.setLong(3, attendanceId);
            return ps.executeUpdate() > 0;
        }
    }

    public boolean updateCheckOut(long attendanceId, LocalTime checkOutTime, BigDecimal totalHours, String status) throws SQLException {
        String sql = """
                UPDATE Attendances
                SET CheckOutTime = ?, TotalHours = ?, Status = ?, UpdatedAt = SYSUTCDATETIME()
                WHERE AttendanceId = ?
                """;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setTime(1, Time.valueOf(checkOutTime));
            ps.setBigDecimal(2, totalHours);
            ps.setString(3, status);
            ps.setLong(4, attendanceId);
            return ps.executeUpdate() > 0;
        }
    }

    public long upsertManualAdjustment(int companyId, int employeeId, LocalDate date,
                                       LocalTime checkInTime, LocalTime checkOutTime,
                                       BigDecimal totalHours, String status, String note,
                                       Integer adjustedByUserId) throws SQLException {
        AttendanceDTO existing = getAttendanceByEmployeeAndDate(employeeId, date);
        if (existing != null && existing.getAttendanceId() != null) {
            String sql = """
                    UPDATE Attendances
                    SET CheckInTime = ?, CheckOutTime = ?, TotalHours = ?, Status = ?,
                        Note = ?, IsManualAdjusted = 1, AdjustedByUserId = ?, UpdatedAt = SYSUTCDATETIME()
                    WHERE AttendanceId = ?
                    """;
            try (Connection conn = DBConnection.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                if (checkInTime != null) {
                    ps.setTime(1, Time.valueOf(checkInTime));
                } else {
                    ps.setNull(1, java.sql.Types.TIME);
                }
                if (checkOutTime != null) {
                    ps.setTime(2, Time.valueOf(checkOutTime));
                } else {
                    ps.setNull(2, java.sql.Types.TIME);
                }
                ps.setBigDecimal(3, totalHours != null ? totalHours : BigDecimal.ZERO);
                ps.setString(4, status);
                ps.setString(5, note);
                if (adjustedByUserId != null && adjustedByUserId > 0) {
                    ps.setInt(6, adjustedByUserId);
                } else {
                    ps.setNull(6, java.sql.Types.INTEGER);
                }
                ps.setLong(7, existing.getAttendanceId());
                ps.executeUpdate();
                return existing.getAttendanceId();
            }
        } else {
            String sql = """
                    INSERT INTO Attendances (CompanyId, EmployeeId, AttendanceDate, CheckInTime, CheckOutTime,
                                             TotalHours, Status, Note, IsManualAdjusted, AdjustedByUserId, CreatedAt)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, 1, ?, SYSUTCDATETIME())
                    """;
            try (Connection conn = DBConnection.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, companyId);
                ps.setInt(2, employeeId);
                ps.setDate(3, Date.valueOf(date));
                if (checkInTime != null) {
                    ps.setTime(4, Time.valueOf(checkInTime));
                } else {
                    ps.setNull(4, java.sql.Types.TIME);
                }
                if (checkOutTime != null) {
                    ps.setTime(5, Time.valueOf(checkOutTime));
                } else {
                    ps.setNull(5, java.sql.Types.TIME);
                }
                ps.setBigDecimal(6, totalHours != null ? totalHours : BigDecimal.ZERO);
                ps.setString(7, status);
                ps.setString(8, note);
                if (adjustedByUserId != null && adjustedByUserId > 0) {
                    ps.setInt(9, adjustedByUserId);
                } else {
                    ps.setNull(9, java.sql.Types.INTEGER);
                }
                ps.executeUpdate();
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        return rs.getLong(1);
                    }
                }
            }
        }
        return 0;
    }

    public List<AttendanceDTO> getPersonalAttendance(int employeeId, int month, int year) throws SQLException {
        String sql = """
                SELECT a.AttendanceId, a.CompanyId, a.EmployeeId, a.AttendanceDate,
                       a.CheckInTime, a.CheckOutTime, a.TotalHours, a.Status, a.Note,
                       a.IsManualAdjusted, a.AdjustedByUserId,
                       e.EmployeeCode, e.FullName, e.DepartmentId, d.DepartmentName,
                       p.PositionTitle, e.ManagerId, m.FullName AS ManagerName
                FROM Attendances a
                JOIN Employees e ON a.CompanyId=e.CompanyId AND a.EmployeeId = e.EmployeeId
                LEFT JOIN Departments d ON e.CompanyId=d.CompanyId AND e.DepartmentId = d.DepartmentId
                LEFT JOIN Positions p ON e.CompanyId=p.CompanyId AND e.PositionId = p.PositionId
                LEFT JOIN Employees m ON e.CompanyId=m.CompanyId AND e.ManagerId = m.EmployeeId
                WHERE a.EmployeeId = ? AND MONTH(a.AttendanceDate) = ? AND YEAR(a.AttendanceDate) = ?
                ORDER BY a.AttendanceDate ASC
                """;

        List<AttendanceDTO> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, employeeId);
            ps.setInt(2, month);
            ps.setInt(3, year);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapAttendanceDTO(rs));
                }
            }
        }
        return list;
    }

    public AttendanceSummaryDAO getPersonalSummary(int employeeId, int month, int year) throws SQLException {
        String sql = """
                SELECT 
                    COUNT(CASE WHEN Status IN ('Present', 'Late', 'Early Leave') THEN 1 END) AS TotalWorkingDays,
                    ISNULL(SUM(CASE 
                        WHEN (Status IN ('Present', 'Late', 'Early Leave') OR Status LIKE '%Present%' OR Status LIKE '%Late%' OR Status LIKE '%Early%') AND TotalHours >= 7.0 THEN 1.0
                        WHEN (Status IN ('Present', 'Late', 'Early Leave') OR Status LIKE '%Present%' OR Status LIKE '%Late%' OR Status LIKE '%Early%') AND TotalHours >= 3.5 THEN 0.5
                        ELSE 0.0
                    END), 0.0) AS TotalWorkCredits,
                    ISNULL(SUM(CASE WHEN Status IN ('Present', 'Late', 'Early Leave') THEN TotalHours ELSE 0 END), 0) AS TotalHours,
                    COUNT(CASE WHEN Status = 'Present' THEN 1 END) AS PresentDays,
                    COUNT(CASE WHEN Status = 'Late' THEN 1 END) AS LateDays,
                    COUNT(CASE WHEN Status = 'Early Leave' THEN 1 END) AS EarlyLeaveDays,
                    COUNT(CASE WHEN Status = 'Absent' THEN 1 END) AS AbsentDays,
                    COUNT(CASE WHEN Status = 'On Leave' THEN 1 END) AS OnLeaveDays
                FROM Attendances
                WHERE EmployeeId = ? AND MONTH(AttendanceDate) = ? AND YEAR(AttendanceDate) = ?
                """;

        AttendanceSummaryDAO summary = new AttendanceSummaryDAO();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, employeeId);
            ps.setInt(2, month);
            ps.setInt(3, year);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    summary.setTotalWorkingDays(rs.getInt("TotalWorkingDays"));
                    summary.setTotalWorkCredits(rs.getBigDecimal("TotalWorkCredits"));
                    summary.setTotalHours(rs.getBigDecimal("TotalHours"));
                    summary.setPresentDays(rs.getInt("PresentDays"));
                    summary.setLateDays(rs.getInt("LateDays"));
                    summary.setEarlyLeaveDays(rs.getInt("EarlyLeaveDays"));
                    summary.setAbsentDays(rs.getInt("AbsentDays"));
                    summary.setOnLeaveDays(rs.getInt("OnLeaveDays"));
                }
            }
        }
        return summary;
    }

    public List<AttendanceDTO> getTeamAttendance(int managerId, LocalDate date) throws SQLException {
        return getTeamAttendance(getEmployeeCompanyId(managerId), managerId, date);
    }

    public List<AttendanceDTO> getTeamAttendance(int companyId, int managerId, LocalDate date) throws SQLException {
        String sql = """
                SELECT a.AttendanceId, e.CompanyId, e.EmployeeId, ? AS AttendanceDate,
                       a.CheckInTime, a.CheckOutTime, ISNULL(a.TotalHours, 0.00) AS TotalHours,
                       ISNULL(a.Status, 'Absent') AS Status, a.Note,
                       ISNULL(a.IsManualAdjusted, 0) AS IsManualAdjusted, a.AdjustedByUserId,
                       e.EmployeeCode, e.FullName, e.DepartmentId, d.DepartmentName,
                       p.PositionTitle, e.ManagerId, m.FullName AS ManagerName,
                       e.Status AS EmployeeStatus
                FROM Employees e
                LEFT JOIN Attendances a ON e.CompanyId=a.CompanyId AND e.EmployeeId = a.EmployeeId AND a.AttendanceDate = ?
                LEFT JOIN Departments d ON e.CompanyId=d.CompanyId AND e.DepartmentId = d.DepartmentId
                LEFT JOIN Positions p ON e.CompanyId=p.CompanyId AND e.PositionId = p.PositionId
                LEFT JOIN Employees m ON e.CompanyId=m.CompanyId AND e.ManagerId = m.EmployeeId
                WHERE e.ManagerId = ? AND e.CompanyId = ?
                ORDER BY e.FullName ASC
                """;

        List<AttendanceDTO> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(date));
            ps.setDate(2, Date.valueOf(date));
            ps.setInt(3, managerId);
            ps.setInt(4, companyId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapAttendanceDTO(rs));
                }
            }
        }
        return list;
    }

    public List<AttendanceDTO> getAllAttendanceByDate(int companyId, LocalDate date) throws SQLException {
        if (companyId <= 0) throw new SecurityException("Vui lòng đăng nhập vào workspace.");
        String sql = """
                SELECT a.AttendanceId, e.CompanyId, e.EmployeeId, ? AS AttendanceDate,
                       a.CheckInTime, a.CheckOutTime, ISNULL(a.TotalHours, 0.00) AS TotalHours,
                       ISNULL(a.Status, 'Absent') AS Status, a.Note,
                       ISNULL(a.IsManualAdjusted, 0) AS IsManualAdjusted, a.AdjustedByUserId,
                       e.EmployeeCode, e.FullName, e.DepartmentId, d.DepartmentName,
                       p.PositionTitle, e.ManagerId, m.FullName AS ManagerName,
                       ISNULL(r.RoleCode, 'EMPLOYEE') AS RoleCode,
                       ISNULL(r.RoleName, N'Nhân viên') AS RoleName,
                       e.Status AS EmployeeStatus
                FROM Employees e
                LEFT JOIN Attendances a ON e.CompanyId=a.CompanyId AND e.EmployeeId = a.EmployeeId AND a.AttendanceDate = ?
                LEFT JOIN Departments d ON e.CompanyId=d.CompanyId AND e.DepartmentId = d.DepartmentId
                LEFT JOIN Positions p ON e.CompanyId=p.CompanyId AND e.PositionId = p.PositionId
                LEFT JOIN Employees m ON e.CompanyId=m.CompanyId AND e.ManagerId = m.EmployeeId
                LEFT JOIN Users u ON e.CompanyId=u.CompanyId AND e.UserId = u.UserId
                LEFT JOIN Roles r ON u.RoleId = r.RoleId
                WHERE e.CompanyId=?
                ORDER BY e.EmployeeId ASC
                """;

        List<AttendanceDTO> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(date));
            ps.setDate(2, Date.valueOf(date));
            ps.setInt(3, companyId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapAttendanceDTO(rs));
                }
            }
        }
        return list;
    }

    public List<AttendanceDTO> searchCompanyAttendance(Integer companyId, Integer departmentId, String status,
                                                      LocalDate fromDate, LocalDate toDate, String keyword) throws SQLException {
        StringBuilder sql = new StringBuilder("""
                SELECT a.AttendanceId, a.CompanyId, a.EmployeeId, a.AttendanceDate,
                       a.CheckInTime, a.CheckOutTime, a.TotalHours, a.Status, a.Note,
                       a.IsManualAdjusted, a.AdjustedByUserId,
                       e.EmployeeCode, e.FullName, e.DepartmentId, d.DepartmentName,
                       p.PositionTitle, e.ManagerId, m.FullName AS ManagerName
                FROM Attendances a
                JOIN Employees e ON a.CompanyId=e.CompanyId AND a.EmployeeId = e.EmployeeId
                LEFT JOIN Departments d ON e.CompanyId=d.CompanyId AND e.DepartmentId = d.DepartmentId
                LEFT JOIN Positions p ON e.CompanyId=p.CompanyId AND e.PositionId = p.PositionId
                LEFT JOIN Employees m ON e.CompanyId=m.CompanyId AND e.ManagerId = m.EmployeeId
                WHERE 1=1
                """);

        List<Object> params = new ArrayList<>();

        if (companyId != null && companyId > 0) {
            sql.append(" AND (a.CompanyId = ? OR e.CompanyId = ? OR a.CompanyId = 0 OR a.CompanyId IS NULL)");
            params.add(companyId);
            params.add(companyId);
        }
        if (departmentId != null && departmentId > 0) {
            sql.append(" AND e.DepartmentId = ?");
            params.add(departmentId);
        }
        if (status != null && !status.isBlank() && !"ALL".equalsIgnoreCase(status)) {
            sql.append(" AND a.Status = ?");
            params.add(status);
        }
        if (fromDate != null) {
            sql.append(" AND a.AttendanceDate >= ?");
            params.add(Date.valueOf(fromDate));
        }
        if (toDate != null) {
            sql.append(" AND a.AttendanceDate <= ?");
            params.add(Date.valueOf(toDate));
        }
        if (keyword != null && !keyword.isBlank()) {
            sql.append(" AND (e.EmployeeCode LIKE ? OR e.FullName LIKE ?)");
            String kw = "%" + keyword.trim() + "%";
            params.add(kw);
            params.add(kw);
        }

        sql.append(" ORDER BY a.AttendanceDate DESC, e.FullName ASC");

        List<AttendanceDTO> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                Object param = params.get(i);
                if (param instanceof Integer v) ps.setInt(i + 1, v);
                else if (param instanceof Date v) ps.setDate(i + 1, v);
                else if (param instanceof String v) ps.setString(i + 1, v);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapAttendanceDTO(rs));
                }
            }
        }
        return list;
    }

    public List<AttendanceReportDAO> getAttendanceReport(Integer companyId, Integer departmentId, int month, int year) throws SQLException {
        StringBuilder sql = new StringBuilder("""
                SELECT 
                    e.EmployeeId, e.EmployeeCode, e.FullName,
                    d.DepartmentName, p.PositionTitle,
                    COUNT(CASE WHEN a.Status IN ('Present', 'Late', 'Early Leave') THEN 1 END) AS TotalWorkDays,
                    ISNULL(SUM(CASE WHEN a.Status IN ('Present', 'Late', 'Early Leave') THEN a.TotalHours ELSE 0 END), 0) AS TotalHours,
                    COUNT(CASE WHEN a.Status = 'Present' THEN 1 END) AS PresentCount,
                    COUNT(CASE WHEN a.Status = 'Late' THEN 1 END) AS LateCount,
                    COUNT(CASE WHEN a.Status = 'Early Leave' THEN 1 END) AS EarlyLeaveCount,
                    COUNT(CASE WHEN a.Status = 'Absent' THEN 1 END) AS AbsentCount,
                    COUNT(CASE WHEN a.Status = 'On Leave' THEN 1 END) AS OnLeaveCount
                FROM Employees e
                LEFT JOIN Departments d ON e.CompanyId=d.CompanyId AND e.DepartmentId = d.DepartmentId
                LEFT JOIN Positions p ON e.CompanyId=p.CompanyId AND e.PositionId = p.PositionId
                LEFT JOIN Attendances a ON e.CompanyId=a.CompanyId AND e.EmployeeId = a.EmployeeId
                     AND MONTH(a.AttendanceDate) = ? AND YEAR(a.AttendanceDate) = ?
                WHERE 1=1
                """);

        List<Object> params = new ArrayList<>();
        params.add(month);
        params.add(year);

        if (companyId != null && companyId > 0) {
            sql.append(" AND (e.CompanyId = ? OR e.CompanyId IS NULL OR e.CompanyId = 0)");
            params.add(companyId);
        }
        if (departmentId != null && departmentId > 0) {
            sql.append(" AND e.DepartmentId = ?");
            params.add(departmentId);
        }

        sql.append("""
                 GROUP BY e.EmployeeId, e.EmployeeCode, e.FullName, d.DepartmentName, p.PositionTitle
                 ORDER BY d.DepartmentName ASC, e.FullName ASC
                """);

        List<AttendanceReportDAO> reports = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                Object p = params.get(i);
                if (p instanceof Integer v) ps.setInt(i + 1, v);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    AttendanceReportDAO r = new AttendanceReportDAO();
                    r.setEmployeeId(rs.getInt("EmployeeId"));
                    r.setEmployeeCode(rs.getString("EmployeeCode"));
                    r.setFullName(rs.getString("FullName"));
                    r.setDepartmentName(rs.getString("DepartmentName"));
                    r.setPositionTitle(rs.getString("PositionTitle"));
                    int workDays = rs.getInt("TotalWorkDays");
                    r.setTotalWorkDays(workDays);
                    r.setTotalHours(rs.getBigDecimal("TotalHours"));
                    int present = rs.getInt("PresentCount");
                    r.setPresentCount(present);
                    r.setLateCount(rs.getInt("LateCount"));
                    r.setEarlyLeaveCount(rs.getInt("EarlyLeaveCount"));
                    r.setAbsentCount(rs.getInt("AbsentCount"));
                    r.setOnLeaveCount(rs.getInt("OnLeaveCount"));

                    double rate = workDays > 0 ? (double) present / workDays * 100.0 : 0.0;
                    r.setPunctualityRate(Math.round(rate * 10.0) / 10.0);

                    reports.add(r);
                }
            }
        }
        return reports;
    }

    public List<AttendanceReportDAO> getAttendanceReportForManager(int managerId, Integer departmentId,
                                                                    int month, int year) throws SQLException {
        return getAttendanceReportForManager(getEmployeeCompanyId(managerId), managerId, departmentId, month, year);
    }

    public List<AttendanceReportDAO> getAttendanceReportForManager(int companyId, int managerId, Integer departmentId,
                                                                    int month, int year) throws SQLException {
        StringBuilder sql = new StringBuilder("""
                SELECT
                    e.EmployeeId, e.EmployeeCode, e.FullName,
                    d.DepartmentName, p.PositionTitle,
                    COUNT(CASE WHEN a.Status IN ('Present', 'Late', 'Early Leave') THEN 1 END) AS TotalWorkDays,
                    ISNULL(SUM(CASE WHEN a.Status IN ('Present', 'Late', 'Early Leave') THEN a.TotalHours ELSE 0 END), 0) AS TotalHours,
                    COUNT(CASE WHEN a.Status = 'Present' THEN 1 END) AS PresentCount,
                    COUNT(CASE WHEN a.Status = 'Late' THEN 1 END) AS LateCount,
                    COUNT(CASE WHEN a.Status = 'Early Leave' THEN 1 END) AS EarlyLeaveCount,
                    COUNT(CASE WHEN a.Status = 'Absent' THEN 1 END) AS AbsentCount,
                    COUNT(CASE WHEN a.Status = 'On Leave' THEN 1 END) AS OnLeaveCount
                FROM Employees e
                LEFT JOIN Departments d ON e.CompanyId=d.CompanyId AND e.DepartmentId = d.DepartmentId
                LEFT JOIN Positions p ON e.CompanyId=p.CompanyId AND e.PositionId = p.PositionId
                LEFT JOIN Attendances a ON e.CompanyId=a.CompanyId AND e.EmployeeId = a.EmployeeId
                     AND MONTH(a.AttendanceDate) = ? AND YEAR(a.AttendanceDate) = ?
                WHERE e.ManagerId = ? AND e.CompanyId = ?
                """);

        List<Object> params = new ArrayList<>();
        params.add(month);
        params.add(year);
        params.add(managerId);
        params.add(companyId);

        if (departmentId != null && departmentId > 0) {
            sql.append(" AND e.DepartmentId = ?");
            params.add(departmentId);
        }

        sql.append("""
                 GROUP BY e.EmployeeId, e.EmployeeCode, e.FullName, d.DepartmentName, p.PositionTitle
                 ORDER BY d.DepartmentName ASC, e.FullName ASC
                """);

        return mapAttendanceReports(sql.toString(), params);
    }

    public void requireEmployee(int companyId, int employeeId) throws SQLException {
        if (companyId <= 0) throw new SecurityException("Vui lòng đăng nhập vào workspace.");
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT 1 FROM Employees WHERE CompanyId=? AND EmployeeId=?")) {
            statement.setInt(1, companyId);
            statement.setInt(2, employeeId);
            try (ResultSet rows = statement.executeQuery()) {
                if (!rows.next()) throw new SecurityException("Không tìm thấy nhân viên trong workspace của bạn.");
            }
        }
    }

    public int getEmployeeCompanyId(int employeeId) throws SQLException {
        String sql = "SELECT CompanyId FROM Employees WHERE EmployeeId = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, employeeId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int cid = rs.getInt("CompanyId");
                    if (!rs.wasNull() && cid > 0) {
                        return cid;
                    }
                }
            }
        }
        throw new SecurityException("Không tìm thấy nhân viên trong workspace.");
    }

    private List<AttendanceReportDAO> mapAttendanceReports(String sql, List<Object> params) throws SQLException {
        List<AttendanceReportDAO> reports = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < params.size(); i++) {
                Object p = params.get(i);
                if (p instanceof Integer v) {
                    ps.setInt(i + 1, v);
                }
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    AttendanceReportDAO r = new AttendanceReportDAO();
                    r.setEmployeeId(rs.getInt("EmployeeId"));
                    r.setEmployeeCode(rs.getString("EmployeeCode"));
                    r.setFullName(rs.getString("FullName"));
                    r.setDepartmentName(rs.getString("DepartmentName"));
                    r.setPositionTitle(rs.getString("PositionTitle"));
                    int workDays = rs.getInt("TotalWorkDays");
                    r.setTotalWorkDays(workDays);
                    r.setTotalHours(rs.getBigDecimal("TotalHours"));
                    int present = rs.getInt("PresentCount");
                    r.setPresentCount(present);
                    r.setLateCount(rs.getInt("LateCount"));
                    r.setEarlyLeaveCount(rs.getInt("EarlyLeaveCount"));
                    r.setAbsentCount(rs.getInt("AbsentCount"));
                    r.setOnLeaveCount(rs.getInt("OnLeaveCount"));

                    double rate = workDays > 0 ? (double) present / workDays * 100.0 : 0.0;
                    r.setPunctualityRate(Math.round(rate * 10.0) / 10.0);

                    reports.add(r);
                }
            }
        }
        return reports;
    }

    private AttendanceDTO mapAttendanceDTO(ResultSet rs) throws SQLException {
        AttendanceDTO dto = new AttendanceDTO();
        long aid = rs.getLong("AttendanceId");
        if (!rs.wasNull()) {
            dto.setAttendanceId(aid);
        }
        dto.setCompanyId(rs.getInt("CompanyId"));
        dto.setEmployeeId(rs.getInt("EmployeeId"));
        Date d = rs.getDate("AttendanceDate");
        if (d != null) dto.setAttendanceDate(d.toLocalDate());
        Time inTime = rs.getTime("CheckInTime");
        if (inTime != null) dto.setCheckInTime(inTime.toLocalTime());
        Time outTime = rs.getTime("CheckOutTime");
        if (outTime != null) dto.setCheckOutTime(outTime.toLocalTime());
        dto.setTotalHours(rs.getBigDecimal("TotalHours"));
        dto.setWorkCredit(AttendanceDTO.calculateWorkCredit(dto.getTotalHours()));
        dto.setStatus(rs.getString("Status"));
        dto.setNote(rs.getString("Note"));
        dto.setManualAdjusted(rs.getBoolean("IsManualAdjusted"));
        int adjUser = rs.getInt("AdjustedByUserId");
        if (!rs.wasNull()) dto.setAdjustedByUserId(adjUser);

        dto.setEmployeeCode(rs.getString("EmployeeCode"));
        dto.setFullName(rs.getString("FullName"));
        dto.setDepartmentId(rs.getInt("DepartmentId"));
        dto.setDepartmentName(rs.getString("DepartmentName"));
        dto.setPositionTitle(rs.getString("PositionTitle"));
        dto.setManagerId(rs.getInt("ManagerId"));
        dto.setManagerName(rs.getString("ManagerName"));
        try {
            dto.setRoleCode(rs.getString("RoleCode"));
        } catch (SQLException ignored) {}
        try {
            dto.setRoleName(rs.getString("RoleName"));
        } catch (SQLException ignored) {}
        try {
            dto.setEmployeeStatus(rs.getString("EmployeeStatus"));
        } catch (SQLException ignored) {}
        return dto;
    }

    public static class AttendanceDTO {
        private Long attendanceId;
        private Integer companyId;
        private Integer employeeId;
        private String employeeCode;
        private String fullName;
        private Integer departmentId;
        private String departmentName;
        private String positionTitle;
        private Integer managerId;
        private String managerName;
        private LocalDate attendanceDate;
        private LocalTime checkInTime;
        private LocalTime checkOutTime;
        private BigDecimal totalHours;
        private BigDecimal workCredit;
        private String status;
        private String note;
        private Boolean manualAdjusted;
        private Integer adjustedByUserId;
        private String roleCode;
        private String roleName;
        private String employeeStatus;

        public Long getAttendanceId() { return attendanceId; }
        public void setAttendanceId(Long attendanceId) { this.attendanceId = attendanceId; }

        public Integer getCompanyId() { return companyId; }
        public void setCompanyId(Integer companyId) { this.companyId = companyId; }

        public Integer getEmployeeId() { return employeeId; }
        public void setEmployeeId(Integer employeeId) { this.employeeId = employeeId; }

        public String getEmployeeCode() { return employeeCode; }
        public void setEmployeeCode(String employeeCode) { this.employeeCode = employeeCode; }

        public String getFullName() { return fullName; }
        public void setFullName(String fullName) { this.fullName = fullName; }

        public Integer getDepartmentId() { return departmentId; }
        public void setDepartmentId(Integer departmentId) { this.departmentId = departmentId; }

        public String getDepartmentName() { return departmentName; }
        public void setDepartmentName(String departmentName) { this.departmentName = departmentName; }

        public String getPositionTitle() { return positionTitle; }
        public void setPositionTitle(String positionTitle) { this.positionTitle = positionTitle; }

        public Integer getManagerId() { return managerId; }
        public void setManagerId(Integer managerId) { this.managerId = managerId; }

        public String getManagerName() { return managerName; }
        public void setManagerName(String managerName) { this.managerName = managerName; }

        public LocalDate getAttendanceDate() { return attendanceDate; }
        public void setAttendanceDate(LocalDate attendanceDate) { this.attendanceDate = attendanceDate; }

        public LocalTime getCheckInTime() { return checkInTime; }
        public void setCheckInTime(LocalTime checkInTime) { this.checkInTime = checkInTime; }

        public LocalTime getCheckOutTime() { return checkOutTime; }
        public void setCheckOutTime(LocalTime checkOutTime) { this.checkOutTime = checkOutTime; }

        public BigDecimal getTotalHours() { return totalHours; }
        public void setTotalHours(BigDecimal totalHours) { this.totalHours = totalHours; }

        public static BigDecimal calculateWorkCredit(BigDecimal hours) {
            if (hours == null) return BigDecimal.ZERO;
            if (hours.compareTo(BigDecimal.valueOf(7.0)) >= 0) {
                return BigDecimal.valueOf(1.0);
            } else if (hours.compareTo(BigDecimal.valueOf(3.5)) >= 0) {
                return BigDecimal.valueOf(0.5);
            }
            return BigDecimal.ZERO;
        }

        public BigDecimal getWorkCredit() {
            if (workCredit != null) {
                return workCredit;
            }
            return calculateWorkCredit(totalHours);
        }

        public void setWorkCredit(BigDecimal workCredit) {
            this.workCredit = workCredit;
        }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public String getNote() { return note; }
        public void setNote(String note) { this.note = note; }

        public Boolean getManualAdjusted() { return manualAdjusted; }
        public void setManualAdjusted(Boolean manualAdjusted) { this.manualAdjusted = manualAdjusted; }

        public Integer getAdjustedByUserId() { return adjustedByUserId; }
        public void setAdjustedByUserId(Integer adjustedByUserId) { this.adjustedByUserId = adjustedByUserId; }

        public String getRoleCode() { return roleCode; }
        public void setRoleCode(String roleCode) { this.roleCode = roleCode; }

        public String getRoleName() { return roleName; }
        public void setRoleName(String roleName) { this.roleName = roleName; }

        public String getEmployeeStatus() { return employeeStatus; }
        public void setEmployeeStatus(String employeeStatus) { this.employeeStatus = employeeStatus; }
    }
}

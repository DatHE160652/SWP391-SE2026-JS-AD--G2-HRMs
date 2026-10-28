package com.hrms.dao;

import com.hrms.model.LeaveBalance;
import com.hrms.util.DBConnection;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Lớp truy xuất cơ sở dữ liệu (DAO) quản lý hạn mức và số dư ngày phép của nhân viên:
 * - Tra cứu số dư ngày phép theo nhân viên, loại phép và năm tài chính.
 * - Khấu trừ số ngày nghỉ khi phê duyệt đơn: cập nhật tăng số ngày đã dùng (UsedDays).
 *   (Lưu ý: Cột RemainingDays trong SQL Server là cột tính toán tự động [EntitledDays - UsedDays]).
 * - Khởi tạo hạn mức ngày phép ban đầu cho nhân viên mới hoặc năm làm việc mới.
 * - Thống kê số dư ngày phép toàn công ty hoặc theo phòng ban.
 */
public class LeaveBalanceDAO {

    /**
     * Lấy toàn bộ số dư ngày phép của một nhân viên trong năm tài chính chỉ định,
     * bao gồm thông tin chi tiết loại nghỉ phép (tên loại phép, có lương/không lương, hạn mức mặc định).
     */
    public List<LeaveBalance> getBalancesByEmployeeAndYear(int employeeId, int year) throws SQLException {
        String sql = """
                SELECT b.BalanceId, b.EmployeeId, b.LeaveTypeId, b.Year,
                       b.EntitledDays, b.UsedDays, b.RemainingDays, b.CreatedAt, b.UpdatedAt,
                       lt.TypeCode, lt.TypeName, lt.IsPaid, lt.DefaultDaysPerYear
                FROM LeaveBalances b
                JOIN LeaveTypes lt ON b.LeaveTypeId = lt.LeaveTypeId
                WHERE b.EmployeeId = ? AND b.Year = ?
                ORDER BY lt.LeaveTypeId ASC
                """;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, employeeId);
            ps.setInt(2, year);
            try (ResultSet rs = ps.executeQuery()) {
                List<LeaveBalance> list = new ArrayList<>();
                while (rs.next()) {
                    list.add(mapLeaveBalance(rs));
                }
                return list;
            }
        }
    }

    /**
     * Tra cứu bản ghi số dư ngày phép của một nhân viên theo từng loại phép và năm tài chính.
     */
    public LeaveBalance getBalance(int employeeId, int leaveTypeId, int year) throws SQLException {
        String sql = """
                SELECT b.BalanceId, b.EmployeeId, b.LeaveTypeId, b.Year,
                       b.EntitledDays, b.UsedDays, b.RemainingDays, b.CreatedAt, b.UpdatedAt,
                       lt.TypeCode, lt.TypeName, lt.IsPaid, lt.DefaultDaysPerYear
                FROM LeaveBalances b
                JOIN LeaveTypes lt ON b.LeaveTypeId = lt.LeaveTypeId
                WHERE b.EmployeeId = ? AND b.LeaveTypeId = ? AND b.Year = ?
                """;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, employeeId);
            ps.setInt(2, leaveTypeId);
            ps.setInt(3, year);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapLeaveBalance(rs);
                }
            }
        }
        return null;
    }

    /**
     * Khấu trừ số ngày phép khi duyệt đơn bằng cách tăng UsedDays trong một Transaction.
     * Lưu ý quan trọng: RemainingDays là cột được tính toán tự động trong SQL Server (EntitledDays - UsedDays),
     * không được cập nhật trực tiếp vào RemainingDays để tránh lỗi hệ thống.
     */
    public boolean deductUsedDays(Connection conn, int balanceId, BigDecimal additionalDays) throws SQLException {
        String sql = """
                UPDATE LeaveBalances
                SET UsedDays = UsedDays + ?, UpdatedAt = SYSUTCDATETIME()
                WHERE BalanceId = ?
                """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBigDecimal(1, additionalDays);
            ps.setInt(2, balanceId);
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * Khởi tạo bản ghi hạn mức ngày phép ban đầu (EntitledDays) cho nhân viên với UsedDays = 0.
     */
    public int insertBalance(Connection conn, int employeeId, int leaveTypeId, int year, BigDecimal entitledDays) throws SQLException {
        String sql = """
                INSERT INTO LeaveBalances (EmployeeId, LeaveTypeId, Year, EntitledDays, UsedDays, CreatedAt)
                VALUES (?, ?, ?, ?, 0.00, SYSUTCDATETIME())
                """;

        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, employeeId);
            ps.setInt(2, leaveTypeId);
            ps.setInt(3, year);
            ps.setBigDecimal(4, entitledDays);
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
     * Lấy danh sách số dư ngày phép của toàn bộ nhân viên trong công ty hoặc theo phòng ban.
     */
    public List<LeaveBalance> getCompanyBalances(int companyId, int year, Integer departmentId) throws SQLException {
        StringBuilder sql = new StringBuilder("""
                SELECT b.BalanceId, b.EmployeeId, b.LeaveTypeId, b.Year,
                       b.EntitledDays, b.UsedDays, b.RemainingDays, b.CreatedAt, b.UpdatedAt,
                       lt.TypeCode, lt.TypeName, lt.IsPaid, lt.DefaultDaysPerYear,
                       e.EmployeeCode, e.FullName, d.DepartmentName
                FROM LeaveBalances b
                JOIN LeaveTypes lt ON b.LeaveTypeId = lt.LeaveTypeId
                JOIN Employees e ON b.EmployeeId = e.EmployeeId
                LEFT JOIN Departments d ON e.DepartmentId = d.DepartmentId
                WHERE e.CompanyId = ? AND b.Year = ?
                """);

        if (departmentId != null && departmentId > 0) {
            sql.append(" AND e.DepartmentId = ?");
        }
        sql.append(" ORDER BY e.FullName ASC, lt.LeaveTypeId ASC");

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            ps.setInt(1, companyId);
            ps.setInt(2, year);
            if (departmentId != null && departmentId > 0) {
                ps.setInt(3, departmentId);
            }

            try (ResultSet rs = ps.executeQuery()) {
                List<LeaveBalance> list = new ArrayList<>();
                while (rs.next()) {
                    LeaveBalance b = mapLeaveBalance(rs);
                    b.setEmployeeCode(rs.getString("EmployeeCode"));
                    b.setEmployeeFullName(rs.getString("FullName"));
                    b.setDepartmentName(rs.getString("DepartmentName"));
                    list.add(b);
                }
                return list;
            }
        }
    }

    private LeaveBalance mapLeaveBalance(ResultSet rs) throws SQLException {
        LeaveBalance b = new LeaveBalance();
        b.setBalanceId(rs.getInt("BalanceId"));
        b.setEmployeeId(rs.getInt("EmployeeId"));
        b.setLeaveTypeId(rs.getInt("LeaveTypeId"));
        b.setYear(rs.getInt("Year"));
        b.setEntitledDays(rs.getBigDecimal("EntitledDays"));
        b.setUsedDays(rs.getBigDecimal("UsedDays"));
        b.setRemainingDays(rs.getBigDecimal("RemainingDays"));

        java.sql.Timestamp cTs = rs.getTimestamp("CreatedAt");
        if (cTs != null) b.setCreatedAt(cTs.toLocalDateTime());
        java.sql.Timestamp uTs = rs.getTimestamp("UpdatedAt");
        if (uTs != null) b.setUpdatedAt(uTs.toLocalDateTime());

        // Policy fields from joined LeaveTypes
        b.setLeaveTypeCode(rs.getString("TypeCode"));
        b.setLeaveTypeName(rs.getString("TypeName"));
        b.setIsPaid(rs.getBoolean("IsPaid"));
        b.setDefaultDaysPerYear(rs.getInt("DefaultDaysPerYear"));

        return b;
    }
}

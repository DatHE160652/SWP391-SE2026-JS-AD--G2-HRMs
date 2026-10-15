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
 * Data access operations for employee leave balance quotas and remaining days.
 */
public class LeaveBalanceDAO {

    /**
     * Retrieves all leave balances for a specific employee and calendar year,
     * including policy metadata joined from LeaveTypes.
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
     * Retrieves a single balance record by employee, leave type, and calendar year.
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
     * Deducts leave days upon approval by incrementing UsedDays within an external transaction.
     * Note: RemainingDays is a computed column (EntitledDays - UsedDays) and updates automatically.
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
     * Inserts an initial balance quota record for an employee.
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
     * Retrieves leave balances across the company or for a specific department for quota monitoring.
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

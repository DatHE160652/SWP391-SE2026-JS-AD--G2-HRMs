package com.hrms.dao;

import com.hrms.model.AttendanceExplanation;
import com.hrms.util.DBConnection;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for AttendanceExplanations (UC86, UC87).
 */
public class AttendanceExplanationDAO {

    public boolean createExplanation(AttendanceExplanation exp) throws SQLException {
        String sql = """
                INSERT INTO dbo.AttendanceExplanations
                (CompanyId, EmployeeId, AttendanceDate, ProposedCheckIn, ProposedCheckOut, ReasonType, ReasonDetail, Status, CreatedAt)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, exp.getCompanyId());
            ps.setInt(2, exp.getEmployeeId());
            ps.setDate(3, Date.valueOf(exp.getAttendanceDate()));
            if (exp.getProposedCheckIn() != null) {
                ps.setTime(4, Time.valueOf(exp.getProposedCheckIn()));
            } else {
                ps.setNull(4, Types.TIME);
            }
            if (exp.getProposedCheckOut() != null) {
                ps.setTime(5, Time.valueOf(exp.getProposedCheckOut()));
            } else {
                ps.setNull(5, Types.TIME);
            }
            ps.setString(6, exp.getReasonType());
            ps.setString(7, exp.getReasonDetail());
            ps.setString(8, "PENDING");
            ps.setTimestamp(9, Timestamp.valueOf(exp.getCreatedAt()));

            int rows = ps.executeUpdate();
            if (rows > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        exp.setExplanationId(rs.getInt(1));
                    }
                }
                return true;
            }
            return false;
        }
    }

    public AttendanceExplanation getExplanationById(int explanationId) throws SQLException {
        String sql = """
                SELECT ae.*, e.EmployeeCode, e.FullName AS EmployeeFullName,
                       d.DepartmentName, u.Username AS ReviewerName
                FROM dbo.AttendanceExplanations ae
                JOIN dbo.Employees e ON ae.EmployeeId = e.EmployeeId
                LEFT JOIN dbo.Departments d ON e.DepartmentId = d.DepartmentId
                LEFT JOIN dbo.Users u ON ae.ReviewerId = u.UserId
                WHERE ae.ExplanationId = ?
                """;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, explanationId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        }
        return null;
    }

    public List<AttendanceExplanation> getExplanationsByEmployee(int companyId, int employeeId, Integer month, Integer year) throws SQLException {
        StringBuilder sql = new StringBuilder("""
                SELECT ae.*, e.EmployeeCode, e.FullName AS EmployeeFullName,
                       d.DepartmentName, u.Username AS ReviewerName
                FROM dbo.AttendanceExplanations ae
                JOIN dbo.Employees e ON ae.EmployeeId = e.EmployeeId
                LEFT JOIN dbo.Departments d ON e.DepartmentId = d.DepartmentId
                LEFT JOIN dbo.Users u ON ae.ReviewerId = u.UserId
                WHERE ae.CompanyId = ? AND ae.EmployeeId = ?
                """);

        if (month != null && year != null) {
            sql.append(" AND MONTH(ae.AttendanceDate) = ? AND YEAR(ae.AttendanceDate) = ?");
        }
        sql.append(" ORDER BY ae.AttendanceDate DESC, ae.CreatedAt DESC");

        List<AttendanceExplanation> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            ps.setInt(1, companyId);
            ps.setInt(2, employeeId);
            if (month != null && year != null) {
                ps.setInt(3, month);
                ps.setInt(4, year);
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        }
        return list;
    }

    public List<AttendanceExplanation> getPendingExplanations(int companyId, Integer managerEmployeeId) throws SQLException {
        StringBuilder sql = new StringBuilder("""
                SELECT ae.*, e.EmployeeCode, e.FullName AS EmployeeFullName,
                       d.DepartmentName, u.Username AS ReviewerName
                FROM dbo.AttendanceExplanations ae
                JOIN dbo.Employees e ON ae.EmployeeId = e.EmployeeId
                LEFT JOIN dbo.Departments d ON e.DepartmentId = d.DepartmentId
                LEFT JOIN dbo.Users u ON ae.ReviewerId = u.UserId
                WHERE ae.CompanyId = ? AND ae.Status = 'PENDING'
                """);

        if (managerEmployeeId != null) {
            sql.append(" AND e.ManagerId = ?");
        }
        sql.append(" ORDER BY ae.CreatedAt ASC");

        List<AttendanceExplanation> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            ps.setInt(1, companyId);
            if (managerEmployeeId != null) {
                ps.setInt(2, managerEmployeeId);
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        }
        return list;
    }

    public boolean hasPendingExplanation(int companyId, int employeeId, LocalDate date) throws SQLException {
        String sql = """
                SELECT 1 FROM dbo.AttendanceExplanations
                WHERE CompanyId = ? AND EmployeeId = ? AND AttendanceDate = ? AND Status = 'PENDING'
                """;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, companyId);
            ps.setInt(2, employeeId);
            ps.setDate(3, Date.valueOf(date));
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public boolean updateStatus(int explanationId, String status, Integer reviewerId, String reviewerComment) throws SQLException {
        String sql = """
                UPDATE dbo.AttendanceExplanations
                SET Status = ?, ReviewerId = ?, ReviewerComment = ?, ReviewedAt = GETDATE()
                WHERE ExplanationId = ? AND Status = 'PENDING'
                """;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            if (reviewerId != null) {
                ps.setInt(2, reviewerId);
            } else {
                ps.setNull(2, Types.INTEGER);
            }
            ps.setString(3, reviewerComment);
            ps.setInt(4, explanationId);

            return ps.executeUpdate() > 0;
        }
    }

    public boolean cancelExplanation(int explanationId, int employeeId) throws SQLException {
        String sql = """
                UPDATE dbo.AttendanceExplanations
                SET Status = 'CANCELLED', ReviewedAt = GETDATE()
                WHERE ExplanationId = ? AND EmployeeId = ? AND Status = 'PENDING'
                """;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, explanationId);
            ps.setInt(2, employeeId);

            return ps.executeUpdate() > 0;
        }
    }

    private AttendanceExplanation mapRow(ResultSet rs) throws SQLException {
        AttendanceExplanation exp = new AttendanceExplanation();
        exp.setExplanationId(rs.getInt("ExplanationId"));
        exp.setCompanyId(rs.getInt("CompanyId"));
        exp.setEmployeeId(rs.getInt("EmployeeId"));
        Date d = rs.getDate("AttendanceDate");
        if (d != null) exp.setAttendanceDate(d.toLocalDate());

        Time in = rs.getTime("ProposedCheckIn");
        if (in != null) exp.setProposedCheckIn(in.toLocalTime());

        Time out = rs.getTime("ProposedCheckOut");
        if (out != null) exp.setProposedCheckOut(out.toLocalTime());

        exp.setReasonType(rs.getString("ReasonType"));
        exp.setReasonDetail(rs.getString("ReasonDetail"));
        exp.setStatus(rs.getString("Status"));

        int rId = rs.getInt("ReviewerId");
        if (!rs.wasNull()) exp.setReviewerId(rId);

        exp.setReviewerComment(rs.getString("ReviewerComment"));
        Timestamp revAt = rs.getTimestamp("ReviewedAt");
        if (revAt != null) exp.setReviewedAt(revAt.toLocalDateTime());

        Timestamp crAt = rs.getTimestamp("CreatedAt");
        if (crAt != null) exp.setCreatedAt(crAt.toLocalDateTime());

        exp.setEmployeeCode(rs.getString("EmployeeCode"));
        exp.setEmployeeFullName(rs.getString("EmployeeFullName"));
        exp.setDepartmentName(rs.getString("DepartmentName"));
        exp.setReviewerDisplayName(rs.getString("ReviewerName"));

        return exp;
    }
}


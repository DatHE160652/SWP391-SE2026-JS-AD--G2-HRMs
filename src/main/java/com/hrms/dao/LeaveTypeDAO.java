package com.hrms.dao;

import com.hrms.model.LeaveType;
import com.hrms.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Lớp truy xuất cơ sở dữ liệu (DAO) cho chính sách và danh mục loại nghỉ phép:
 * - Tra cứu danh mục loại nghỉ phép của công ty theo ID hoặc mã loại phép
 * (TypeCode).
 * - Thêm mới chính sách loại nghỉ phép, cập nhật số ngày mặc định và chế độ
 * tính lương (IsPaid).
 * - Kích hoạt hoặc vô hiệu hóa trạng thái hoạt động của từng loại phép.
 */
public class LeaveTypeDAO {

    /**
     * Lấy toàn bộ danh mục các loại nghỉ phép được cấu hình cho một công ty.
     */
    public List<LeaveType> getLeaveTypesByCompany(int companyId) throws SQLException {
        String sql = """
                SELECT LeaveTypeId, CompanyId, TypeCode, TypeName, DefaultDaysPerYear, IsPaid, IsActive, CreatedAt
                FROM LeaveTypes
                WHERE CompanyId = ?
                ORDER BY LeaveTypeId ASC
                """;

        try (Connection conn = DBConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, companyId);
            try (ResultSet rs = ps.executeQuery()) {
                List<LeaveType> list = new ArrayList<>();
                while (rs.next()) {
                    list.add(mapLeaveType(rs));
                }
                return list;
            }
        }
    }

    /**
     * Lấy thông tin chi tiết một loại nghỉ phép theo khóa chính (LeaveTypeId).
     */
    public LeaveType getLeaveTypeById(int leaveTypeId) throws SQLException {
        String sql = """
                SELECT LeaveTypeId, CompanyId, TypeCode, TypeName, DefaultDaysPerYear, IsPaid, IsActive, CreatedAt
                FROM LeaveTypes
                WHERE LeaveTypeId = ?
                """;

        try (Connection conn = DBConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, leaveTypeId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapLeaveType(rs);
                }
            }
        }
        return null;
    }

    /**
     * Tìm kiếm loại nghỉ phép theo mã loại (VD: ANNUAL, SICK) trong phạm vi công
     * ty.
     */
    public LeaveType getLeaveTypeByCode(int companyId, String typeCode) throws SQLException {
        String sql = """
                SELECT LeaveTypeId, CompanyId, TypeCode, TypeName, DefaultDaysPerYear, IsPaid, IsActive, CreatedAt
                FROM LeaveTypes
                WHERE CompanyId = ? AND TypeCode = ?
                """;

        try (Connection conn = DBConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, companyId);
            ps.setString(2, typeCode);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapLeaveType(rs);
                }
            }
        }
        return null;
    }

    /**
     * Tạo mới một chính sách loại nghỉ phép cho công ty.
     */
    public int createLeaveType(LeaveType leaveType) throws SQLException {
        String sql = """
                INSERT INTO LeaveTypes (CompanyId, TypeCode, TypeName, DefaultDaysPerYear, IsPaid, CreatedAt)
                VALUES (?, ?, ?, ?, ?, SYSUTCDATETIME())
                """;

        try (Connection conn = DBConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, leaveType.getCompanyId());
            ps.setString(2, leaveType.getTypeCode().trim().toUpperCase());
            ps.setString(3, leaveType.getTypeName().trim());
            ps.setInt(4, leaveType.getDefaultDaysPerYear() != null ? leaveType.getDefaultDaysPerYear() : 12);
            ps.setBoolean(5, leaveType.getPaid() != null ? leaveType.getPaid() : true);

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
     * Cập nhật thông tin chính sách loại nghỉ phép (tên, số ngày mặc định, chế độ
     * tính lương).
     */
    public boolean updateLeaveType(LeaveType leaveType) throws SQLException {
        String sql = """
                UPDATE LeaveTypes
                SET TypeName = ?, DefaultDaysPerYear = ?, IsPaid = ?
                WHERE LeaveTypeId = ? AND CompanyId = ?
                """;

        try (Connection conn = DBConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, leaveType.getTypeName().trim());
            ps.setInt(2, leaveType.getDefaultDaysPerYear() != null ? leaveType.getDefaultDaysPerYear() : 12);
            ps.setBoolean(3, leaveType.getPaid() != null ? leaveType.getPaid() : true);
            ps.setInt(4, leaveType.getLeaveTypeId());
            ps.setInt(5, leaveType.getCompanyId());

            return ps.executeUpdate() > 0;
        }
    }

    /**
     * Bật hoặc tắt trạng thái hoạt động (kích hoạt / vô hiệu hóa) của loại nghỉ
     * phép.
     */
    public boolean setLeaveTypeActive(int leaveTypeId, int companyId, boolean active) throws SQLException {
        String sql = "UPDATE LeaveTypes SET IsActive=? WHERE LeaveTypeId=? AND CompanyId=?";
        try (Connection connection = DBConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setBoolean(1, active);
            statement.setInt(2, leaveTypeId);
            statement.setInt(3, companyId);
            return statement.executeUpdate() == 1;
        }
    }

    private LeaveType mapLeaveType(ResultSet rs) throws SQLException {
        LeaveType type = new LeaveType();
        type.setLeaveTypeId(rs.getInt("LeaveTypeId"));
        type.setCompanyId(rs.getInt("CompanyId"));
        type.setTypeCode(rs.getString("TypeCode"));
        type.setTypeName(rs.getString("TypeName"));
        type.setActive(rs.getBoolean("IsActive"));
        type.setDefaultDaysPerYear(rs.getInt("DefaultDaysPerYear"));
        type.setPaid(rs.getBoolean("IsPaid"));
        type.setIsPaid(rs.getBoolean("IsPaid"));
        java.sql.Timestamp ts = rs.getTimestamp("CreatedAt");
        if (ts != null) {
            type.setCreatedAt(ts.toLocalDateTime());
        }
        return type;
    }
}

package com.hrms.dao;

import com.hrms.service.FieldValidationException;
import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;

public class OrganizationAllowanceDAO {
    public static void validate(BigDecimal amount) {
        validate(amount,"positionAllowance");
    }

    public static void validate(BigDecimal amount,String field) {
        if (amount != null && (amount.signum()<0 || amount.scale()>2 || amount.precision()-amount.scale()>16))
            throw new FieldValidationException(field, "Phụ cấp phải không âm, tối đa 2 chữ số thập phân và 16 chữ số nguyên.");
    }

    public BigDecimal positionAllowance(Connection connection, int companyId, Integer positionId, LocalDate date) throws SQLException {
        if (positionId == null) return null;
        try (PreparedStatement statement=connection.prepareStatement("""
                SELECT COUNT(a.AllowancePolicyId) AS PolicyCount,MAX(a.Amount) AS LevelAllowance
                FROM Positions p LEFT JOIN ResponsibilityAllowancePolicies a
                  ON a.CompanyId=p.CompanyId AND a.ResponsibilityLevelId=p.ResponsibilityLevelId
                  AND a.EffectiveFrom<=? AND (a.EffectiveTo IS NULL OR a.EffectiveTo>?)
                WHERE p.CompanyId=? AND p.PositionId=?
                GROUP BY p.PositionId
                """)) {
            statement.setDate(1,Date.valueOf(date)); statement.setDate(2,Date.valueOf(date));
            statement.setInt(3,companyId); statement.setInt(4,positionId);
            try (ResultSet rows=statement.executeQuery()) {
                if (!rows.next()) throw new FieldValidationException("positionId","Không tìm thấy chức danh trong công ty.");
                if (rows.getInt("PolicyCount")>1) throw new FieldValidationException("responsibilityLevelId","Có nhiều chính sách phụ cấp cùng hiệu lực. Quản trị viên cần xử lý trước.");
                return rows.getBigDecimal("LevelAllowance");
            }
        }
    }

    public BigDecimal employeeAllowance(Connection connection,int companyId,int employeeId,LocalDate date) throws SQLException {
        try (PreparedStatement statement=connection.prepareStatement("""
                SELECT PositionId FROM EmployeePositionAssignments WHERE CompanyId=? AND EmployeeId=?
                    AND EffectiveFrom<=? AND (EffectiveTo IS NULL OR EffectiveTo>?)
                """)) {
            statement.setInt(1,companyId); statement.setInt(2,employeeId);
            statement.setDate(3,Date.valueOf(date)); statement.setDate(4,Date.valueOf(date));
            try (ResultSet rows=statement.executeQuery()) {
                if (!rows.next()) return null;
                int positionId=rows.getInt(1);
                if (rows.next()) throw new IllegalArgumentException("Có nhiều phân công cùng hiệu lực.");
                return positionAllowance(connection,companyId,positionId,date);
            }
        }
    }

    public void setLevelAllowance(Connection connection,int companyId,int levelId,BigDecimal amount) throws SQLException {
        validate(amount,"defaultAllowance");
        if (amount==null) throw new FieldValidationException("defaultAllowance","Nhập phụ cấp mặc định; dùng 0 nếu không có phụ cấp.");
        try (PreparedStatement statement=connection.prepareStatement("""
                SELECT COUNT(*) FROM ResponsibilityAllowancePolicies WITH (UPDLOCK,HOLDLOCK)
                WHERE CompanyId=? AND ResponsibilityLevelId=? AND EffectiveFrom<=CONVERT(date,SYSDATETIME())
                    AND (EffectiveTo IS NULL OR EffectiveTo>CONVERT(date,SYSDATETIME()))
                """)) {
            statement.setInt(1,companyId); statement.setInt(2,levelId);
            try (ResultSet rows=statement.executeQuery()) {
                rows.next();
                if (rows.getInt(1)>1) throw new FieldValidationException("defaultAllowance","Các chính sách phụ cấp đang bị chồng lấn. Cần xử lý trước khi thay đổi.");
            }
        }
        try (PreparedStatement statement=connection.prepareStatement("""
                UPDATE ResponsibilityAllowancePolicies SET EffectiveTo=CONVERT(date,SYSDATETIME())
                WHERE CompanyId=? AND ResponsibilityLevelId=? AND EffectiveFrom<CONVERT(date,SYSDATETIME())
                    AND (EffectiveTo IS NULL OR EffectiveTo>CONVERT(date,SYSDATETIME()))
                """)) {
            statement.setInt(1,companyId); statement.setInt(2,levelId); statement.executeUpdate();
        }
        try (PreparedStatement statement=connection.prepareStatement("""
                UPDATE ResponsibilityAllowancePolicies SET Amount=? WHERE CompanyId=? AND ResponsibilityLevelId=?
                    AND EffectiveFrom=CONVERT(date,SYSDATETIME())
                """)) {
            statement.setBigDecimal(1,amount); statement.setInt(2,companyId); statement.setInt(3,levelId);
            if (statement.executeUpdate()>0) return;
        }
        if (amount!=null) {
            try (PreparedStatement statement=connection.prepareStatement("""
                    INSERT INTO ResponsibilityAllowancePolicies(CompanyId,ResponsibilityLevelId,Amount,EffectiveFrom,EffectiveTo)
                    SELECT ?,?,?,CONVERT(date,SYSDATETIME()),MIN(EffectiveFrom)
                    FROM ResponsibilityAllowancePolicies WHERE CompanyId=? AND ResponsibilityLevelId=?
                        AND EffectiveFrom>CONVERT(date,SYSDATETIME())
                    """)) {
                statement.setInt(1,companyId); statement.setInt(2,levelId); statement.setBigDecimal(3,amount);
                statement.setInt(4,companyId); statement.setInt(5,levelId); statement.executeUpdate();
            }
        }
    }
}

package com.hrms.dao;

import com.hrms.util.DBConnection;
import com.hrms.util.ValidationSupport;
import java.sql.*;

public class PositionCatalogDAO {
    public boolean isEmployeeLevel(int companyId, Integer id) throws SQLException {
        return exists("SELECT 1 FROM ResponsibilityLevels WHERE CompanyId=? AND ResponsibilityLevelId=? AND (UPPER(LTRIM(RTRIM(LevelCode))) IN ('EMP','EMPLOYEE') OR UPPER(LTRIM(RTRIM(LevelName)))='EMPLOYEE')", companyId, id);
    }

    public boolean responsibilityLevelExists(int companyId, Integer id, boolean activeOnly) throws SQLException {
        return exists("SELECT 1 FROM ResponsibilityLevels WHERE CompanyId=? AND ResponsibilityLevelId=?" + (activeOnly ? " AND IsActive=1" : ""), companyId, id);
    }

    private boolean exists(String sql, int companyId, Integer id) throws SQLException {
        if (id == null || id <= 0) return false;
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, companyId);
            statement.setInt(2, id);
            try (ResultSet rows = statement.executeQuery()) { return rows.next(); }
        }
    }

    public String getCatalogJson(int companyId) throws SQLException {
        try (Connection connection = DBConnection.getConnection()) {
            String levels = options(connection, """
                    SELECT r.ResponsibilityLevelId AS Id,r.LevelName AS Name,r.LevelCode,r.IsActive,
                        CASE WHEN policy.PolicyCount=1 THEN policy.Amount END AS Amount
                    FROM ResponsibilityLevels r
                    OUTER APPLY (
                        SELECT COUNT(*) AS PolicyCount,MAX(a.Amount) AS Amount
                        FROM ResponsibilityAllowancePolicies a
                        WHERE a.CompanyId=r.CompanyId AND a.ResponsibilityLevelId=r.ResponsibilityLevelId
                        AND a.EffectiveFrom<=CONVERT(date,SYSDATETIME())
                        AND (a.EffectiveTo IS NULL OR a.EffectiveTo>CONVERT(date,SYSDATETIME()))
                    ) policy
                    WHERE r.CompanyId=? ORDER BY r.RankOrder
                    """, companyId, true);
            return "{\"responsibilityLevels\":" + levels + "}";
        }
    }

    private String options(Connection connection, String sql, int companyId, boolean allowance) throws SQLException {
        StringBuilder json = new StringBuilder("[");
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, companyId);
            try (ResultSet rows = statement.executeQuery()) {
                while (rows.next()) {
                    if (json.length()>1) json.append(',');
                    json.append("{\"id\":").append(rows.getInt("Id"))
                        .append(",\"name\":").append(ValidationSupport.quote(rows.getString("Name")))
                        .append(",\"active\":").append(rows.getBoolean("IsActive"))
                        .append(",\"code\":").append(ValidationSupport.quote(rows.getString("LevelCode")));
                    if (allowance) json.append(",\"allowance\":").append(rows.getBigDecimal("Amount"));
                    json.append('}');
                }
            }
        }
        return json.append(']').toString();
    }
}

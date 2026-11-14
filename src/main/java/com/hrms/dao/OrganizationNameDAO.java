package com.hrms.dao;

import com.hrms.util.DBConnection;
import java.sql.*;

/** Names are unique within each company and catalogue, including inactive records. */
public final class OrganizationNameDAO {
    public enum Catalog {
        DEPARTMENT("Departments", "DepartmentName", "DepartmentId"),
        POSITION("Positions", "PositionTitle", "PositionId"),
        RESPONSIBILITY_LEVEL("ResponsibilityLevels", "LevelName", "ResponsibilityLevelId");

        final String table, nameColumn, idColumn;
        Catalog(String table, String nameColumn, String idColumn) {
            this.table = table;
            this.nameColumn = nameColumn;
            this.idColumn = idColumn;
        }
    }

    public boolean exists(Catalog catalog, int companyId, String name, Integer excludeId) throws SQLException {
        String sql = "SELECT 1 FROM " + catalog.table + " WHERE CompanyId=? AND LTRIM(RTRIM("
                + catalog.nameColumn + ")) COLLATE Latin1_General_100_CI_AS = ? COLLATE Latin1_General_100_CI_AS"
                + " AND (? IS NULL OR " + catalog.idColumn + "<>?)";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, companyId);
            statement.setString(2, name.strip());
            statement.setObject(3, excludeId, Types.INTEGER);
            statement.setObject(4, excludeId, Types.INTEGER);
            try (ResultSet rows = statement.executeQuery()) { return rows.next(); }
        }
    }
}

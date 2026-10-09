package com.hrms.dao;

import com.hrms.model.Position;
import com.hrms.util.DBConnection;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class PositionDAO {
    public List<Position> getAllPositions(int companyId) throws SQLException {
        ensureDepartmentColumn();
        String sql = """
                SELECT p.PositionId, p.CompanyId, p.DepartmentId, d.DepartmentName,
                       p.PositionTitle, p.Description, p.CreatedAt
                FROM Positions p
                LEFT JOIN Departments d ON d.DepartmentId = p.DepartmentId
                WHERE p.CompanyId = ?
                ORDER BY p.PositionId DESC
                """;

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, companyId);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<Position> positions = new ArrayList<>();
                while (resultSet.next()) {
                    positions.add(mapPosition(resultSet));
                }
                return positions;
            }
        }
    }

    public Position getPositionById(int id) throws SQLException {
        ensureDepartmentColumn();
        String sql = """
                SELECT p.PositionId, p.CompanyId, p.DepartmentId, d.DepartmentName,
                       p.PositionTitle, p.Description, p.CreatedAt
                FROM Positions p
                LEFT JOIN Departments d ON d.DepartmentId = p.DepartmentId
                WHERE p.PositionId = ?
                """;

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? mapPosition(resultSet) : null;
            }
        }
    }

    public boolean insertPosition(Position position) throws SQLException {
        ensureDepartmentColumn();
        String sql = """
                INSERT INTO Positions (CompanyId, DepartmentId, PositionTitle, Description, CreatedAt)
                VALUES (?, ?, ?, ?, SYSUTCDATETIME())
                """;

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, position.getCompanyId());
            statement.setInt(2, position.getDepartmentId());
            statement.setString(3, position.getPositionTitle());
            statement.setString(4, position.getDescription());
            return statement.executeUpdate() > 0;
        }
    }

    public boolean updatePosition(Position position) throws SQLException {
        ensureDepartmentColumn();
        String sql = """
                UPDATE Positions
                SET DepartmentId = ?, PositionTitle = ?, Description = ?
                WHERE PositionId = ?
                """;

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, position.getDepartmentId());
            statement.setString(2, position.getPositionTitle());
            statement.setString(3, position.getDescription());
            statement.setInt(4, position.getPositionId());
            return statement.executeUpdate() > 0;
        }
    }

    public boolean deletePosition(int id) throws SQLException {
        String sql = "DELETE FROM Positions WHERE PositionId = ?";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            return statement.executeUpdate() > 0;
        }
    }

    public boolean positionTitleExists(int companyId, int departmentId, String positionTitle, Integer excludePositionId)
            throws SQLException {
        ensureDepartmentColumn();
        String sql = """
                SELECT COUNT(*)
                FROM Positions
                WHERE CompanyId = ? AND DepartmentId = ? AND PositionTitle = ?
                  AND (? IS NULL OR PositionId <> ?)
                """;

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, companyId);
            statement.setInt(2, departmentId);
            statement.setString(3, positionTitle);
            if (excludePositionId == null) {
                statement.setNull(4, java.sql.Types.INTEGER);
                statement.setNull(5, java.sql.Types.INTEGER);
            } else {
                statement.setInt(4, excludePositionId);
                statement.setInt(5, excludePositionId);
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() && resultSet.getInt(1) > 0;
            }
        }
    }

    public boolean departmentExists(int companyId, int departmentId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM Departments WHERE CompanyId = ? AND DepartmentId = ?";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, companyId);
            statement.setInt(2, departmentId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() && resultSet.getInt(1) > 0;
            }
        }
    }

    public void ensureDepartmentColumn() throws SQLException {
        try (Connection connection = DBConnection.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            boolean exists;
            try (ResultSet columns = metaData.getColumns(null, null, "Positions", "DepartmentId")) {
                exists = columns.next();
            }
            if (exists) {
                backfillDepartmentFromTitle(connection);
                return;
            }
            try (Statement statement = connection.createStatement()) {
                statement.executeUpdate("ALTER TABLE Positions ADD DepartmentId INT NULL");
            }
            backfillDepartmentFromTitle(connection);
        }
    }

    private void backfillDepartmentFromTitle(Connection connection) throws SQLException {
        String sql = """
                UPDATE p
                SET DepartmentId = d.DepartmentId
                FROM Positions p
                INNER JOIN Departments d ON d.CompanyId = p.CompanyId
                WHERE p.DepartmentId IS NULL
                  AND (
                      LOWER(p.PositionTitle) = LOWER(d.DepartmentCode)
                      OR LOWER(p.PositionTitle) LIKE LOWER(d.DepartmentCode) + ' %'
                      OR LOWER(p.PositionTitle) = LOWER(d.DepartmentName)
                      OR LOWER(p.PositionTitle) LIKE LOWER(d.DepartmentName) + ' %'
                  )
                """;
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate(sql);
        }
    }

    private Position mapPosition(ResultSet resultSet) throws SQLException {
        Position position = new Position();
        position.setPositionId(resultSet.getInt("PositionId"));
        position.setCompanyId(resultSet.getInt("CompanyId"));
        int departmentId = resultSet.getInt("DepartmentId");
        position.setDepartmentId(resultSet.wasNull() ? null : departmentId);
        position.setDepartmentName(resultSet.getString("DepartmentName"));
        position.setPositionTitle(resultSet.getString("PositionTitle"));
        position.setDescription(resultSet.getString("Description"));
        Timestamp createdAt = resultSet.getTimestamp("CreatedAt");
        position.setCreatedAt(createdAt == null ? null : createdAt.toLocalDateTime());
        return position;
    }
}

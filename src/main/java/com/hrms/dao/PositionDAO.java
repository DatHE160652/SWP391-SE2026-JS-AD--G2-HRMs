package com.hrms.dao;

import com.hrms.model.Position;
import com.hrms.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class PositionDAO {
    public List<Position> getAllPositions(int companyId) throws SQLException {
        String sql = """
                SELECT PositionId, CompanyId, PositionTitle, Description, CreatedAt
                FROM Positions
                WHERE CompanyId = ?
                ORDER BY PositionId DESC
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
        String sql = """
                SELECT PositionId, CompanyId, PositionTitle, Description, CreatedAt
                FROM Positions
                WHERE PositionId = ?
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
        String sql = """
                INSERT INTO Positions (CompanyId, PositionTitle, Description, CreatedAt)
                VALUES (?, ?, ?, SYSUTCDATETIME())
                """;

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, position.getCompanyId());
            statement.setString(2, position.getPositionTitle());
            statement.setString(3, position.getDescription());
            return statement.executeUpdate() > 0;
        }
    }

    public boolean updatePosition(Position position) throws SQLException {
        String sql = """
                UPDATE Positions
                SET PositionTitle = ?, Description = ?
                WHERE PositionId = ?
                """;

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, position.getPositionTitle());
            statement.setString(2, position.getDescription());
            statement.setInt(3, position.getPositionId());
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

    public boolean positionTitleExists(int companyId, String positionTitle, Integer excludePositionId)
            throws SQLException {
        String sql = """
                SELECT COUNT(*)
                FROM Positions
                WHERE CompanyId = ? AND PositionTitle = ?
                  AND (? IS NULL OR PositionId <> ?)
                """;

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, companyId);
            statement.setString(2, positionTitle);
            if (excludePositionId == null) {
                statement.setNull(3, java.sql.Types.INTEGER);
                statement.setNull(4, java.sql.Types.INTEGER);
            } else {
                statement.setInt(3, excludePositionId);
                statement.setInt(4, excludePositionId);
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() && resultSet.getInt(1) > 0;
            }
        }
    }

    private Position mapPosition(ResultSet resultSet) throws SQLException {
        Position position = new Position();
        position.setPositionId(resultSet.getInt("PositionId"));
        position.setCompanyId(resultSet.getInt("CompanyId"));
        position.setPositionTitle(resultSet.getString("PositionTitle"));
        position.setDescription(resultSet.getString("Description"));
        Timestamp createdAt = resultSet.getTimestamp("CreatedAt");
        position.setCreatedAt(createdAt == null ? null : createdAt.toLocalDateTime());
        return position;
    }
}

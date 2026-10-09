package com.hrms.service;

import com.hrms.dao.PositionDAO;
import com.hrms.model.Position;
import com.hrms.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

public class PositionService {
    private final PositionDAO positionDAO = new PositionDAO();

    public List<Position> getPositions() throws SQLException {
        positionDAO.ensureDepartmentColumn();
        return positionDAO.getAllPositions(getOrCreateDefaultCompanyId());
    }

    public Position getPositionDetail(int id) throws SQLException {
        positionDAO.ensureDepartmentColumn();
        return positionDAO.getPositionById(id);
    }

    public boolean createPosition(Position position) throws SQLException {
        positionDAO.ensureDepartmentColumn();
        position.setCompanyId(getOrCreateDefaultCompanyId());
        validate(position, null);
        return positionDAO.insertPosition(position);
    }

    public boolean updatePosition(Position position) throws SQLException {
        positionDAO.ensureDepartmentColumn();
        Position existing = positionDAO.getPositionById(position.getPositionId());
        if (existing == null) {
            throw new IllegalArgumentException("Position not found.");
        }
        position.setCompanyId(existing.getCompanyId());
        validate(position, position.getPositionId());
        return positionDAO.updatePosition(position);
    }

    public boolean deletePosition(int id) throws SQLException {
        return positionDAO.deletePosition(id);
    }

    private void validate(Position position, Integer excludePositionId) throws SQLException {
        if (position.getDepartmentId() == null || position.getDepartmentId() <= 0) {
            throw new IllegalArgumentException("Department is required.");
        }
        if (!positionDAO.departmentExists(position.getCompanyId(), position.getDepartmentId())) {
            throw new IllegalArgumentException("Department does not exist.");
        }
        if (position.getPositionTitle() == null || position.getPositionTitle().isBlank()) {
            throw new IllegalArgumentException("Position title is required.");
        }
        if (positionDAO.positionTitleExists(position.getCompanyId(), position.getDepartmentId(),
                position.getPositionTitle(), excludePositionId)) {
            throw new IllegalArgumentException("Position title already exists in this department.");
        }
    }

    private int getOrCreateDefaultCompanyId() throws SQLException {
        try (Connection connection = DBConnection.getConnection()) {
            String selectSql = "SELECT TOP 1 CompanyId FROM Companies ORDER BY CompanyId";

            try (Statement statement = connection.createStatement();
                 ResultSet resultSet = statement.executeQuery(selectSql)) {
                if (resultSet.next()) {
                    return resultSet.getInt("CompanyId");
                }
            }

            String insertSql = """
                    INSERT INTO Companies (WorkspaceCode, CompanyName, ContactEmail)
                    VALUES (?, ?, ?)
                    """;

            try (PreparedStatement statement = connection.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
                statement.setString(1, "demo");
                statement.setString(2, "Demo Company");
                statement.setString(3, "admin@hrms.local");
                statement.executeUpdate();

                try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        return generatedKeys.getInt(1);
                    }
                }
            }
        }

        throw new SQLException("Default company could not be created.");
    }
}

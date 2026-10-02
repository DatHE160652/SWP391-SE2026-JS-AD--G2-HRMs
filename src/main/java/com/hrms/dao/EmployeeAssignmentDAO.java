package com.hrms.dao;

import com.hrms.model.EmployeeAssignment;
import com.hrms.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class EmployeeAssignmentDAO {
    public boolean assignDepartment(int employeeId, int departmentId) throws SQLException {
        String sql = "UPDATE Employees SET DepartmentId = ?, UpdatedAt = SYSUTCDATETIME() WHERE EmployeeId = ?";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, departmentId);
            statement.setInt(2, employeeId);
            return statement.executeUpdate() > 0;
        }
    }

    public boolean assignPosition(int employeeId, int positionId) throws SQLException {
        String sql = "UPDATE Employees SET PositionId = ?, UpdatedAt = SYSUTCDATETIME() WHERE EmployeeId = ?";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, positionId);
            statement.setInt(2, employeeId);
            return statement.executeUpdate() > 0;
        }
    }

    public boolean assignManager(int employeeId, int managerId) throws SQLException {
        String sql = "UPDATE Employees SET ManagerId = ?, UpdatedAt = SYSUTCDATETIME() WHERE EmployeeId = ?";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, managerId);
            statement.setInt(2, employeeId);
            return statement.executeUpdate() > 0;
        }
    }

    public EmployeeAssignment getEmployeeOrganization(int employeeId) throws SQLException {
        String sql = """
                SELECT EmployeeId, DepartmentId, PositionId, ManagerId
                FROM Employees
                WHERE EmployeeId = ?
                """;

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, employeeId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return null;
                }
                return new EmployeeAssignment(
                        resultSet.getInt("EmployeeId"),
                        getNullableInt(resultSet, "DepartmentId"),
                        getNullableInt(resultSet, "PositionId"),
                        getNullableInt(resultSet, "ManagerId")
                );
            }
        }
    }

    public boolean employeeExists(int employeeId) throws SQLException {
        return recordExists("SELECT COUNT(*) FROM Employees WHERE EmployeeId = ?", employeeId);
    }

    public boolean departmentExists(int departmentId) throws SQLException {
        return recordExists("SELECT COUNT(*) FROM Departments WHERE DepartmentId = ?", departmentId);
    }

    public boolean positionExists(int positionId) throws SQLException {
        return recordExists("SELECT COUNT(*) FROM Positions WHERE PositionId = ?", positionId);
    }

    private boolean recordExists(String sql, int id) throws SQLException {
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() && resultSet.getInt(1) > 0;
            }
        }
    }

    private Integer getNullableInt(ResultSet resultSet, String columnName) throws SQLException {
        int value = resultSet.getInt(columnName);
        return resultSet.wasNull() ? null : value;
    }
}

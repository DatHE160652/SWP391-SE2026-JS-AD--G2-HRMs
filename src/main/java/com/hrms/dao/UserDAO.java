package com.hrms.dao;

import com.hrms.model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class UserDAO {
    private static final String EMPLOYEE_ROLE_CODE = "EMPLOYEE";

    public int createEmployeeAccount(Connection connection, User user) throws SQLException {
        String sql = """
                INSERT INTO Users (
                    CompanyId, RoleId, Username, Email, PasswordHash, IsActive, IsLocked
                )
                VALUES (?, ?, ?, ?, ?, 1, 0)
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setInt(1, user.getCompanyId());
            statement.setInt(2, getEmployeeRoleId(connection));
            statement.setString(3, user.getUsername());
            statement.setString(4, user.getEmail());
            statement.setString(5, user.getPasswordHash());

            int affectedRows = statement.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Creating employee account failed.");
            }

            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getInt(1);
                }
            }

            throw new SQLException("Creating employee account failed, no generated UserId returned.");
        }
    }

    public boolean usernameExists(Connection connection, String username) throws SQLException {
        String sql = "SELECT COUNT(*) FROM Users WHERE Username = ?";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, username);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() && resultSet.getInt(1) > 0;
            }
        }
    }

    private int getEmployeeRoleId(Connection connection) throws SQLException {
        String sql = "SELECT RoleId FROM Roles WHERE RoleCode = ?";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, EMPLOYEE_ROLE_CODE);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getInt("RoleId");
                }
            }
        }

        String insertSql = """
                INSERT INTO Roles (RoleCode, RoleName, Description)
                VALUES ('EMPLOYEE', N'Nhân viên', N'Tài khoản nhân viên được tạo tự động')
                """;

        try (PreparedStatement statement = connection.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
            statement.executeUpdate();
            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getInt(1);
                }
            }
        }

        throw new SQLException("Role EMPLOYEE was not found and could not be created.");
    }
}

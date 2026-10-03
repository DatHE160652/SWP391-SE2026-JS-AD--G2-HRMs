package com.hrms.dao;

import com.hrms.model.User;
import com.hrms.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;

public class UserDAO {
    private static final String EMPLOYEE_ROLE_CODE = "EMPLOYEE";

    public User findSystemAdminByIdentifier(String identifier) throws SQLException {
        String sql = """
                SELECT u.UserId, u.CompanyId, u.RoleId, r.RoleCode, u.Username, u.Email,
                       u.DisplayName, u.PasswordHash, u.IsActive, u.IsLocked,
                       u.FailedLoginAttempts, u.LockedUntil, u.LastLoginAt, u.CreatedAt, u.UpdatedAt
                FROM Users u
                INNER JOIN Roles r ON r.RoleId = u.RoleId
                WHERE (u.Username = ? OR u.EmailNormalized = LOWER(LTRIM(RTRIM(?))))
                  AND r.RoleCode = 'SYSTEM_ADMIN'
                  AND u.CompanyId IS NULL
                """;
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, identifier);
            statement.setString(2, identifier);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? mapUser(resultSet) : null;
            }
        }
    }

    public boolean isSystemAdminAuthorized(int userId) throws SQLException {
        String sql = """
                SELECT COUNT(*)
                FROM Users u
                INNER JOIN Roles r ON r.RoleId = u.RoleId
                WHERE u.UserId = ?
                  AND u.CompanyId IS NULL
                  AND r.RoleCode = 'SYSTEM_ADMIN'
                  AND u.IsActive = 1
                  AND u.IsLocked = 0
                  AND (u.LockedUntil IS NULL OR u.LockedUntil <= SYSUTCDATETIME())
                """;
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, userId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() && resultSet.getInt(1) == 1;
            }
        }
    }

    public int createEmployeeAccount(Connection connection, User user) throws SQLException {
        String sql = """
                INSERT INTO Users (
                    CompanyId, RoleId, Username, Email, DisplayName, PasswordHash, IsActive, IsLocked
                )
                VALUES (?, ?, ?, ?, ?, ?, 1, 0)
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setInt(1, user.getCompanyId());
            statement.setInt(2, getEmployeeRoleId(connection));
            statement.setString(3, user.getUsername());
            statement.setString(4, user.getEmail());
            statement.setString(5, user.getDisplayName() == null ? user.getUsername() : user.getDisplayName());
            statement.setString(6, user.getPasswordHash());

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

    private User mapUser(ResultSet resultSet) throws SQLException {
        User user = new User();
        user.setUserId(resultSet.getInt("UserId"));
        user.setCompanyId((Integer) resultSet.getObject("CompanyId"));
        user.setRoleId(resultSet.getInt("RoleId"));
        user.setRoleCode(resultSet.getString("RoleCode"));
        user.setUsername(resultSet.getString("Username"));
        user.setEmail(resultSet.getString("Email"));
        user.setDisplayName(resultSet.getString("DisplayName"));
        user.setPasswordHash(resultSet.getString("PasswordHash"));
        user.setActive(resultSet.getBoolean("IsActive"));
        user.setLocked(resultSet.getBoolean("IsLocked"));
        user.setFailedLoginAttempts(resultSet.getInt("FailedLoginAttempts"));
        user.setLockedUntil(toLocalDateTime(resultSet.getTimestamp("LockedUntil")));
        user.setLastLoginAt(toLocalDateTime(resultSet.getTimestamp("LastLoginAt")));
        user.setCreatedAt(toLocalDateTime(resultSet.getTimestamp("CreatedAt")));
        user.setUpdatedAt(toLocalDateTime(resultSet.getTimestamp("UpdatedAt")));
        return user;
    }

    private java.time.LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
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

package com.hrms.dao;

import com.hrms.model.TenantAccount;
import com.hrms.model.User;
import com.hrms.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;

public class UserDAO {
    private static final String EMPLOYEE_ROLE_CODE = "EMPLOYEE";
    private static final String AUTH_WRITE_OPTIONS = """
            SET ANSI_NULLS ON;
            SET QUOTED_IDENTIFIER ON;
            SET ANSI_PADDING ON;
            SET ANSI_WARNINGS ON;
            SET ARITHABORT ON;
            SET CONCAT_NULL_YIELDS_NULL ON;
            SET NUMERIC_ROUNDABORT OFF;
            """;

    public int createEmployeeAccount(Connection connection, User user) throws SQLException {
        String sql = """
                INSERT INTO Users (CompanyId, RoleId, Username, Email, DisplayName, PasswordHash, IsActive, IsLocked)
                VALUES (?, ?, ?, ?, ?, ?, 1, 0)
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setInt(1, user.getCompanyId());
            statement.setInt(2, getEmployeeRoleId(connection));
            statement.setString(3, user.getUsername());
            statement.setString(4, user.getEmail());
            statement.setString(5, user.getDisplayName() != null && !user.getDisplayName().isBlank() ? user.getDisplayName() : user.getUsername());
            statement.setString(6, user.getPasswordHash());
            if (statement.executeUpdate() == 0) throw new SQLException("Creating employee account failed.");
            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) return generatedKeys.getInt(1);
            }
            throw new SQLException("Creating employee account failed, no generated UserId returned.");
        }
    }

    public boolean usernameExists(Connection connection, String username) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("SELECT COUNT(*) FROM Users WHERE Username = ?")) {
            statement.setString(1, username);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() && resultSet.getInt(1) > 0;
            }
        }
    }

    public boolean emailExists(Connection connection, String email) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("SELECT COUNT(*) FROM Users WHERE EmailNormalized = LOWER(LTRIM(RTRIM(?)))")) {
            statement.setString(1, email);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() && resultSet.getInt(1) > 0;
            }
        }
    }

    public boolean updateTenantPassword(int userId, String passwordHash) throws SQLException {
        String sql = """
                UPDATE Users
                SET PasswordHash = ?,
                    MustChangePassword = 0,
                    ActivatedAt = COALESCE(ActivatedAt, SYSUTCDATETIME()),
                    PasswordChangedAt = SYSUTCDATETIME(),
                    UpdatedAt = SYSUTCDATETIME()
                WHERE UserId = ?
                  AND CompanyId IS NOT NULL
                  AND IsActive = 1
                  AND IsLocked = 0
                """;
        try (Connection connection = DBConnection.getConnection()) {
            applyAuthWriteOptions(connection);
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, passwordHash);
                statement.setInt(2, userId);
                return statement.executeUpdate() == 1;
            }
        }
    }

    public boolean updateTenantUserRoleByEmployeeId(int employeeId, String roleCode) throws SQLException {
        String sql = """
                UPDATE u
                SET RoleId = ?,
                    UpdatedAt = SYSUTCDATETIME()
                FROM Users u
                INNER JOIN Employees e ON e.UserId = u.UserId
                WHERE e.EmployeeId = ?
                  AND u.CompanyId IS NOT NULL
                """;
        try (Connection connection = DBConnection.getConnection()) {
            int roleId = getRoleIdForCode(connection, roleCode);
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setInt(1, roleId);
                statement.setInt(2, employeeId);
                return statement.executeUpdate() > 0;
            }
        }
    }

    public User findSystemAdminByIdentifier(String identifier) throws SQLException {
        if (identifier == null || identifier.isBlank()) return null;
        String normalizedIdentifier = identifier.trim();
        String sql = """
                SELECT TOP (2) u.UserId, u.CompanyId, u.RoleId, r.RoleCode,
                       u.Username, u.Email, u.DisplayName, u.PasswordHash,
                       u.IsActive, u.IsLocked, u.FailedLoginAttempts,
                       u.LockedUntil, u.LastLoginAt, u.CreatedAt, u.UpdatedAt, u.RowVersion
                FROM Users u INNER JOIN Roles r ON r.RoleId = u.RoleId
                WHERE u.CompanyId IS NULL AND r.RoleCode = 'SYSTEM_ADMIN'
                  AND (u.Username = ? OR u.EmailNormalized = LOWER(LTRIM(RTRIM(?))))
                """;
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, normalizedIdentifier);
            statement.setString(2, normalizedIdentifier);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) return null;
                User user = mapUser(resultSet);
                return resultSet.next() ? null : user;
            }
        }
    }

    public TenantAccount findTenantAccount(String workspaceCode, String email) throws SQLException {
        if (workspaceCode == null || workspaceCode.isBlank() || email == null || email.isBlank()) return null;
        String sql = """
                SELECT TOP (2) u.UserId, u.CompanyId, u.RoleId, r.RoleCode,
                       u.Username, u.Email, u.DisplayName, u.PasswordHash,
                       u.MustChangePassword, u.ActivatedAt, u.PasswordChangedAt,
                       u.IsActive, u.IsLocked, u.FailedLoginAttempts,
                       u.LockedUntil, u.LastLoginAt, u.CreatedAt, u.UpdatedAt, u.RowVersion,
                       c.WorkspaceCode, c.CompanyName, e.EmployeeId
                FROM Users u
                INNER JOIN Roles r ON r.RoleId = u.RoleId
                INNER JOIN Companies c ON c.CompanyId = u.CompanyId
                INNER JOIN Subscriptions s ON s.CompanyId = c.CompanyId
                LEFT JOIN Employees e ON e.UserId = u.UserId
                WHERE c.WorkspaceCode = ?
                  AND u.EmailNormalized = LOWER(LTRIM(RTRIM(?)))
                  AND c.IsActive = 1 AND s.Status = N'ACTIVE'
                  AND u.IsActive = 1 AND u.IsLocked = 0
                  AND (u.LockedUntil IS NULL OR u.LockedUntil <= SYSUTCDATETIME())
                """;
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, workspaceCode.trim());
            statement.setString(2, email.trim());
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) return null;
                User user = mapUser(resultSet);
                TenantAccount account = new TenantAccount(user, resultSet.getInt("CompanyId"),
                        resultSet.getString("WorkspaceCode"), resultSet.getString("CompanyName"),
                        resultSet.getString("RoleCode"), resultSet.getObject("EmployeeId", Integer.class),
                        resultSet.getBoolean("MustChangePassword"));
                return resultSet.next() ? null : account;
            }
        }
    }

    public boolean isTenantUserAuthorized(Integer userId) throws SQLException {
        if (userId == null) return false;
        String sql = """
                SELECT 1 FROM Users u
                INNER JOIN Roles r ON r.RoleId = u.RoleId
                INNER JOIN Companies c ON c.CompanyId = u.CompanyId
                INNER JOIN Subscriptions s ON s.CompanyId = c.CompanyId
                WHERE u.UserId = ? AND u.CompanyId IS NOT NULL
                  AND r.RoleCode IN ('COMPANY_ADMIN', 'MANAGER', 'EMPLOYEE')
                  AND c.IsActive = 1 AND s.Status = N'ACTIVE'
                  AND u.IsActive = 1 AND u.IsLocked = 0
                  AND (u.LockedUntil IS NULL OR u.LockedUntil <= SYSUTCDATETIME())
                """;
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, userId);
            try (ResultSet result = statement.executeQuery()) { return result.next(); }
        }
    }

    public boolean isSystemAdminAuthorized(Integer userId) throws SQLException {
        if (userId == null) return false;
        String sql = """
                SELECT 1 FROM Users u INNER JOIN Roles r ON r.RoleId = u.RoleId
                WHERE u.UserId = ? AND u.CompanyId IS NULL AND r.RoleCode = 'SYSTEM_ADMIN'
                  AND u.IsActive = 1 AND u.IsLocked = 0
                  AND (u.LockedUntil IS NULL OR u.LockedUntil <= SYSUTCDATETIME())
                """;
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, userId);
            try (ResultSet result = statement.executeQuery()) { return result.next(); }
        }
    }

    public boolean recordTenantLoginSuccess(User user) throws SQLException {
        String sql = """
                DECLARE @Now DATETIME2 = SYSUTCDATETIME();
                UPDATE u SET FailedLoginAttempts = 0, IsLocked = 0, LockedUntil = NULL,
                    LastLoginAt = @Now, UpdatedAt = @Now
                FROM Users u INNER JOIN Companies c ON c.CompanyId = u.CompanyId
                INNER JOIN Subscriptions s ON s.CompanyId = c.CompanyId
                WHERE u.UserId = ? AND u.RowVersion = ? AND c.IsActive = 1 AND s.Status = N'ACTIVE'
                  AND u.IsActive = 1 AND u.IsLocked = 0
                  AND (u.LockedUntil IS NULL OR u.LockedUntil <= @Now)
                """;
        try (Connection connection = DBConnection.getConnection()) {
            applyAuthWriteOptions(connection);
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setInt(1, user.getUserId());
                statement.setBytes(2, user.getRowVersion());
                return statement.executeUpdate() == 1;
            }
        }
    }

    public void recordTenantLoginFailure(User user, int maxAttempts, int lockMinutes) throws SQLException {
        recordLoginFailure(user, maxAttempts, lockMinutes, false);
    }

    public void recordSystemAdminLoginFailure(User user, int maxAttempts, int lockMinutes) throws SQLException {
        recordLoginFailure(user, maxAttempts, lockMinutes, true);
    }

    private void recordLoginFailure(User user, int maxAttempts, int lockMinutes, boolean systemAdmin)
            throws SQLException {
        String sql = """
                DECLARE @Now DATETIME2 = SYSUTCDATETIME();
                UPDATE u SET FailedLoginAttempts = CASE WHEN u.LockedUntil IS NOT NULL AND u.LockedUntil <= @Now
                        THEN 1 ELSE u.FailedLoginAttempts + 1 END,
                    IsLocked = CASE WHEN u.LockedUntil IS NOT NULL AND u.LockedUntil <= @Now THEN 0
                        WHEN u.FailedLoginAttempts + 1 >= ? THEN 1 ELSE 0 END,
                    LockedUntil = CASE WHEN u.LockedUntil IS NOT NULL AND u.LockedUntil <= @Now THEN NULL
                        WHEN u.FailedLoginAttempts + 1 >= ? THEN DATEADD(MINUTE, ?, @Now) ELSE NULL END,
                    UpdatedAt = @Now
                FROM Users u INNER JOIN Roles r ON r.RoleId = u.RoleId
                WHERE u.UserId = ? AND u.PasswordHash COLLATE Latin1_General_100_BIN2 = ?
                  AND u.IsActive = 1 AND u.IsLocked = 0
                """;
        try (Connection connection = DBConnection.getConnection()) {
            applyAuthWriteOptions(connection);
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setInt(1, maxAttempts); statement.setInt(2, maxAttempts);
                statement.setInt(3, lockMinutes); statement.setInt(4, user.getUserId());
                statement.setString(5, user.getPasswordHash());
                statement.executeUpdate();
            }
        }
    }

    public boolean recordSystemAdminLoginSuccess(User user) throws SQLException {
        String sql = """
                DECLARE @Now DATETIME2 = SYSUTCDATETIME();
                UPDATE u SET FailedLoginAttempts = 0, IsLocked = 0, LockedUntil = NULL,
                    LastLoginAt = @Now, UpdatedAt = @Now
                FROM Users u INNER JOIN Roles r ON r.RoleId = u.RoleId
                WHERE u.UserId = ? AND u.RowVersion = ? AND u.CompanyId IS NULL
                  AND r.RoleCode = 'SYSTEM_ADMIN' AND u.IsActive = 1 AND u.IsLocked = 0
                  AND (u.LockedUntil IS NULL OR u.LockedUntil <= @Now)
                """;
        try (Connection connection = DBConnection.getConnection()) {
            applyAuthWriteOptions(connection);
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setInt(1, user.getUserId());
                statement.setBytes(2, user.getRowVersion());
                return statement.executeUpdate() == 1;
            }
        }
    }

    private void applyAuthWriteOptions(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) { statement.execute(AUTH_WRITE_OPTIONS); }
    }

    private User mapUser(ResultSet resultSet) throws SQLException {
        User user = new User();
        user.setUserId(resultSet.getInt("UserId"));
        user.setCompanyId(resultSet.getObject("CompanyId", Integer.class));
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
        user.setRowVersion(resultSet.getBytes("RowVersion"));
        return user;
    }

    private LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }

    private int getRoleIdForCode(Connection connection, String roleCode) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("SELECT RoleId FROM Roles WHERE RoleCode = ?")) {
            statement.setString(1, roleCode);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) return resultSet.getInt("RoleId");
            }
        }
        String roleName = "MANAGER".equalsIgnoreCase(roleCode) ? "Manager" : "Employee";
        String insertSql = "INSERT INTO Roles (RoleCode, RoleName, Description) VALUES (?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, roleCode);
            statement.setString(2, roleName);
            statement.setString(3, roleName + " tenant role");
            statement.executeUpdate();
            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) return generatedKeys.getInt(1);
            }
        }
        throw new SQLException("Role " + roleCode + " was not found and could not be created.");
    }

    private int getEmployeeRoleId(Connection connection) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("SELECT RoleId FROM Roles WHERE RoleCode = ?")) {
            statement.setString(1, EMPLOYEE_ROLE_CODE);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) return resultSet.getInt("RoleId");
            }
        }
        String insertSql = "INSERT INTO Roles (RoleCode, RoleName, Description) VALUES ('EMPLOYEE', N'Nhân viên', N'Tài khoản nhân viên được tạo tự động')";
        try (PreparedStatement statement = connection.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
            statement.executeUpdate();
            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) return generatedKeys.getInt(1);
            }
        }
        throw new SQLException("Role EMPLOYEE was not found and could not be created.");
    }
}

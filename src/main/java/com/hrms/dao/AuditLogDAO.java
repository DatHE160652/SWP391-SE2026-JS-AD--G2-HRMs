package com.hrms.dao;

import com.hrms.model.AuditLog;
import com.hrms.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class AuditLogDAO {
    public void insert(AuditLog log) throws SQLException {
        ensureTableExists();
        try (Connection connection = DBConnection.getConnection()) {
            insert(connection, log);
        }
    }

    public void insert(Connection connection, AuditLog log) throws SQLException {
        String sql = """
                INSERT INTO AuditLogs (CompanyId, UserId, Action, TableName, RecordId, OldValues, NewValues, IpAddress)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            setNullableInt(statement, 1, log.getCompanyId());
            setNullableInt(statement, 2, log.getUserId());
            statement.setString(3, log.getAction());
            statement.setString(4, log.getTableName());
            statement.setString(5, log.getRecordId());
            statement.setString(6, log.getOldValues());
            statement.setString(7, log.getNewValues());
            statement.setString(8, log.getIpAddress());
            statement.executeUpdate();
        }
    }

    public List<HistoryEntry> getEmployeeHistory(int employeeId) throws SQLException {
        ensureTableExists();
        String sql = """
                SELECT TOP 100 l.CreatedAt, l.Action, l.OldValues, l.NewValues,
                       COALESCE(actor.FullName, u.Email, u.Username, 'System') AS ChangedBy
                FROM AuditLogs l
                LEFT JOIN Users u ON u.UserId = l.UserId
                LEFT JOIN Employees actor ON actor.UserId = u.UserId
                WHERE l.TableName = 'Employees' AND l.RecordId = ?
                ORDER BY l.CreatedAt DESC, l.LogId DESC
                """;

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, String.valueOf(employeeId));
            try (ResultSet resultSet = statement.executeQuery()) {
                List<HistoryEntry> history = new ArrayList<>();
                while (resultSet.next()) {
                    Timestamp createdAt = resultSet.getTimestamp("CreatedAt");
                    history.add(new HistoryEntry(
                            createdAt == null ? null : createdAt.toLocalDateTime().toString(),
                            resultSet.getString("Action"),
                            resultSet.getString("ChangedBy"),
                            resultSet.getString("OldValues"),
                            resultSet.getString("NewValues")
                    ));
                }
                return history;
            }
        }
    }

    private void ensureTableExists() throws SQLException {
        String sql = """
                IF OBJECT_ID('AuditLogs', 'U') IS NULL
                BEGIN
                    CREATE TABLE AuditLogs
                    (
                        LogId BIGINT IDENTITY(1,1) PRIMARY KEY,
                        CompanyId INT NULL,
                        UserId INT NULL,
                        Action NVARCHAR(100) NOT NULL,
                        TableName NVARCHAR(100) NOT NULL,
                        RecordId NVARCHAR(100) NOT NULL,
                        OldValues NVARCHAR(MAX) NULL,
                        NewValues NVARCHAR(MAX) NULL,
                        IpAddress NVARCHAR(64) NULL,
                        CreatedAt DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME()
                    )
                END
                """;
        try (Connection connection = DBConnection.getConnection();
             Statement statement = connection.createStatement()) {
            statement.executeUpdate(sql);
        }
    }

    private void setNullableInt(PreparedStatement statement, int index, Integer value) throws SQLException {
        if (value == null) {
            statement.setNull(index, java.sql.Types.INTEGER);
        } else {
            statement.setInt(index, value);
        }
    }

    public record HistoryEntry(String createdAt, String action, String changedBy, String oldValue, String newValue) {
    }
}

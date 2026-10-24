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
        EmployeeHistoryValues.Values values = "Employees".equals(log.getTableName())
                ? new EmployeeHistoryValues(connection, log.getCompanyId()).format(log.getOldValues(), log.getNewValues())
                : new EmployeeHistoryValues.Values(log.getOldValues(), log.getNewValues());
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
            statement.setString(6, values.oldValue());
            statement.setString(7, values.newValue());
            statement.setString(8, log.getIpAddress());
            statement.executeUpdate();
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
                SELECT TOP 100 subject.CompanyId, l.CreatedAt, l.Action, l.OldValues, l.NewValues,
                       COALESCE(actor.FullName, u.Email, u.Username, 'System') AS ChangedBy
                FROM AuditLogs l
                JOIN Employees subject ON subject.EmployeeId=? AND (l.CompanyId=subject.CompanyId OR l.CompanyId IS NULL)
                LEFT JOIN Users u ON u.UserId = l.UserId
                LEFT JOIN Employees actor ON actor.UserId = u.UserId
                WHERE l.TableName = 'Employees' AND l.RecordId = ?
                ORDER BY l.CreatedAt DESC, l.LogId DESC
                """;

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, employeeId);
            statement.setString(2, String.valueOf(employeeId));
            List<StoredHistory> stored = new ArrayList<>();
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    Timestamp createdAt = resultSet.getTimestamp("CreatedAt");
                    stored.add(new StoredHistory((Integer) resultSet.getObject("CompanyId"), new HistoryEntry(
                            createdAt == null ? null : createdAt.toLocalDateTime().toString(),
                            resultSet.getString("Action"),
                            resultSet.getString("ChangedBy"),
                            resultSet.getString("OldValues"),
                            resultSet.getString("NewValues")
                    )));
                }
            }
            List<HistoryEntry> history = new ArrayList<>();
            java.util.Map<Integer,EmployeeHistoryValues> formatters = new java.util.HashMap<>();
            java.util.Map<Integer,EmployeeHistoryDisplay> displays = new java.util.HashMap<>();
            for (StoredHistory row : stored) {
                HistoryEntry entry = row.entry();
                EmployeeHistoryValues formatter = formatters.computeIfAbsent(row.companyId(), company -> new EmployeeHistoryValues(connection, company));
                EmployeeHistoryValues.Values values = formatter.format(entry.oldValue(), entry.newValue());
                values = displays.computeIfAbsent(row.companyId(), company -> new EmployeeHistoryDisplay(connection, company, employeeId))
                        .format(values.oldValue(), values.newValue());
                history.add(new HistoryEntry(entry.createdAt(),entry.action(),entry.changedBy(),values.oldValue(),values.newValue()));
            }
            return history;
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

    private record StoredHistory(Integer companyId, HistoryEntry entry) {}
}

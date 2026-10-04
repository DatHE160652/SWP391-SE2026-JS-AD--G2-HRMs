package com.hrms.dao;

import com.hrms.model.RegistrationRequest;
import com.hrms.model.RegistrationRequestFilter;
import com.hrms.model.RegistrationRequestPage;
import com.hrms.model.RegistrationRequestStats;
import com.hrms.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Data access for public Registration Requests and System Admin request screens. */
public class RegistrationRequestDAO {
    private static final String SELECT_COLUMNS = "RegistrationRequestId, FullName, Email, Phone, "
            + "CompanyName, Region, Status, CreatedAt, UpdatedAt";

    public List<RegistrationRequest> findVisibleRequests() throws SQLException {
        String sql = "SELECT " + SELECT_COLUMNS
                + " FROM RegistrationRequests WHERE Status <> 'DELETED'"
                + " ORDER BY CreatedAt DESC, RegistrationRequestId DESC";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            List<RegistrationRequest> requests = new ArrayList<>();
            while (resultSet.next()) requests.add(map(resultSet));
            return requests;
        }
    }

    public List<RegistrationRequest> findRecentPending() throws SQLException {
        String sql = "SELECT TOP (5) " + SELECT_COLUMNS
                + " FROM RegistrationRequests WHERE Status IN ('NEW', 'UNDER_REVIEW')"
                + " ORDER BY CreatedAt DESC, RegistrationRequestId DESC";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            List<RegistrationRequest> requests = new ArrayList<>();
            while (resultSet.next()) requests.add(map(resultSet));
            return requests;
        }
    }

    public RegistrationRequest findVisibleById(int requestId) throws SQLException {
        String sql = "SELECT " + SELECT_COLUMNS
                + " FROM RegistrationRequests"
                + " WHERE RegistrationRequestId = ? AND Status <> 'DELETED'";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, requestId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? map(resultSet) : null;
            }
        }
    }

    /**
     * Inserts a request and its receipt-attempt metadata atomically. A null result means a duplicate.
     * The mail provider is deliberately not called while this transaction is open.
     */
    public InsertResult insertNew(RegistrationRequest request) throws SQLException {
        try (Connection connection = DBConnection.getConnection()) {
            applyWriteOptions(connection);
            connection.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
            connection.setAutoCommit(false);
            try {
                if (duplicateExists(connection, request.getEmail(), request.getCompanyName())) {
                    connection.rollback();
                    return null;
                }

                String insertRequest = """
                        INSERT INTO RegistrationRequests
                            (FullName, Email, Phone, CompanyName, Region, CompanyNameNormalized, Status)
                        OUTPUT INSERTED.RegistrationRequestId, INSERTED.CreatedAt
                        VALUES (?, ?, ?, ?, ?, ?, 'NEW')
                        """;
                int requestId;
                LocalDateTime createdAt;
                try (PreparedStatement statement = connection.prepareStatement(insertRequest)) {
                    statement.setNString(1, request.getFullName());
                    statement.setString(2, request.getEmail());
                    statement.setString(3, request.getPhone());
                    statement.setNString(4, request.getCompanyName());
                    statement.setString(5, request.getRegion());
                    statement.setNString(6, request.getCompanyName().toLowerCase(java.util.Locale.ROOT));
                    try (ResultSet result = statement.executeQuery()) {
                        if (!result.next()) throw new SQLException("Registration request was not created.");
                        requestId = result.getInt("RegistrationRequestId");
                        Timestamp timestamp = result.getTimestamp("CreatedAt");
                        createdAt = timestamp == null ? null : timestamp.toLocalDateTime();
                    }
                }

                long attemptId;
                String insertAttempt = """
                        INSERT INTO EmailAttempts
                            (EmailType, RegistrationRequestId, RecipientEmail, Status)
                        VALUES ('REQUEST_RECEIPT', ?, ?, 'PENDING')
                        """;
                try (PreparedStatement statement = connection.prepareStatement(
                        insertAttempt, Statement.RETURN_GENERATED_KEYS)) {
                    statement.setInt(1, requestId);
                    statement.setString(2, request.getEmail());
                    statement.executeUpdate();
                    try (ResultSet keys = statement.getGeneratedKeys()) {
                        if (!keys.next()) throw new SQLException("Email attempt was not created.");
                        attemptId = keys.getLong(1);
                    }
                }

                request.setRegistrationRequestId(requestId);
                request.setStatus("NEW");
                request.setCreatedAt(createdAt);
                connection.commit();
                return new InsertResult(request, attemptId);
            } catch (SQLException exception) {
                try { connection.rollback(); } catch (SQLException ignored) { }
                if (isDuplicateKey(exception)) return null;
                throw exception;
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    public void markEmailAttempt(long attemptId, String status, String errorCode) throws SQLException {
        if (!List.of("ACCEPTED", "FAILED", "UNKNOWN").contains(status)) {
            throw new IllegalArgumentException("Invalid email attempt status.");
        }
        String sql = """
                UPDATE EmailAttempts
                SET Status = ?, AttemptedAt = SYSUTCDATETIME(), CompletedAt = SYSUTCDATETIME(), ErrorCode = ?
                WHERE EmailAttemptId = ? AND Status = 'PENDING'
                """;
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, status);
            statement.setString(2, cleanErrorCode(errorCode));
            statement.setLong(3, attemptId);
            statement.executeUpdate();
        }
    }

    public RegistrationRequestStats getStats(boolean includePlatformCounts) throws SQLException {
        String sql = "SELECT COUNT_BIG(CASE WHEN Status = 'NEW' THEN 1 END) AS NewCount, "
                + "COUNT_BIG(CASE WHEN Status = 'UNDER_REVIEW' THEN 1 END) AS ReviewCount, "
                + "COUNT_BIG(CASE WHEN Status = 'CONVERTING' THEN 1 END) AS ConvertingCount, "
                + "COUNT_BIG(CASE WHEN Status = 'CONVERTED' THEN 1 END) AS ConvertedCount, "
                + (includePlatformCounts
                    ? "(SELECT COUNT_BIG(*) FROM Companies) AS CompanyCount, "
                      + "(SELECT COUNT_BIG(*) FROM Users WHERE CompanyId IS NOT NULL) AS TenantUserCount "
                    : "CAST(0 AS BIGINT) AS CompanyCount, CAST(0 AS BIGINT) AS TenantUserCount ")
                + "FROM RegistrationRequests WHERE Status <> 'DELETED'";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {
            result.next();
            return new RegistrationRequestStats(result.getLong("NewCount"), result.getLong("ReviewCount"),
                    result.getLong("ConvertingCount"), result.getLong("ConvertedCount"),
                    result.getLong("CompanyCount"), result.getLong("TenantUserCount"));
        }
    }

    public RegistrationRequestPage findPage(RegistrationRequestFilter filter) throws SQLException {
        String predicate = filterPredicate(filter);
        try (Connection connection = DBConnection.getConnection()) {
            long total;
            try (PreparedStatement count = connection.prepareStatement(
                    "SELECT COUNT_BIG(*) FROM RegistrationRequests" + predicate)) {
                bindFilter(count, filter);
                try (ResultSet result = count.executeQuery()) {
                    result.next();
                    total = result.getLong(1);
                }
            }
            int page = (int) Math.min(filter.getPage(), Math.max(1, (total + filter.getSize() - 1) / filter.getSize()));
            List<RegistrationRequest> items = new ArrayList<>();
            try (PreparedStatement statement = connection.prepareStatement("SELECT " + SELECT_COLUMNS
                    + " FROM RegistrationRequests" + predicate
                    + " ORDER BY CreatedAt DESC, RegistrationRequestId DESC OFFSET ? ROWS FETCH NEXT ? ROWS ONLY")) {
                int index = bindFilter(statement, filter);
                statement.setLong(index++, (long) (page - 1) * filter.getSize());
                statement.setInt(index, filter.getSize());
                try (ResultSet result = statement.executeQuery()) {
                    while (result.next()) items.add(map(result));
                }
            }
            return new RegistrationRequestPage(items, total, page, filter.getSize());
        }
    }

    static String filterPredicate(RegistrationRequestFilter filter) {
        StringBuilder sql = new StringBuilder(" WHERE Status <> 'DELETED'");
        if (!filter.getQ().isEmpty()) {
            sql.append(" AND (CompanyName LIKE ? ESCAPE '\\' OR FullName LIKE ? ESCAPE '\\'"
                    + " OR Email LIKE ? ESCAPE '\\' OR CONCAT('REQ-', RegistrationRequestId) LIKE ? ESCAPE '\\')");
        }
        if ("PENDING".equals(filter.getStatus())) sql.append(" AND Status IN ('NEW', 'UNDER_REVIEW')");
        else if (!filter.getStatus().isEmpty()) sql.append(" AND Status = ?");
        if (!filter.getRegion().isEmpty()) sql.append(" AND Region = ?");
        if (!filter.getDate().isEmpty()) sql.append(" AND CreatedAt >= ? AND CreatedAt < ?");
        return sql.toString();
    }

    static String escapeLike(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_").replace("[", "\\[");
    }

    private boolean duplicateExists(Connection connection, String email, String companyName) throws SQLException {
        String sql = """
                SELECT TOP (1) 1 FROM RegistrationRequests WITH (UPDLOCK, HOLDLOCK)
                WHERE Status IN ('NEW', 'UNDER_REVIEW', 'CONVERTING')
                  AND (EmailNormalized = ? OR CompanyNameNormalized = ?)
                UNION ALL
                SELECT TOP (1) 1 FROM Users WITH (UPDLOCK, HOLDLOCK)
                WHERE EmailNormalized = ?
                UNION ALL
                SELECT TOP (1) 1 FROM Companies WITH (UPDLOCK, HOLDLOCK)
                WHERE CompanyNameNormalized = ?
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, email);
            statement.setNString(2, companyName);
            statement.setString(3, email);
            statement.setNString(4, companyName);
            try (ResultSet result = statement.executeQuery()) {
                return result.next();
            }
        }
    }

    private int bindFilter(PreparedStatement statement, RegistrationRequestFilter filter) throws SQLException {
        int index = 1;
        if (!filter.getQ().isEmpty()) {
            String pattern = "%" + escapeLike(filter.getQ()) + "%";
            for (int field = 0; field < 4; field++) statement.setNString(index++, pattern);
        }
        if (!filter.getStatus().isEmpty() && !"PENDING".equals(filter.getStatus())) statement.setString(index++, filter.getStatus());
        if (!filter.getRegion().isEmpty()) statement.setString(index++, filter.getRegion());
        if (!filter.getDate().isEmpty()) {
            java.time.LocalDate date = java.time.LocalDate.parse(filter.getDate());
            statement.setObject(index++, date.atStartOfDay());
            statement.setObject(index++, date.plusDays(1).atStartOfDay());
        }
        return index;
    }

    private RegistrationRequest map(ResultSet resultSet) throws SQLException {
        RegistrationRequest request = new RegistrationRequest();
        request.setRegistrationRequestId(resultSet.getInt("RegistrationRequestId"));
        request.setFullName(resultSet.getString("FullName"));
        request.setEmail(resultSet.getString("Email"));
        request.setPhone(resultSet.getString("Phone"));
        request.setCompanyName(resultSet.getString("CompanyName"));
        request.setRegion(resultSet.getString("Region"));
        request.setStatus(resultSet.getString("Status"));
        request.setCreatedAt(toLocalDateTime(resultSet.getTimestamp("CreatedAt")));
        request.setUpdatedAt(toLocalDateTime(resultSet.getTimestamp("UpdatedAt")));
        return request;
    }

    private LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }

    private void applyWriteOptions(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("SET ANSI_NULLS ON; SET QUOTED_IDENTIFIER ON; SET ANSI_PADDING ON; "
                    + "SET ANSI_WARNINGS ON; SET ARITHABORT ON; SET CONCAT_NULL_YIELDS_NULL ON; "
                    + "SET NUMERIC_ROUNDABORT OFF;");
        }
    }

    private boolean isDuplicateKey(SQLException exception) {
        return exception.getErrorCode() == 2601 || exception.getErrorCode() == 2627;
    }

    private String cleanErrorCode(String errorCode) {
        if (errorCode == null || errorCode.isBlank()) return null;
        String cleaned = errorCode.replaceAll("[^A-Za-z0-9_.-]", "_");
        return cleaned.length() > 100 ? cleaned.substring(0, 100) : cleaned;
    }

    public record InsertResult(RegistrationRequest request, long emailAttemptId) { }
}

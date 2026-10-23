package com.hrms.dao;

import com.hrms.model.AuditLog;
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
    private final ConnectionProvider connections;

    public RegistrationRequestDAO() {
        this(DBConnection::getConnection);
    }

    RegistrationRequestDAO(ConnectionProvider connections) {
        this.connections = connections;
    }

    @FunctionalInterface
    interface ConnectionProvider {
        Connection open() throws SQLException;
    }


    public List<RegistrationRequest> findVisibleRequests() throws SQLException {
        String sql = "SELECT " + SELECT_COLUMNS
                + " FROM RegistrationRequests WHERE Status <> 'DELETED'"
                + " ORDER BY CreatedAt DESC, RegistrationRequestId DESC";
        try (Connection connection = connections.open();
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
        try (Connection connection = connections.open();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            List<RegistrationRequest> requests = new ArrayList<>();
            while (resultSet.next()) requests.add(map(resultSet));
            return requests;
        }
    }

    public RegistrationRequest findVisibleById(int requestId) throws SQLException {
        String sql = """
                SELECT rr.RegistrationRequestId, rr.FullName, rr.Email, rr.Phone,
                       rr.CompanyName, rr.Region, rr.Status, rr.CreatedAt, rr.UpdatedAt,
                       rr.ConvertedAt, c.WorkspaceCode,
                       converter.DisplayName AS ConvertedByName,
                       ca.Email AS CompanyAdminEmail,
                       ca.ActivatedAt AS CompanyAdminActivatedAt,
                       latestEmail.Status AS ActivationEmailStatus,
                       latestEmail.CompletedAt AS ActivationEmailCompletedAt
                FROM RegistrationRequests rr
                LEFT JOIN Companies c ON c.CompanyId = rr.ConvertedCompanyId
                LEFT JOIN Users converter ON converter.UserId = rr.ConvertedByUserId
                LEFT JOIN Users ca ON ca.CompanyId = rr.ConvertedCompanyId
                  AND ca.RoleId = (SELECT RoleId FROM Roles WHERE RoleCode = 'COMPANY_ADMIN')
                OUTER APPLY (
                    SELECT TOP (1) ea.Status, ea.CompletedAt
                    FROM EmailAttempts ea
                    WHERE ea.RegistrationRequestId = rr.RegistrationRequestId
                      AND ea.EmailType IN ('ACCOUNT_ACTIVATION', 'ACTIVATION_RESEND')
                    ORDER BY ea.CreatedAt DESC, ea.EmailAttemptId DESC
                ) latestEmail
                WHERE rr.RegistrationRequestId = ? AND rr.Status <> 'DELETED'
                """;
        try (Connection connection = connections.open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, requestId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? mapWithConversion(resultSet) : null;
            }
        }
    }

    /**
     * Inserts a request and its receipt-attempt metadata atomically. A null result means a duplicate.
     * The mail provider is deliberately not called while this transaction is open.
     */
    public InsertResult insertNew(RegistrationRequest request) throws SQLException {
        try (Connection connection = connections.open()) {
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
        try (Connection connection = connections.open();
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
        try (Connection connection = connections.open();
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
        try (Connection connection = connections.open()) {
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
                WHERE Status IN ('NEW', 'UNDER_REVIEW', 'CONVERTING', 'DELETED')
                  AND (Email = LOWER(LTRIM(RTRIM(?))) OR CompanyNameNormalized = ?)
                UNION ALL
                SELECT TOP (1) 1 FROM Users WITH (UPDLOCK, HOLDLOCK)
                WHERE Email = LOWER(LTRIM(RTRIM(?)))
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
            statement.setObject(index++, com.hrms.util.TimeSupport.startOfDayUtc(date));
            statement.setObject(index++, com.hrms.util.TimeSupport.startOfDayUtc(date.plusDays(1)));
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

    /** Maps the base columns plus the conversion columns selected only by findVisibleById. */
    private RegistrationRequest mapWithConversion(ResultSet resultSet) throws SQLException {
        RegistrationRequest request = map(resultSet);
        request.setConvertedAt(toLocalDateTime(resultSet.getTimestamp("ConvertedAt")));
        request.setWorkspaceCode(resultSet.getString("WorkspaceCode"));
        request.setConvertedByName(resultSet.getString("ConvertedByName"));
        request.setCompanyAdminEmail(resultSet.getString("CompanyAdminEmail"));
        request.setCompanyAdminActivatedAt(toLocalDateTime(resultSet.getTimestamp("CompanyAdminActivatedAt")));
        request.setActivationEmailStatus(resultSet.getString("ActivationEmailStatus"));
        request.setActivationEmailCompletedAt(toLocalDateTime(resultSet.getTimestamp("ActivationEmailCompletedAt")));
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

    public MutationResult confirmAndProvision(int requestId, int actorId, String password, String ipAddress)
            throws SQLException {
        try (Connection connection = connections.open()) {
            applyWriteOptions(connection);
            connection.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
            connection.setAutoCommit(false);
            boolean completed = false;
            try {
                if (!isActiveSystemAdmin(connection, actorId)) {
                    connection.rollback();
                    completed = true;
                    return MutationResult.forbidden();
                }
                RegistrationRequest registration = findForUpdate(connection, requestId);
                if (registration == null) {
                    connection.rollback();
                    completed = true;
                    return MutationResult.notFound();
                }
                if (!List.of("NEW", "UNDER_REVIEW").contains(registration.getStatus())) {
                    connection.rollback();
                    completed = true;
                    return MutationResult.conflict();
                }
                String claim = "UPDATE RegistrationRequests SET Status='CONVERTING', UpdatedAt=SYSUTCDATETIME() "
                        + "WHERE RegistrationRequestId=? AND Status IN ('NEW','UNDER_REVIEW')";
                try (PreparedStatement statement = connection.prepareStatement(claim)) {
                    statement.setInt(1, requestId);
                    if (statement.executeUpdate() != 1) {
                        connection.rollback();
                        completed = true;
                        return MutationResult.conflict();
                    }
                }
                if (identityConflict(connection, registration)) {
                    connection.rollback();
                    completed = true;
                    return MutationResult.conflict();
                }

                CompanyInsert company = insertCompany(connection, registration, requestId);
                int companyId = company.companyId();
                int userId = insertCompanyAdmin(connection, registration, companyId, password);
                int planId = findDefaultPlan(connection);
                if (planId <= 0) throw new SQLException("Default active subscription plan is missing.");
                insertSubscription(connection, companyId, planId);
                String convert = "UPDATE RegistrationRequests SET Status='CONVERTED', ConvertedCompanyId=?, "
                        + "ConvertedAt=SYSUTCDATETIME(), ConvertedByUserId=?, UpdatedAt=SYSUTCDATETIME() "
                        + "WHERE RegistrationRequestId=? AND Status='CONVERTING'";
                try (PreparedStatement statement = connection.prepareStatement(convert)) {
                    statement.setInt(1, companyId);
                    statement.setInt(2, actorId);
                    statement.setInt(3, requestId);
                    if (statement.executeUpdate() != 1) throw new SQLException("Registration request conversion failed.");
                }
                insertAudit(connection, actorId, "CONFIRM_REQUEST", requestId, registration.getStatus(), "CONVERTED", ipAddress);
                long attemptId = insertActivationAttempt(connection, requestId, userId, actorId, registration.getEmail());
                MutationResult result = MutationResult.confirmed(registration, companyId, userId, attemptId, company.workspaceCode());
                connection.commit();
                completed = true;
                return result;
            } catch (SQLException | RuntimeException exception) {
                try { connection.rollback(); } catch (SQLException ignored) { }
                completed = true;
                if (exception instanceof SQLException sqlException && isDuplicateKey(sqlException)) {
                    return MutationResult.conflict();
                }
                throw exception;
            } finally {
                if (!completed) {
                    try { connection.rollback(); } catch (SQLException ignored) { }
                }
            }
        }
    }

    public MutationResult softDelete(int requestId, int actorId, String ipAddress) throws SQLException {
        try (Connection connection = connections.open()) {
            applyWriteOptions(connection);
            connection.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
            connection.setAutoCommit(false);
            boolean completed = false;
            try {
                if (!isActiveSystemAdmin(connection, actorId)) {
                    connection.rollback(); completed = true; return MutationResult.forbidden();
                }
                RegistrationRequest registration = findForUpdate(connection, requestId);
                if (registration == null) {
                    connection.rollback(); completed = true; return MutationResult.notFound();
                }
                if (!List.of("NEW", "UNDER_REVIEW").contains(registration.getStatus())) {
                    connection.rollback(); completed = true; return MutationResult.conflict();
                }
                String sql = "UPDATE RegistrationRequests SET Status='DELETED', DeletedAt=SYSUTCDATETIME(), "
                        + "DeletedByUserId=?, UpdatedAt=SYSUTCDATETIME() "
                        + "WHERE RegistrationRequestId=? AND Status IN ('NEW','UNDER_REVIEW')";
                try (PreparedStatement statement = connection.prepareStatement(sql)) {
                    statement.setInt(1, actorId); statement.setInt(2, requestId);
                    if (statement.executeUpdate() != 1) {
                        connection.rollback(); completed = true; return MutationResult.conflict();
                    }
                }
                insertAudit(connection, actorId, "SOFT_DELETE_REQUEST", requestId,
                        registration.getStatus(), "DELETED", ipAddress);
                connection.commit(); completed = true;
                return MutationResult.deleted(registration);
            } catch (SQLException | RuntimeException exception) {
                try { connection.rollback(); } catch (SQLException ignored) { }
                completed = true;
                if (exception instanceof SQLException sqlException && isDuplicateKey(sqlException)) {
                    return MutationResult.conflict();
                }
                throw exception;
            } finally {
                if (!completed) {
                    try { connection.rollback(); } catch (SQLException ignored) { }
                }
            }
        }
    }

    private boolean isActiveSystemAdmin(Connection connection, int actorId) throws SQLException {
        String sql = "SELECT 1 FROM Users u INNER JOIN Roles r ON r.RoleId=u.RoleId "
                + "WHERE u.UserId=? AND u.CompanyId IS NULL AND r.RoleCode='SYSTEM_ADMIN' "
                + "AND u.IsActive=1 AND u.IsLocked=0 AND (u.LockedUntil IS NULL OR u.LockedUntil<=SYSUTCDATETIME())";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, actorId);
            try (ResultSet result = statement.executeQuery()) { return result.next(); }
        }
    }

    private RegistrationRequest findForUpdate(Connection connection, int requestId) throws SQLException {
        String sql = "SELECT " + SELECT_COLUMNS + " FROM RegistrationRequests WITH (UPDLOCK,HOLDLOCK) "
                + "WHERE RegistrationRequestId=?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, requestId);
            try (ResultSet result = statement.executeQuery()) { return result.next() ? map(result) : null; }
        }
    }

    private boolean identityConflict(Connection connection, RegistrationRequest request) throws SQLException {
        String sql = "SELECT TOP (1) 1 FROM Users WITH (UPDLOCK,HOLDLOCK) WHERE Email=LOWER(LTRIM(RTRIM(?))) "
                + "UNION ALL SELECT TOP (1) 1 FROM Companies WITH (UPDLOCK,HOLDLOCK) WHERE CompanyNameNormalized=?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, request.getEmail());
            statement.setNString(2, normalizedCompany(request.getCompanyName()));
            try (ResultSet result = statement.executeQuery()) { return result.next(); }
        }
    }

    private CompanyInsert insertCompany(Connection connection, RegistrationRequest request, int requestId) throws SQLException {
        String normalized = normalizedCompany(request.getCompanyName());
        for (int suffix = 0; suffix < 20; suffix++) {
            String workspace = createWorkspaceCode(request, suffix);
            if (workspaceExists(connection, workspace)) continue;
            String sql = "INSERT INTO Companies (WorkspaceCode,CompanyName,CompanyNameNormalized,Region,ContactEmail,ContactPhone,IsActive) "
                    + "OUTPUT INSERTED.CompanyId VALUES (?,?,?,?,?,?,1)";
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, workspace); statement.setNString(2, request.getCompanyName());
                statement.setNString(3, normalized); statement.setString(4, request.getRegion());
                statement.setString(5, request.getEmail()); statement.setString(6, request.getPhone());
                try (ResultSet result = statement.executeQuery()) {
                    if (result.next()) return new CompanyInsert(result.getInt(1), workspace);
                }
            }
        }
        throw new SQLException("Unable to allocate unique workspace code.");
    }

    private record CompanyInsert(int companyId, String workspaceCode) { }

    private boolean workspaceExists(Connection connection, String workspace) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT 1 FROM Companies WITH (UPDLOCK,HOLDLOCK) WHERE WorkspaceCode=?")) {
            statement.setString(1, workspace);
            try (ResultSet result = statement.executeQuery()) { return result.next(); }
        }
    }

    static String createWorkspaceCode(RegistrationRequest request, int suffix) {
        String name = java.text.Normalizer.normalize(request.getCompanyName().toLowerCase(java.util.Locale.ROOT),
                java.text.Normalizer.Form.NFD).replaceAll("\\p{M}+", "").replace('đ', 'd');
        String slug = name.replaceAll("[^a-z0-9]+", "-").replaceAll("^-|-$", "");
        if (slug.isEmpty()) slug = "tenant";
        String ending = suffix == 0 ? "" : "-" + (suffix + 1);
        int maxPrefix = 50 - ending.length();
        slug = slug.substring(0, Math.min(slug.length(), maxPrefix)).replaceAll("-$", "");
        if (slug.isEmpty()) slug = "t";
        String code = slug + ending;
        if (List.of("app", "admin", "www", "api", "system").contains(code)) return "tenant" + ending;
        return code;
    }

    private String normalizedCompany(String companyName) {
        return companyName.trim().replaceAll("\\s+", " ").toLowerCase(java.util.Locale.ROOT);
    }

    private int insertCompanyAdmin(Connection connection, RegistrationRequest request, int companyId,
                                   String password) throws SQLException {
        int roleId;
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT RoleId FROM Roles WHERE RoleCode='COMPANY_ADMIN'")) {
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) throw new SQLException("COMPANY_ADMIN role is missing.");
                roleId = result.getInt(1);
            }
        }
        String username = null;
        for (int suffix = 0; suffix < 20; suffix++) {
            String candidate = "company-admin-" + companyId + (suffix == 0 ? "" : "-" + suffix);
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT 1 FROM Users WITH (UPDLOCK,HOLDLOCK) WHERE Username=?")) {
                statement.setString(1, candidate);
                try (ResultSet result = statement.executeQuery()) {
                    if (!result.next()) { username = candidate; break; }
                }
            }
        }
        if (username == null) throw new SQLException("Unable to allocate unique Company Admin username.");
        String sql = "INSERT INTO Users (CompanyId,RoleId,Username,Email,DisplayName,Password,MustChangePassword,ActivatedAt,PasswordChangedAt,IsActive,IsLocked) "
                + "OUTPUT INSERTED.UserId VALUES (?,?,?,?,?,?,1,NULL,NULL,1,0)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, companyId); statement.setInt(2, roleId);
            statement.setString(3, username); statement.setString(4, request.getEmail());
            statement.setNString(5, request.getFullName()); statement.setString(6, password);
            try (ResultSet result = statement.executeQuery()) {
                if (result.next()) return result.getInt(1);
            }
        }
        throw new SQLException("Company Admin was not created.");
    }

    private int findDefaultPlan(Connection connection) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT PlanId FROM Plans WITH (UPDLOCK,HOLDLOCK) WHERE PlanName=N'DEFAULT' AND IsActive=1")) {
            try (ResultSet result = statement.executeQuery()) { return result.next() ? result.getInt(1) : -1; }
        }
    }

    private void insertSubscription(Connection connection, int companyId, int planId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO Subscriptions (CompanyId,PlanId,StartDate,Status) VALUES (?, ?, CAST(SYSUTCDATETIME() AT TIME ZONE 'UTC' AT TIME ZONE 'SE Asia Standard Time' AS DATE), N'ACTIVE')")) {
            statement.setInt(1, companyId); statement.setInt(2, planId); statement.executeUpdate();
        }
    }

    private void insertAudit(Connection connection, int actorId, String action, int requestId,
                             String oldStatus, String newStatus, String ipAddress) throws SQLException {
        AuditLog log = new AuditLog(); log.setUserId(actorId); log.setAction(action);
        log.setTableName("RegistrationRequests"); log.setRecordId(String.valueOf(requestId));
        log.setOldValues("{\"status\":\"" + oldStatus + "\"}");
        log.setNewValues("{\"status\":\"" + newStatus + "\"}");
        log.setIpAddress(ipAddress == null || ipAddress.length() > 50 ? null : ipAddress);
        new AuditLogDAO().insert(connection, log);
    }

    private long insertActivationAttempt(Connection connection, int requestId, int userId,
                                         int actorId, String recipient) throws SQLException {
        String sql = "INSERT INTO EmailAttempts (EmailType,RegistrationRequestId,UserId,RequestedByUserId,RecipientEmail,Status) "
                + "OUTPUT INSERTED.EmailAttemptId VALUES ('ACCOUNT_ACTIVATION',?,?,?,?, 'PENDING')";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, requestId); statement.setInt(2, userId);
            statement.setInt(3, actorId); statement.setString(4, recipient);
            try (ResultSet result = statement.executeQuery()) {
                if (result.next()) return result.getLong(1);
            }
        }
        throw new SQLException("Activation email attempt was not recorded.");
    }

    public record MutationResult(String outcome, RegistrationRequest request, int companyId,
                                 int userId, long emailAttemptId, String workspaceCode) {
        static MutationResult forbidden() { return new MutationResult("FORBIDDEN", null, 0, 0, 0, null); }
        static MutationResult notFound() { return new MutationResult("NOT_FOUND", null, 0, 0, 0, null); }
        static MutationResult conflict() { return new MutationResult("CONFLICT", null, 0, 0, 0, null); }
        static MutationResult deleted(RegistrationRequest request) { return new MutationResult("DELETED", request, 0, 0, 0, null); }
        static MutationResult confirmed(RegistrationRequest request, int companyId, int userId,
                                        long attemptId, String workspace) {
            return new MutationResult("CONFIRMED", request, companyId, userId, attemptId, workspace);
        }
    }

    public record InsertResult(RegistrationRequest request, long emailAttemptId) { }
}

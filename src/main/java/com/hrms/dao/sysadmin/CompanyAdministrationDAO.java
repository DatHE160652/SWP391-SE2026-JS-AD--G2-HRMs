package com.hrms.dao.sysadmin;
import com.hrms.dao.AuditLogDAO;

import com.hrms.model.AuditLog;
import com.hrms.model.CompanyAdministrationFilter;
import com.hrms.model.CompanyAdministrationView;
import com.hrms.model.CompanyMemberView;
import com.hrms.util.DBConnection;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Data access for System Admin company listings and subscription state
 * transitions.
 */
public class CompanyAdministrationDAO {
    private static final String SELECT_COLUMNS = """
            c.CompanyId, c.WorkspaceCode, c.CompanyName, c.Region, c.IsActive AS CompanyIsActive,
            c.ContactEmail, c.ContactPhone, c.CreatedAt AS CompanyCreatedAt,
            admin.DisplayName AS CompanyAdminName, admin.Email AS CompanyAdminEmail,
            admin.ActivatedAt AS CompanyAdminActivatedAt,
            (SELECT COUNT_BIG(*) FROM Users account WHERE account.CompanyId = c.CompanyId) AS AccountCount,
            s.SubscriptionId, s.Status AS SubscriptionStatus, s.StartDate, s.EndDate,
            s.SuspendedAt, s.SuspensionReason,
            p.PlanName, p.MaxAccounts, p.Price, p.BillingCycle
            FROM Companies c
            INNER JOIN Subscriptions s ON s.CompanyId = c.CompanyId
            INNER JOIN Plans p ON p.PlanId = s.PlanId
            OUTER APPLY (
                SELECT TOP (1) u.DisplayName, u.Email, u.ActivatedAt
                FROM Users u INNER JOIN Roles r ON r.RoleId = u.RoleId
                WHERE u.CompanyId = c.CompanyId AND r.RoleCode = 'COMPANY_ADMIN'
                ORDER BY u.UserId
            ) admin
            """;
    private final ConnectionProvider connections;

    public CompanyAdministrationDAO() {
        this(DBConnection::getConnection);
    }

    CompanyAdministrationDAO(ConnectionProvider connections) {
        this.connections = connections;
    }

    @FunctionalInterface
    interface ConnectionProvider {
        Connection open() throws SQLException;
    }

    public List<CompanyAdministrationView> findAll() throws SQLException {
        return findAll(new CompanyAdministrationFilter("", "", CompanyAdministrationFilter.NEWEST));
    }

    public List<CompanyAdministrationView> findAll(CompanyAdministrationFilter filter) throws SQLException {
        String sql = "SELECT " + SELECT_COLUMNS + filterPredicate(filter) + orderBy(filter);
        try (Connection connection = connections.open()) {
            expireOverdueSubscriptions(connection);
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                bindFilter(statement, filter);
                try (ResultSet results = statement.executeQuery()) {
                    List<CompanyAdministrationView> companies = new ArrayList<>();
                    while (results.next())
                        companies.add(map(results));
                    return companies;
                }
            }
        }
    }

    static String filterPredicate(CompanyAdministrationFilter filter) {
        StringBuilder sql = new StringBuilder(" WHERE 1=1");
        if (!filter.getQ().isEmpty()) {
            sql.append(" AND (c.CompanyName LIKE ? ESCAPE '\\' OR c.WorkspaceCode LIKE ? ESCAPE '\\'")
                    .append(" OR admin.Email LIKE ? ESCAPE '\\' OR admin.DisplayName LIKE ? ESCAPE '\\')");
        }
        if (!filter.isAllStatuses())
            sql.append(" AND s.Status = ?");
        if (!filter.getRegion().isEmpty())
            sql.append(" AND c.Region = ?");
        return sql.toString();
    }

    static String orderBy(CompanyAdministrationFilter filter) {
        return switch (filter.getSort()) {
            case CompanyAdministrationFilter.NEWEST -> " ORDER BY c.CreatedAt DESC, c.CompanyId DESC";
            case CompanyAdministrationFilter.ACCOUNT_DESC ->
                " ORDER BY AccountCount DESC, c.CreatedAt DESC, c.CompanyId DESC";
            case CompanyAdministrationFilter.NAME_ASC -> " ORDER BY c.CompanyNameNormalized ASC, c.CompanyId ASC";
            default -> throw new IllegalArgumentException("Invalid trusted Company sort.");
        };
    }

    private void bindFilter(PreparedStatement statement, CompanyAdministrationFilter filter) throws SQLException {
        int index = 1;
        if (!filter.getQ().isEmpty()) {
            String pattern = "%" + filter.getQ()
                    .replace("\\", "\\\\")
                    .replace("%", "\\%")
                    .replace("_", "\\_") + "%";
            for (int i = 0; i < 4; i++)
                statement.setNString(index++, pattern);
        }
        if (!filter.isAllStatuses())
            statement.setString(index++, filter.getStatus());
        if (!filter.getRegion().isEmpty())
            statement.setString(index, filter.getRegion());
    }

    public CompanyAdministrationView findById(int companyId) throws SQLException {
        String sql = "SELECT " + SELECT_COLUMNS + " WHERE c.CompanyId = ?";
        try (Connection connection = connections.open()) {
            expireOverdueSubscriptions(connection);
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setInt(1, companyId);
                try (ResultSet results = statement.executeQuery()) {
                    if (!results.next())
                        return null;
                    CompanyAdministrationView company = map(results);
                    company.setCoreMembers(findCoreMembers(connection, companyId));
                    return company;
                }
            }
        }
    }

    private void expireOverdueSubscriptions(Connection connection) throws SQLException {
        String sql = """
                UPDATE s
                SET s.Status = N'SUSPENDED',
                    s.SuspendedAt = COALESCE(s.SuspendedAt, SYSUTCDATETIME()),
                    s.SuspensionReason = COALESCE(NULLIF(LTRIM(RTRIM(s.SuspensionReason)), N''), N'Gói dịch vụ đã hết hạn'),
                    s.UpdatedAt = SYSUTCDATETIME()
                FROM Subscriptions s
                INNER JOIN Plans p ON p.PlanId = s.PlanId
                WHERE s.Status = N'ACTIVE'
                  AND COALESCE(s.EndDate,
                        CASE WHEN UPPER(LTRIM(RTRIM(p.BillingCycle))) = 'YEARLY'
                             THEN DATEADD(YEAR, 1, s.StartDate)
                             ELSE DATEADD(MONTH, 1, s.StartDate) END)
                      < CAST(SYSUTCDATETIME() AT TIME ZONE 'UTC' AT TIME ZONE 'SE Asia Standard Time' AS DATE)
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.executeUpdate();
        }
    }

    private List<CompanyMemberView> findCoreMembers(Connection connection, int companyId) throws SQLException {
        String sql = """
                SELECT TOP (20) u.DisplayName, u.Email, r.RoleCode, u.IsActive
                FROM Users u
                INNER JOIN Roles r ON r.RoleId = u.RoleId
                WHERE u.CompanyId = ? AND r.RoleCode IN ('COMPANY_ADMIN', 'MANAGER')
                ORDER BY CASE r.RoleCode WHEN 'COMPANY_ADMIN' THEN 0 WHEN 'MANAGER' THEN 1 ELSE 2 END,
                         u.DisplayName, u.UserId
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, companyId);
            try (ResultSet results = statement.executeQuery()) {
                List<CompanyMemberView> members = new ArrayList<>();
                while (results.next()) {
                    members.add(new CompanyMemberView(results.getString("DisplayName"),
                            results.getString("Email"), results.getString("RoleCode"),
                            results.getBoolean("IsActive")));
                }
                return members;
            }
        }
    }

    public MutationResult renew(int companyId, int actorId, int months, String ipAddress) throws SQLException {
        if (months <= 0 || months > 36) {
            throw new IllegalArgumentException("Thời gian gia hạn phải từ 1 đến 36 tháng.");
        }
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
                SubscriptionRecord subscription = findSubscriptionForUpdate(connection, companyId);
                if (subscription == null) {
                    connection.rollback();
                    completed = true;
                    return MutationResult.notFound();
                }
                LocalDate previousEndDate = null;
                try (PreparedStatement ps = connection.prepareStatement(
                        "SELECT EndDate FROM Subscriptions WITH (UPDLOCK, HOLDLOCK) WHERE CompanyId = ?")) {
                    ps.setInt(1, companyId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next())
                            previousEndDate = toLocalDate(rs.getDate("EndDate"));
                    }
                }
                String sql = """
                        UPDATE s
                        SET s.Status = N'ACTIVE',
                            s.SuspendedAt = NULL,
                            s.SuspendedByUserId = NULL,
                            s.SuspensionReason = NULL,
                            s.EndDate = DATEADD(MONTH, ?,
                                CASE WHEN s.EndDate IS NOT NULL AND s.EndDate >= CAST(SYSUTCDATETIME() AT TIME ZONE 'UTC' AT TIME ZONE 'SE Asia Standard Time' AS DATE)
                                     THEN s.EndDate
                                     ELSE CAST(SYSUTCDATETIME() AT TIME ZONE 'UTC' AT TIME ZONE 'SE Asia Standard Time' AS DATE) END),
                            s.LastActivatedAt = SYSUTCDATETIME(),
                            s.LastActivatedByUserId = ?,
                            s.UpdatedAt = SYSUTCDATETIME()
                        FROM Subscriptions s
                        WHERE s.CompanyId = ?
                        """;
                try (PreparedStatement ps = connection.prepareStatement(sql)) {
                    ps.setInt(1, months);
                    ps.setInt(2, actorId);
                    ps.setInt(3, companyId);
                    if (ps.executeUpdate() != 1) {
                        connection.rollback();
                        completed = true;
                        return MutationResult.conflict();
                    }
                }
                LocalDate newEndDate = null;
                try (PreparedStatement ps = connection.prepareStatement(
                        "SELECT EndDate FROM Subscriptions WHERE CompanyId = ?")) {
                    ps.setInt(1, companyId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next())
                            newEndDate = toLocalDate(rs.getDate("EndDate"));
                    }
                }
                AuditLog log = new AuditLog();
                log.setCompanyId(companyId);
                log.setUserId(actorId);
                log.setAction("RENEW_SUBSCRIPTION");
                log.setTableName("Subscriptions");
                log.setRecordId(String.valueOf(subscription.subscriptionId()));
                log.setOldValues("{\"status\":\"" + subscription.status() + "\""
                        + (previousEndDate != null ? ",\"endDate\":\"" + previousEndDate + "\"" : "") + "}");
                log.setNewValues("{\"status\":\"ACTIVE\""
                        + (newEndDate != null ? ",\"endDate\":\"" + newEndDate + "\"" : "")
                        + ",\"months\":" + months
                        + ",\"reason\":\"Gia hạn thêm " + months + " tháng\"}");
                log.setIpAddress(ipAddress == null || ipAddress.length() > 50 ? null : ipAddress);
                new AuditLogDAO().insert(connection, log);
                connection.commit();
                completed = true;
                return MutationResult.changed("ACTIVE");
            } catch (SQLException | RuntimeException ex) {
                try {
                    connection.rollback();
                } catch (SQLException ignored) {
                }
                completed = true;
                throw ex;
            } finally {
                if (!completed) {
                    try {
                        connection.rollback();
                    } catch (SQLException ignored) {
                    }
                }
            }
        }
    }

    public MutationResult changePlan(int companyId, int actorId, int newPlanId, String ipAddress) throws SQLException {
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
                SubscriptionRecord subscription = findSubscriptionForUpdate(connection, companyId);
                if (subscription == null) {
                    connection.rollback();
                    completed = true;
                    return MutationResult.notFound();
                }
                String planName = null;
                int newMaxAccounts = 0;
                try (PreparedStatement ps = connection.prepareStatement(
                        "SELECT PlanName, MaxAccounts FROM Plans WITH (UPDLOCK, HOLDLOCK) WHERE PlanId = ? AND IsActive = 1")) {
                    ps.setInt(1, newPlanId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            connection.rollback();
                            completed = true;
                            return MutationResult.conflict();
                        }
                        planName = rs.getNString("PlanName");
                        newMaxAccounts = rs.getInt("MaxAccounts");
                    }
                }
                long currentAccounts = 0;
                try (PreparedStatement ps = connection.prepareStatement(
                        "SELECT COUNT_BIG(*) FROM Users WITH (UPDLOCK, HOLDLOCK) WHERE CompanyId = ?")) {
                    ps.setInt(1, companyId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next())
                            currentAccounts = rs.getLong(1);
                    }
                }
                if (currentAccounts > newMaxAccounts) {
                    throw new IllegalArgumentException("Không thể chuyển sang gói " + planName
                            + " (tối đa " + newMaxAccounts + " tài khoản) vì công ty đang có " + currentAccounts
                            + " tài khoản.");
                }
                try (PreparedStatement ps = connection.prepareStatement(
                        "UPDATE Subscriptions SET PlanId = ?, UpdatedAt = SYSUTCDATETIME() WHERE CompanyId = ?")) {
                    ps.setInt(1, newPlanId);
                    ps.setInt(2, companyId);
                    if (ps.executeUpdate() != 1) {
                        connection.rollback();
                        completed = true;
                        return MutationResult.conflict();
                    }
                }
                insertAudit(connection, actorId, companyId, subscription.subscriptionId(),
                        "CHANGE_PLAN", subscription.status(), subscription.status(),
                        "Đổi sang gói " + planName + " (#" + newPlanId + ")", ipAddress);
                connection.commit();
                completed = true;
                return MutationResult.changed(subscription.status());
            } catch (SQLException | RuntimeException ex) {
                try {
                    connection.rollback();
                } catch (SQLException ignored) {
                }
                completed = true;
                throw ex;
            } finally {
                if (!completed) {
                    try {
                        connection.rollback();
                    } catch (SQLException ignored) {
                    }
                }
            }
        }
    }

    public MutationResult rollbackRenew(long logId, int actorId, String ipAddress) throws SQLException {
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
                Integer companyId = null;
                Integer createdByUserId = null;
                String oldValues = null;
                String newValues = null;
                String recordId = null;
                try (PreparedStatement ps = connection.prepareStatement(
                        "SELECT CompanyId, UserId, RecordId, OldValues, NewValues FROM AuditLogs WITH (UPDLOCK, HOLDLOCK) WHERE LogId = ? AND Action = N'RENEW_SUBSCRIPTION'")) {
                    ps.setLong(1, logId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            connection.rollback();
                            completed = true;
                            return MutationResult.notFound();
                        }
                        companyId = (Integer) rs.getObject("CompanyId");
                        createdByUserId = (Integer) rs.getObject("UserId");
                        recordId = rs.getString("RecordId");
                        oldValues = rs.getNString("OldValues");
                        newValues = rs.getNString("NewValues");
                    }
                }
                if (createdByUserId == null || createdByUserId.intValue() != actorId) {
                    connection.rollback();
                    completed = true;
                    return MutationResult.forbidden();
                }
                if (companyId == null || oldValues == null) {
                    connection.rollback();
                    completed = true;
                    return MutationResult.conflict();
                }
                try (PreparedStatement ps = connection.prepareStatement(
                        "SELECT 1 FROM AuditLogs WHERE Action = N'ROLLBACK_RENEW_SUBSCRIPTION' AND NewValues LIKE ?")) {
                    ps.setNString(1, "%\"revertedLogId\":" + logId + "%");
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            connection.rollback();
                            completed = true;
                            return MutationResult.conflict();
                        }
                    }
                }

                String oldStatus = extractJsonField(oldValues, "status");
                String oldEndDateRaw = extractJsonField(oldValues, "endDate");
                if (oldStatus == null || oldStatus.isBlank())
                    oldStatus = "ACTIVE";
                LocalDate oldEndDate = (oldEndDateRaw == null || oldEndDateRaw.isBlank()) ? null
                        : LocalDate.parse(oldEndDateRaw);

                SubscriptionRecord currentSub = findSubscriptionForUpdate(connection, companyId);
                if (currentSub == null) {
                    connection.rollback();
                    completed = true;
                    return MutationResult.notFound();
                }

                if ("SUSPENDED".equalsIgnoreCase(oldStatus)) {
                    String sql = """
                            UPDATE Subscriptions
                            SET Status = N'SUSPENDED',
                                EndDate = ?,
                                SuspendedAt = COALESCE(SuspendedAt, SYSUTCDATETIME()),
                                SuspendedByUserId = COALESCE(SuspendedByUserId, ?),
                                SuspensionReason = N'Khôi phục từ thao tác gia hạn nhầm',
                                UpdatedAt = SYSUTCDATETIME()
                            WHERE CompanyId = ?
                            """;
                    try (PreparedStatement ps = connection.prepareStatement(sql)) {
                        if (oldEndDate != null)
                            ps.setDate(1, Date.valueOf(oldEndDate));
                        else
                            ps.setNull(1, java.sql.Types.DATE);
                        ps.setInt(2, actorId);
                        ps.setInt(3, companyId);
                        if (ps.executeUpdate() != 1) {
                            connection.rollback();
                            completed = true;
                            return MutationResult.conflict();
                        }
                    }
                } else {
                    String sql = """
                            UPDATE Subscriptions
                            SET Status = N'ACTIVE',
                                EndDate = ?,
                                SuspendedAt = NULL,
                                SuspendedByUserId = NULL,
                                SuspensionReason = NULL,
                                UpdatedAt = SYSUTCDATETIME()
                            WHERE CompanyId = ?
                            """;
                    try (PreparedStatement ps = connection.prepareStatement(sql)) {
                        if (oldEndDate != null)
                            ps.setDate(1, Date.valueOf(oldEndDate));
                        else
                            ps.setNull(1, java.sql.Types.DATE);
                        ps.setInt(2, companyId);
                        if (ps.executeUpdate() != 1) {
                            connection.rollback();
                            completed = true;
                            return MutationResult.conflict();
                        }
                    }
                }

                AuditLog rollbackLog = new AuditLog();
                rollbackLog.setCompanyId(companyId);
                rollbackLog.setUserId(actorId);
                rollbackLog.setAction("ROLLBACK_RENEW_SUBSCRIPTION");
                rollbackLog.setTableName("Subscriptions");
                rollbackLog.setRecordId(recordId);
                rollbackLog.setOldValues(newValues);
                rollbackLog.setNewValues("{\"status\":\"" + oldStatus + "\""
                        + (oldEndDate != null ? ",\"endDate\":\"" + oldEndDate + "\"" : "")
                        + ",\"revertedLogId\":" + logId
                        + ",\"reason\":\"Khôi phục lại (#" + logId + ")\"}");
                rollbackLog.setIpAddress(ipAddress == null || ipAddress.length() > 50 ? null : ipAddress);
                new AuditLogDAO().insert(connection, rollbackLog);
                connection.commit();
                completed = true;
                return MutationResult.changed(oldStatus);
            } catch (SQLException | RuntimeException ex) {
                try {
                    connection.rollback();
                } catch (SQLException ignored) {
                }
                completed = true;
                throw ex;
            } finally {
                if (!completed) {
                    try {
                        connection.rollback();
                    } catch (SQLException ignored) {
                    }
                }
            }
        }
    }

    private static String extractJsonField(String json, String fieldName) {
        if (json == null)
            return null;
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("\"" + java.util.regex.Pattern.quote(fieldName) + "\"\\s*:\\s*\"([^\"]*)\"")
                .matcher(json);
        return m.find() ? m.group(1) : null;
    }

    private boolean isActiveSystemAdmin(Connection connection, int actorId) throws SQLException {
        String sql = "SELECT 1 FROM Users u INNER JOIN Roles r ON r.RoleId = u.RoleId "
                + "WHERE u.UserId=? AND u.CompanyId IS NULL AND r.RoleCode='SYSTEM_ADMIN' "
                + "AND u.IsActive=1 AND u.IsLocked=0 "
                + "AND (u.LockedUntil IS NULL OR u.LockedUntil<=SYSUTCDATETIME())";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, actorId);
            try (ResultSet results = statement.executeQuery()) {
                return results.next();
            }
        }
    }

    private SubscriptionRecord findSubscriptionForUpdate(Connection connection, int companyId) throws SQLException {
        String sql = "SELECT SubscriptionId, Status FROM Subscriptions WITH (UPDLOCK,HOLDLOCK) WHERE CompanyId=?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, companyId);
            try (ResultSet results = statement.executeQuery()) {
                return results.next() ? new SubscriptionRecord(results.getInt("SubscriptionId"),
                        results.getString("Status")) : null;
            }
        }
    }

    private void insertAudit(Connection connection, int actorId, int companyId, int subscriptionId,
            String action, String oldStatus, String newStatus, String reason, String ipAddress)
            throws SQLException {
        AuditLog log = new AuditLog();
        log.setCompanyId(companyId);
        log.setUserId(actorId);
        log.setAction(action);
        log.setTableName("Subscriptions");
        log.setRecordId(String.valueOf(subscriptionId));
        log.setOldValues("{\"status\":\"" + oldStatus + "\"}");
        log.setNewValues("{\"status\":\"" + newStatus + "\""
                + (reason == null ? "" : ",\"reason\":\"" + jsonValue(reason) + "\"") + "}");
        log.setIpAddress(ipAddress == null || ipAddress.length() > 50 ? null : ipAddress);
        new AuditLogDAO().insert(connection, log);
    }

    private String jsonValue(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\r", "\\r").replace("\n", "\\n");
    }

    private CompanyAdministrationView map(ResultSet result) throws SQLException {
        CompanyAdministrationView company = new CompanyAdministrationView();
        company.setCompanyId(result.getInt("CompanyId"));
        company.setWorkspaceCode(result.getString("WorkspaceCode"));
        company.setCompanyName(result.getString("CompanyName"));
        company.setRegion(result.getString("Region"));
        company.setCompanyActive(result.getBoolean("CompanyIsActive"));
        company.setContactEmail(result.getString("ContactEmail"));
        company.setContactPhone(result.getString("ContactPhone"));
        company.setCompanyCreatedAt(toLocalDateTime(result.getTimestamp("CompanyCreatedAt")));
        company.setCompanyAdminName(result.getString("CompanyAdminName"));
        company.setCompanyAdminEmail(result.getString("CompanyAdminEmail"));
        company.setCompanyAdminActivatedAt(toLocalDateTime(result.getTimestamp("CompanyAdminActivatedAt")));
        company.setAccountCount(result.getInt("AccountCount"));
        company.setSubscriptionId(result.getInt("SubscriptionId"));
        company.setSubscriptionStatus(result.getString("SubscriptionStatus"));
        company.setPlanName(result.getString("PlanName"));
        company.setMaxAccounts(result.getInt("MaxAccounts"));
        company.setPrice(result.getBigDecimal("Price"));
        company.setBillingCycle(result.getString("BillingCycle"));
        company.setSubscriptionStartDate(toLocalDate(result.getDate("StartDate")));
        company.setSubscriptionEndDate(toLocalDate(result.getDate("EndDate")));
        company.setSuspendedAt(toLocalDateTime(result.getTimestamp("SuspendedAt")));
        company.setSuspensionReason(result.getString("SuspensionReason"));
        return company;
    }

    private LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }

    private LocalDate toLocalDate(Date date) {
        return date == null ? null : date.toLocalDate();
    }

    private void applyWriteOptions(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("SET ANSI_NULLS ON; SET QUOTED_IDENTIFIER ON; SET ANSI_PADDING ON; "
                    + "SET ANSI_WARNINGS ON; SET ARITHABORT ON; SET CONCAT_NULL_YIELDS_NULL ON; "
                    + "SET NUMERIC_ROUNDABORT OFF;");
        }
    }

    private record SubscriptionRecord(int subscriptionId, String status) {
    }

    public record MutationResult(String outcome, String status) {
        static MutationResult forbidden() {
            return new MutationResult("FORBIDDEN", null);
        }

        static MutationResult notFound() {
            return new MutationResult("NOT_FOUND", null);
        }

        static MutationResult conflict() {
            return new MutationResult("CONFLICT", null);
        }

        static MutationResult changed(String status) {
            return new MutationResult("CHANGED", status);
        }
    }
}

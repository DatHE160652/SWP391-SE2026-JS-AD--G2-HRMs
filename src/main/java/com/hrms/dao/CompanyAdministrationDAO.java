package com.hrms.dao;

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

/** Data access for System Admin company listings and subscription state transitions. */
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
        try (Connection connection = connections.open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            bindFilter(statement, filter);
            try (ResultSet results = statement.executeQuery()) {
                List<CompanyAdministrationView> companies = new ArrayList<>();
                while (results.next()) companies.add(map(results));
                return companies;
            }
        }
    }

    static String filterPredicate(CompanyAdministrationFilter filter) {
        StringBuilder sql = new StringBuilder(" WHERE 1=1");
        if (!filter.isAllStatuses()) sql.append(" AND s.Status = ?");
        if (!filter.getRegion().isEmpty()) sql.append(" AND c.Region = ?");
        return sql.toString();
    }

    static String orderBy(CompanyAdministrationFilter filter) {
        return switch (filter.getSort()) {
            case CompanyAdministrationFilter.NEWEST -> " ORDER BY c.CreatedAt DESC, c.CompanyId DESC";
            case CompanyAdministrationFilter.ACCOUNT_DESC -> " ORDER BY AccountCount DESC, c.CreatedAt DESC, c.CompanyId DESC";
            case CompanyAdministrationFilter.NAME_ASC -> " ORDER BY c.CompanyNameNormalized ASC, c.CompanyId ASC";
            default -> throw new IllegalArgumentException("Invalid trusted Company sort.");
        };
    }

    private void bindFilter(PreparedStatement statement, CompanyAdministrationFilter filter) throws SQLException {
        int index = 1;
        if (!filter.isAllStatuses()) statement.setString(index++, filter.getStatus());
        if (!filter.getRegion().isEmpty()) statement.setString(index, filter.getRegion());
    }

    public CompanyAdministrationView findById(int companyId) throws SQLException {
        String sql = "SELECT " + SELECT_COLUMNS + " WHERE c.CompanyId = ?";
        try (Connection connection = connections.open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, companyId);
            try (ResultSet results = statement.executeQuery()) {
                if (!results.next()) return null;
                CompanyAdministrationView company = map(results);
                company.setCoreMembers(findCoreMembers(connection, companyId));
                return company;
            }
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

    public MutationResult suspend(int companyId, int actorId, String reason, String ipAddress) throws SQLException {
        return changeSubscriptionStatus(companyId, actorId, reason, ipAddress, true);
    }

    public MutationResult activate(int companyId, int actorId, String ipAddress) throws SQLException {
        return activate(companyId, actorId, null, ipAddress);
    }

    public MutationResult activate(int companyId, int actorId, String reason, String ipAddress) throws SQLException {
        return changeSubscriptionStatus(companyId, actorId, reason, ipAddress, false);
    }

    private MutationResult changeSubscriptionStatus(int companyId, int actorId, String reason, String ipAddress,
                                                    boolean suspend) throws SQLException {
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
                String expectedStatus = suspend ? "ACTIVE" : "SUSPENDED";
                if (!expectedStatus.equals(subscription.status())) {
                    connection.rollback();
                    completed = true;
                    return MutationResult.conflict();
                }

                String nextStatus = suspend ? "SUSPENDED" : "ACTIVE";
                String sql = suspend
                        ? "UPDATE Subscriptions SET Status=N'SUSPENDED', SuspendedAt=SYSUTCDATETIME(), "
                        + "SuspendedByUserId=?, SuspensionReason=?, UpdatedAt=SYSUTCDATETIME() "
                        + "WHERE CompanyId=? AND Status=N'ACTIVE'"
                        : "UPDATE Subscriptions SET Status=N'ACTIVE', SuspendedAt=NULL, SuspendedByUserId=NULL, "
                        + "SuspensionReason=NULL, LastActivatedAt=SYSUTCDATETIME(), LastActivatedByUserId=?, "
                        + "UpdatedAt=SYSUTCDATETIME() WHERE CompanyId=? AND Status=N'SUSPENDED'";
                try (PreparedStatement statement = connection.prepareStatement(sql)) {
                    statement.setInt(1, actorId);
                    if (suspend) {
                        statement.setNString(2, reason);
                        statement.setInt(3, companyId);
                    } else {
                        statement.setInt(2, companyId);
                    }
                    if (statement.executeUpdate() != 1) {
                        connection.rollback();
                        completed = true;
                        return MutationResult.conflict();
                    }
                }
                insertAudit(connection, actorId, companyId, subscription.subscriptionId(),
                        suspend ? "SUSPEND_SUBSCRIPTION" : "ACTIVATE_SUBSCRIPTION",
                        subscription.status(), nextStatus, reason, ipAddress);
                connection.commit();
                completed = true;
                return MutationResult.changed(nextStatus);
            } catch (SQLException | RuntimeException exception) {
                try { connection.rollback(); } catch (SQLException ignored) { }
                completed = true;
                throw exception;
            } finally {
                if (!completed) {
                    try { connection.rollback(); } catch (SQLException ignored) { }
                }
            }
        }
    }

    private boolean isActiveSystemAdmin(Connection connection, int actorId) throws SQLException {
        String sql = "SELECT 1 FROM Users u INNER JOIN Roles r ON r.RoleId = u.RoleId "
                + "WHERE u.UserId=? AND u.CompanyId IS NULL AND r.RoleCode='SYSTEM_ADMIN' "
                + "AND u.IsActive=1 AND u.IsLocked=0 "
                + "AND (u.LockedUntil IS NULL OR u.LockedUntil<=SYSUTCDATETIME())";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, actorId);
            try (ResultSet results = statement.executeQuery()) { return results.next(); }
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

    private record SubscriptionRecord(int subscriptionId, String status) { }

    public record MutationResult(String outcome, String status) {
        static MutationResult forbidden() { return new MutationResult("FORBIDDEN", null); }
        static MutationResult notFound() { return new MutationResult("NOT_FOUND", null); }
        static MutationResult conflict() { return new MutationResult("CONFLICT", null); }
        static MutationResult changed(String status) { return new MutationResult("CHANGED", status); }
    }
}

package com.hrms.dao.sysadmin;

import com.hrms.model.CompanyAdministrationFilter;
import com.hrms.model.CompanyAdministrationView;
import com.hrms.model.CompanyMemberView;
import com.hrms.util.DBConnection;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Read-only data access for System Admin company listings and details. */
public class CompanyAdministrationDAO {
    private static final String SELECT_COLUMNS = """
            c.CompanyId, c.WorkspaceCode, c.CompanyName, c.Region, c.IsActive AS CompanyIsActive,
            c.ContactEmail, c.ContactPhone, c.CreatedAt AS CompanyCreatedAt,
            admin.DisplayName AS CompanyAdminName, admin.Email AS CompanyAdminEmail,
            admin.ActivatedAt AS CompanyAdminActivatedAt,
            (SELECT COUNT_BIG(*) FROM Users account WHERE account.CompanyId = c.CompanyId AND account.IsActive=1 AND account.IsLocked=0) AS AccountCount,
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
                while (results.next())
                    companies.add(map(results));
                return companies;
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
        try (Connection connection = connections.open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
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
}

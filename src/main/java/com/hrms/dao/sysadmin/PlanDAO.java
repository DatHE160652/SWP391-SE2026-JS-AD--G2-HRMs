package com.hrms.dao.sysadmin;
import com.hrms.dao.AuditLogDAO;

import com.hrms.model.AuditLog;
import com.hrms.model.Plan;
import com.hrms.util.DBConnection;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/** Data access and schema migration for platform subscription Plans. */
public class PlanDAO {
    private static volatile boolean schemaMigrated = false;

    public static void ensureSchema(Connection connection) throws SQLException {
        if (schemaMigrated) return;
        synchronized (PlanDAO.class) {
            if (schemaMigrated) return;
            String sql = """
                    DECLARE @dropSql NVARCHAR(MAX) = N'';
                    SELECT @dropSql += N'ALTER TABLE dbo.Plans DROP CONSTRAINT ' + QUOTENAME(cc.name) + N'; '
                    FROM sys.check_constraints cc
                    INNER JOIN sys.columns col ON col.object_id = cc.parent_object_id AND col.column_id = cc.parent_column_id
                    WHERE cc.parent_object_id = OBJECT_ID(N'dbo.Plans')
                      AND col.name IN (N'MaxAccounts', N'MaxManagers', N'BillingCycle')
                      AND cc.name NOT IN (N'CK_Plans_MaxAccounts_Positive', N'CK_Plans_MaxManagers_Positive', N'CK_Plans_BillingCycle_Valid');
                    IF LEN(@dropSql) > 0 EXEC sp_executesql @dropSql;

                    IF OBJECT_ID(N'dbo.CK_Plans_MaxAccounts_Positive', N'C') IS NULL
                        ALTER TABLE dbo.Plans ADD CONSTRAINT CK_Plans_MaxAccounts_Positive CHECK (MaxAccounts >= 1);
                    IF OBJECT_ID(N'dbo.CK_Plans_MaxManagers_Positive', N'C') IS NULL
                        ALTER TABLE dbo.Plans ADD CONSTRAINT CK_Plans_MaxManagers_Positive CHECK (MaxManagers >= 1);
                    IF OBJECT_ID(N'dbo.CK_Plans_BillingCycle_Valid', N'C') IS NULL
                        ALTER TABLE dbo.Plans ADD CONSTRAINT CK_Plans_BillingCycle_Valid CHECK (BillingCycle IN (N'MONTHLY', N'YEARLY'));
                    """;
            try (Statement statement = connection.createStatement()) {
                statement.execute(sql);
            }
            schemaMigrated = true;
        }
    }

    public List<Plan> findAll() throws SQLException {
        String sql = """
                SELECT p.PlanId, p.PlanName, p.MaxAccounts, p.MaxManagers, p.Price, p.BillingCycle, p.IsActive, p.CreatedAt,
                       (SELECT COUNT_BIG(*) FROM Subscriptions s WHERE s.PlanId = p.PlanId) AS CompanyCount
                FROM Plans p
                ORDER BY p.IsActive DESC, p.Price ASC, p.PlanId ASC
                """;
        try (Connection connection = DBConnection.getConnection()) {
            ensureSchema(connection);
            try (PreparedStatement statement = connection.prepareStatement(sql);
                 ResultSet rs = statement.executeQuery()) {
                List<Plan> list = new ArrayList<>();
                while (rs.next()) {
                    list.add(map(rs));
                }
                return list;
            }
        }
    }

    public List<Plan> findActive() throws SQLException {
        String sql = """
                SELECT p.PlanId, p.PlanName, p.MaxAccounts, p.MaxManagers, p.Price, p.BillingCycle, p.IsActive, p.CreatedAt,
                       (SELECT COUNT_BIG(*) FROM Subscriptions s WHERE s.PlanId = p.PlanId) AS CompanyCount
                FROM Plans p
                WHERE p.IsActive = 1
                ORDER BY p.Price ASC, p.PlanId ASC
                """;
        try (Connection connection = DBConnection.getConnection()) {
            ensureSchema(connection);
            try (PreparedStatement statement = connection.prepareStatement(sql);
                 ResultSet rs = statement.executeQuery()) {
                List<Plan> list = new ArrayList<>();
                while (rs.next()) {
                    list.add(map(rs));
                }
                return list;
            }
        }
    }

    public Plan findById(int planId) throws SQLException {
        String sql = """
                SELECT p.PlanId, p.PlanName, p.MaxAccounts, p.MaxManagers, p.Price, p.BillingCycle, p.IsActive, p.CreatedAt,
                       (SELECT COUNT_BIG(*) FROM Subscriptions s WHERE s.PlanId = p.PlanId) AS CompanyCount
                FROM Plans p
                WHERE p.PlanId = ?
                """;
        try (Connection connection = DBConnection.getConnection()) {
            ensureSchema(connection);
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setInt(1, planId);
                try (ResultSet rs = statement.executeQuery()) {
                    return rs.next() ? map(rs) : null;
                }
            }
        }
    }

    public int create(String planName, int maxAccounts, int maxManagers, BigDecimal price, String billingCycle,
                      int actorId, String ipAddress) throws SQLException {
        try (Connection connection = DBConnection.getConnection()) {
            ensureSchema(connection);
            connection.setAutoCommit(false);
            try {
                String sql = """
                        INSERT INTO Plans (PlanName, MaxEmployees, MaxAccounts, MaxManagers, Price, BillingCycle, IsActive)
                        OUTPUT INSERTED.PlanId
                        VALUES (?, NULL, ?, ?, ?, ?, 1)
                        """;
                int planId;
                try (PreparedStatement ps = connection.prepareStatement(sql)) {
                    ps.setNString(1, planName);
                    ps.setInt(2, maxAccounts);
                    ps.setInt(3, maxManagers);
                    ps.setBigDecimal(4, price);
                    ps.setString(5, billingCycle);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) throw new SQLException("Failed to insert Plan.");
                        planId = rs.getInt(1);
                    }
                }
                insertAudit(connection, actorId, "CREATE_PLAN", planId, null,
                        "{\"planName\":\"" + escapeJson(planName) + "\",\"maxAccounts\":" + maxAccounts
                                + ",\"maxManagers\":" + maxManagers + ",\"price\":" + price.toPlainString()
                                + ",\"billingCycle\":\"" + billingCycle + "\"}",
                        ipAddress);
                connection.commit();
                return planId;
            } catch (SQLException | RuntimeException ex) {
                try { connection.rollback(); } catch (SQLException ignored) { }
                throw ex;
            }
        }
    }

    public boolean update(int planId, String planName, int maxAccounts, int maxManagers, BigDecimal price,
                          String billingCycle, int actorId, String ipAddress) throws SQLException {
        try (Connection connection = DBConnection.getConnection()) {
            ensureSchema(connection);
            connection.setAutoCommit(false);
            try {
                String sql = """
                        UPDATE Plans
                        SET PlanName = ?, MaxAccounts = ?, MaxManagers = ?, Price = ?, BillingCycle = ?
                        WHERE PlanId = ?
                        """;
                int updated;
                try (PreparedStatement ps = connection.prepareStatement(sql)) {
                    ps.setNString(1, planName);
                    ps.setInt(2, maxAccounts);
                    ps.setInt(3, maxManagers);
                    ps.setBigDecimal(4, price);
                    ps.setString(5, billingCycle);
                    ps.setInt(6, planId);
                    updated = ps.executeUpdate();
                }
                if (updated != 1) {
                    connection.rollback();
                    return false;
                }
                insertAudit(connection, actorId, "UPDATE_PLAN", planId, null,
                        "{\"planName\":\"" + escapeJson(planName) + "\",\"maxAccounts\":" + maxAccounts
                                + ",\"maxManagers\":" + maxManagers + ",\"price\":" + price.toPlainString()
                                + ",\"billingCycle\":\"" + billingCycle + "\"}",
                        ipAddress);
                connection.commit();
                return true;
            } catch (SQLException | RuntimeException ex) {
                try { connection.rollback(); } catch (SQLException ignored) { }
                throw ex;
            }
        }
    }

    public boolean setActive(int planId, boolean active, int actorId, String ipAddress) throws SQLException {
        try (Connection connection = DBConnection.getConnection()) {
            ensureSchema(connection);
            connection.setAutoCommit(false);
            try {
                String sql = "UPDATE Plans SET IsActive = ? WHERE PlanId = ?";
                int updated;
                try (PreparedStatement ps = connection.prepareStatement(sql)) {
                    ps.setBoolean(1, active);
                    ps.setInt(2, planId);
                    updated = ps.executeUpdate();
                }
                if (updated != 1) {
                    connection.rollback();
                    return false;
                }
                insertAudit(connection, actorId, active ? "ACTIVATE_PLAN" : "DEACTIVATE_PLAN", planId,
                        null, "{\"isActive\":" + active + "}", ipAddress);
                connection.commit();
                return true;
            } catch (SQLException | RuntimeException ex) {
                try { connection.rollback(); } catch (SQLException ignored) { }
                throw ex;
            }
        }
    }

    private void insertAudit(Connection connection, int actorId, String action, int planId,
                             String oldValues, String newValues, String ipAddress) throws SQLException {
        AuditLog log = new AuditLog();
        log.setUserId(actorId);
        log.setAction(action);
        log.setTableName("Plans");
        log.setRecordId(String.valueOf(planId));
        log.setOldValues(oldValues);
        log.setNewValues(newValues);
        log.setIpAddress(ipAddress == null || ipAddress.length() > 50 ? null : ipAddress);
        new AuditLogDAO().insert(connection, log);
    }

    private static String escapeJson(String val) {
        return val == null ? "" : val.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private Plan map(ResultSet rs) throws SQLException {
        Plan plan = new Plan();
        plan.setPlanId(rs.getInt("PlanId"));
        plan.setPlanName(rs.getNString("PlanName"));
        plan.setMaxAccounts(rs.getInt("MaxAccounts"));
        plan.setMaxManagers(rs.getInt("MaxManagers"));
        plan.setPrice(rs.getBigDecimal("Price"));
        plan.setBillingCycle(rs.getString("BillingCycle"));
        plan.setActive(rs.getBoolean("IsActive"));
        Timestamp created = rs.getTimestamp("CreatedAt");
        plan.setCreatedAt(created == null ? null : created.toLocalDateTime());
        plan.setActiveCompanyCount(rs.getLong("CompanyCount"));
        return plan;
    }
}

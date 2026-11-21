package com.hrms.dao.companyadmin;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;

public final class ActiveAccountQuotaDAO {
    public static final String LIMIT_MESSAGE="Đã đạt đến giới hạn của gói bạn đang dùng hiện tại";
    private static boolean initialized;

    public static synchronized void ensureSchema(Connection connection) throws SQLException {
        if (initialized) return;
        try (var stream=ActiveAccountQuotaDAO.class.getResourceAsStream("/db/active-account-quota.sql")) {
            if (stream==null) throw new SQLException("Missing active-account quota migration.");
            try (var statement=connection.createStatement()) {
                statement.execute(new String(stream.readAllBytes(),StandardCharsets.UTF_8));
            }
            initialized=true;
        } catch (IOException e) { throw new SQLException("Cannot update active-account quota.",e); }
    }

    /** Call while holding the company transaction lock through account creation/reactivation. */
    public void requireCapacity(Connection connection,int companyId,Integer reactivatedUserId) throws SQLException {
        var dao=new EmployeeLifecycleDAO();
        // Unlocking an already active account does not consume another seat.
        if (reactivatedUserId!=null && !dao.query(connection,
                "SELECT 1 FROM Users WHERE CompanyId=? AND UserId=? AND IsActive=1 AND IsLocked=0",companyId,reactivatedUserId).isEmpty()) return;
        var rows=dao.query(connection,"""
                SELECT p.MaxAccounts,
                    (SELECT COUNT_BIG(*) FROM Users WITH(UPDLOCK,HOLDLOCK) WHERE CompanyId=? AND IsActive=1 AND IsLocked=0) ActiveAccounts
                FROM Subscriptions s WITH(UPDLOCK,HOLDLOCK) JOIN Plans p WITH(UPDLOCK,HOLDLOCK) ON p.PlanId=s.PlanId
                WHERE s.CompanyId=?
                """,companyId,companyId);
        if (rows.isEmpty()) throw new IllegalArgumentException("Workspace chưa có cấu hình gói sử dụng.");
        if (((Number)rows.get(0).get("ActiveAccounts")).longValue()>=((Number)rows.get(0).get("MaxAccounts")).longValue())
            throw new IllegalArgumentException(LIMIT_MESSAGE);
    }
}

package com.hrms.dao;

import com.hrms.model.LeavePolicy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class LeavePolicyDAO {
    public LeavePolicy getPolicy(Connection conn, int companyId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT QuotaMode,QuotaDays,MaxDaysPerRequest FROM CompanyLeavePolicies WHERE CompanyId=?")) {
            ps.setInt(1, companyId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? new LeavePolicy(companyId, rs.getString(1), rs.getInt(2), rs.getInt(3)) : null;
            }
        }
    }

    /** Serialize policy changes, submissions and decisions in the same tenant. */
    public void lockCompany(Connection conn, int companyId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT CompanyId FROM Companies WITH (UPDLOCK,HOLDLOCK) WHERE CompanyId=?")) {
            ps.setInt(1, companyId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new IllegalArgumentException("Công ty không tồn tại.");
            }
        }
    }

    public void savePolicy(Connection conn, LeavePolicy policy, int actorId) throws SQLException {
        String sql = """
            UPDATE CompanyLeavePolicies SET QuotaMode=?,QuotaDays=?,MaxDaysPerRequest=?,
                UpdatedByUserId=?,UpdatedAt=SYSUTCDATETIME() WHERE CompanyId=?;
            IF @@ROWCOUNT=0
                INSERT INTO CompanyLeavePolicies(QuotaMode,QuotaDays,MaxDaysPerRequest,UpdatedByUserId,CompanyId)
                VALUES(?,?,?,?,?);
            """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int base : new int[]{0, 5}) {
                ps.setString(base + 1, policy.quotaMode());
                ps.setInt(base + 2, policy.quotaDays());
                ps.setInt(base + 3, policy.maxDaysPerRequest());
                ps.setInt(base + 4, actorId);
                ps.setInt(base + 5, policy.companyId());
            }
            ps.executeUpdate();
        }
    }
}

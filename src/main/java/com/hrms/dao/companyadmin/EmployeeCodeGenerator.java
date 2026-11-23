package com.hrms.dao.companyadmin;

import java.sql.*;
import java.util.Locale;

/** Allocates codes while the caller holds a transaction until insertion commits. */
public final class EmployeeCodeGenerator {
    private EmployeeCodeGenerator() {}

    public static String next(Connection connection, int companyId) throws SQLException {
        if (connection.getAutoCommit()) throw new SQLException("Employee code allocation requires a transaction.");
        OrganizationTransaction.lockCompany(connection, companyId);
        String prefix;
        try (PreparedStatement ps = connection.prepareStatement("SELECT WorkspaceCode FROM Companies WHERE CompanyId=?")) {
            ps.setInt(1, companyId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new SQLException("Workspace not found.");
                prefix = rs.getString(1);
            }
        }
        if (prefix == null || prefix.isBlank()) throw new SQLException("Workspace code is required.");
        prefix = prefix.trim().toUpperCase(Locale.ROOT);
        long maximum = 0;
        long count = 0;
        try (PreparedStatement ps = connection.prepareStatement("SELECT EmployeeCode FROM Employees WHERE CompanyId=?")) {
            ps.setInt(1, companyId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    count++;
                    String code = rs.getString(1);
                    if (code != null && code.regionMatches(true, 0, prefix, 0, prefix.length())) {
                        String suffix = code.substring(prefix.length());
                        if (suffix.matches("[0-9]+")) {
                            try { maximum = Math.max(maximum, Long.parseLong(suffix)); }
                            catch (NumberFormatException e) { throw new SQLException("Employee code sequence exceeds supported range.", e); }
                        }
                    }
                }
            }
        }
        long current = Math.max(count, maximum);
        if (current == Long.MAX_VALUE) throw new SQLException("Employee code sequence exhausted.");
        String code = prefix + String.format(Locale.ROOT, "%03d", current + 1);
        if (code.length() > 50) throw new SQLException("Generated employee code exceeds 50 characters.");
        return code;
    }
}

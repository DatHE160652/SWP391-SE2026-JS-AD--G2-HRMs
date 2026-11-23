package com.hrms.dao.sysadmin;

import com.hrms.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/** Read-only queries for System Admin EmailAttempts monitoring. */
public class PlatformAuditDAO {
    private static final ZoneId DISPLAY_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter DISPLAY_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    public record EmailAttemptEntry(long emailAttemptId, String emailType, String recipientEmail,
                                    String status, String errorCode, Integer registrationRequestId,
                                    String companyName, String createdAtDisplay, String completedAtDisplay) {}

    public List<EmailAttemptEntry> findEmailAttempts(String statusFilter, String query, int limit) throws SQLException {
        StringBuilder sql = new StringBuilder("""
                SELECT TOP (?) ea.EmailAttemptId, ea.EmailType, ea.RecipientEmail, ea.Status, ea.ErrorCode,
                       ea.RegistrationRequestId, rr.CompanyName, ea.CreatedAt, ea.CompletedAt
                FROM EmailAttempts ea
                LEFT JOIN RegistrationRequests rr ON rr.RegistrationRequestId = ea.RegistrationRequestId
                WHERE 1=1
                """);
        List<Object> params = new ArrayList<>();
        params.add(limit);

        if (statusFilter != null && !statusFilter.isBlank()) {
            sql.append(" AND ea.Status = ? ");
            params.add(statusFilter.trim());
        }
        if (query != null && !query.isBlank()) {
            sql.append(" AND (ea.RecipientEmail LIKE ? OR rr.CompanyName LIKE ? OR ea.ErrorCode LIKE ?) ");
            String like = "%" + query.trim() + "%";
            for (int i = 0; i < 3; i++) params.add(like);
        }
        sql.append(" ORDER BY ea.EmailAttemptId DESC");

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                Object p = params.get(i);
                if (p instanceof Integer n) ps.setInt(i + 1, n);
                else ps.setNString(i + 1, String.valueOf(p));
            }
            try (ResultSet rs = ps.executeQuery()) {
                List<EmailAttemptEntry> list = new ArrayList<>();
                while (rs.next()) {
                    list.add(new EmailAttemptEntry(
                            rs.getLong("EmailAttemptId"),
                            rs.getString("EmailType"),
                            rs.getString("RecipientEmail"),
                            rs.getString("Status"),
                            rs.getString("ErrorCode"),
                            (Integer) rs.getObject("RegistrationRequestId"),
                            rs.getNString("CompanyName"),
                            formatTimestamp(rs.getTimestamp("CreatedAt")),
                            formatTimestamp(rs.getTimestamp("CompletedAt"))
                    ));
                }
                return list;
            }
        }
    }

    private static String formatTimestamp(Timestamp ts) {
        if (ts == null) return "—";
        return ts.toInstant().atZone(DISPLAY_ZONE).format(DISPLAY_TIME);
    }
}

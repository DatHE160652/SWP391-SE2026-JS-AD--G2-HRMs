package com.hrms.dao.sysadmin;

import com.hrms.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Read-only queries for System Admin AuditLogs and EmailAttempts monitoring. */
public class PlatformAuditDAO {
    private static final ZoneId DISPLAY_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter DISPLAY_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
    private static final DateTimeFormatter DISPLAY_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final Pattern JSON_KV = Pattern.compile("\"([^\"]+)\"\\s*:\\s*(?:\"([^\"]*)\"|(-?\\d+(?:\\.\\d+)?|true|false))");

    public record AuditLogEntry(long logId, String action, String tableName, String recordId,
                                int actorUserId, String actorName, String actorEmail, String companyName,
                                String oldValues, String newValues, String ipAddress,
                                String createdAtDisplay) {
        public String oldValuesDisplay() {
            return formatAuditJson(oldValues);
        }

        public String newValuesDisplay() {
            return formatAuditJson(newValues);
        }

        public String actionLabel() {
            return translateAction(action);
        }
    }

    public record EmailAttemptEntry(long emailAttemptId, String emailType, String recipientEmail,
                                    String status, String errorCode, Integer registrationRequestId,
                                    String companyName, String createdAtDisplay, String completedAtDisplay) {}

    public List<AuditLogEntry> findPlatformAuditLogs(String actionFilter, String query, int limit) throws SQLException {
        StringBuilder sql = new StringBuilder("""
                SELECT TOP (?) a.LogId, a.UserId AS ActorUserId, a.Action, a.TableName, a.RecordId, a.OldValues, a.NewValues, a.IpAddress, a.CreatedAt,
                       u.DisplayName AS ActorName, u.Email AS ActorEmail, c.CompanyName
                FROM AuditLogs a
                LEFT JOIN Users u ON u.UserId = a.UserId
                LEFT JOIN Companies c ON c.CompanyId = a.CompanyId
                WHERE (u.CompanyId IS NULL OR a.TableName IN ('RegistrationRequests', 'Subscriptions', 'Plans'))
                """);
        List<Object> params = new ArrayList<>();
        params.add(limit);

        if (actionFilter != null && !actionFilter.isBlank()) {
            sql.append(" AND a.Action = ? ");
            params.add(actionFilter.trim());
        }
        if (query != null && !query.isBlank()) {
            sql.append(" AND (a.Action LIKE ? OR a.TableName LIKE ? OR u.Email LIKE ? OR c.CompanyName LIKE ? OR a.RecordId LIKE ?) ");
            String like = "%" + query.trim() + "%";
            for (int i = 0; i < 5; i++) params.add(like);
        }
        sql.append(" ORDER BY a.LogId DESC");

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                Object p = params.get(i);
                if (p instanceof Integer n) ps.setInt(i + 1, n);
                else ps.setNString(i + 1, String.valueOf(p));
            }
            try (ResultSet rs = ps.executeQuery()) {
                List<AuditLogEntry> list = new ArrayList<>();
                while (rs.next()) {
                    list.add(new AuditLogEntry(
                            rs.getLong("LogId"),
                            rs.getNString("Action"),
                            rs.getNString("TableName"),
                            rs.getString("RecordId"),
                            rs.getInt("ActorUserId"),
                            rs.getNString("ActorName"),
                            rs.getString("ActorEmail"),
                            rs.getNString("CompanyName"),
                            rs.getNString("OldValues"),
                            rs.getNString("NewValues"),
                            rs.getString("IpAddress"),
                            formatTimestamp(rs.getTimestamp("CreatedAt"))
                    ));
                }
                return list;
            }
        }
    }

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

    private static String formatAuditJson(String raw) {
        if (raw == null || raw.isBlank()) return "—";
        Matcher matcher = JSON_KV.matcher(raw);
        List<String> parts = new ArrayList<>();
        while (matcher.find()) {
            String key = matcher.group(1);
            String val = matcher.group(2) != null ? matcher.group(2) : matcher.group(3);
            if ("revertedLogId".equals(key)) continue;
            parts.add(formatPair(key, val));
        }
        return parts.isEmpty() ? raw : String.join(" · ", parts);
    }

    private static String formatPair(String key, String val) {
        return switch (key) {
            case "status" -> "Trạng thái: " + translateStatus(val);
            case "reason" -> "Ghi chú: " + val;
            default -> key + ": " + val;
        };
    }

    private static String translateAction(String action) {
        if (action == null) return "—";
        return switch (action.toUpperCase(Locale.ROOT)) {
            case "CONFIRM_REQUEST" -> "Phê duyệt & khởi tạo doanh nghiệp";
            case "MARK_UNDER_REVIEW" -> "Bắt đầu xem xét yêu cầu";
            case "DELETE_REQUEST", "SOFT_DELETE_REQUEST" -> "Từ chối / Xóa yêu cầu";
            case "RESEND_ACTIVATION" -> "Gửi lại email kích hoạt";
            default -> action;
        };
    }

    private static String translateStatus(String status) {
        if (status == null) return "—";
        return switch (status.toUpperCase(Locale.ROOT)) {
            case "ACTIVE" -> "Đang hoạt động";
            case "SUSPENDED" -> "Đã tạm ngưng";
            case "NEW" -> "Mới tiếp nhận";
            case "UNDER_REVIEW" -> "Đang xem xét";
            case "CONVERTED" -> "Đã khởi tạo";
            case "DELETED" -> "Đã xóa";
            case "PENDING_ACTIVATION" -> "Chờ kích hoạt";
            case "RESENT" -> "Đã gửi lại email kích hoạt";
            default -> status;
        };
    }

    private static String formatDateSafe(String isoDate) {
        try {
            return LocalDate.parse(isoDate).format(DISPLAY_DATE);
        } catch (Exception ex) {
            return isoDate;
        }
    }

    private static String formatPriceSafe(String rawPrice) {
        try {
            long num = new java.math.BigDecimal(rawPrice).longValue();
            return NumberFormat.getNumberInstance(Locale.US).format(num);
        } catch (Exception ex) {
            return rawPrice;
        }
    }

    private static String formatTimestamp(Timestamp ts) {
        if (ts == null) return "—";
        return ts.toInstant().atZone(DISPLAY_ZONE).format(DISPLAY_TIME);
    }
}

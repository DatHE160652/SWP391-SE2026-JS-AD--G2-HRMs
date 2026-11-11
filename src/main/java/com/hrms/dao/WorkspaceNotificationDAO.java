package com.hrms.dao;

import com.hrms.service.security.TenantPrincipal;
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
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class WorkspaceNotificationDAO {

    private static final ZoneId VN_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public synchronized void ensureSchema(Connection connection) throws SQLException {
        String ddl = """
                IF OBJECT_ID('dbo.UserNotificationReads', 'U') IS NULL
                BEGIN
                    CREATE TABLE dbo.UserNotificationReads (
                        CompanyId INT NOT NULL,
                        UserId INT NOT NULL,
                        NotificationKey VARCHAR(160) NOT NULL,
                        ReadAt DATETIME2(7) NOT NULL CONSTRAINT DF_UserNotificationReads_ReadAt DEFAULT (SYSUTCDATETIME()),
                        CONSTRAINT PK_UserNotificationReads PRIMARY KEY (CompanyId, UserId, NotificationKey)
                    );
                END;
                """;
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(ddl);
        }
    }

    public Map<String, Object> getNotificationsForUser(TenantPrincipal principal) throws SQLException {
        int companyId = principal.getCompanyId();
        int userId = principal.getUserId();
        Integer employeeId = principal.getEmployeeId();
        String roleCode = principal.getRoleCode() == null ? "" : principal.getRoleCode().toUpperCase();
        boolean isAdminOrHr = "COMPANY_ADMIN".equals(roleCode) || "HR".equals(roleCode);
        boolean isManager = "MANAGER".equals(roleCode);

        try (Connection connection = DBConnection.getConnection()) {
            ensureSchema(connection);
            new ProfileUpdateDAO().ensureSchema(connection, companyId);
            new DocumentUpdatePeriodDAO().ensureSchemaAndCompanyDefaults(connection, companyId);

            Set<String> readKeys = loadReadKeys(connection, companyId, userId);
            List<Map<String, Object>> items = new ArrayList<>();

            // 1. Update Period Notifications (Profile & Document Update Periods)
            collectUpdatePeriodNotifications(connection, companyId, employeeId, roleCode, isAdminOrHr, readKeys, items);

            // 2. Attendance / Check-in / Explanation Notifications
            collectAttendanceNotifications(connection, companyId, employeeId, roleCode, isAdminOrHr, isManager, readKeys, items);

            // 3. Leave & Payslip Notifications
            collectLeaveAndPayslipNotifications(connection, companyId, employeeId, roleCode, isAdminOrHr, isManager, readKeys, items);

            // Sort: unread first, then by sortTimestamp descending
            items.sort((a, b) -> {
                boolean readA = Boolean.TRUE.equals(a.get("read"));
                boolean readB = Boolean.TRUE.equals(b.get("read"));
                if (readA != readB) {
                    return readA ? 1 : -1;
                }
                long tsA = ((Number) a.getOrDefault("sortEpoch", 0L)).longValue();
                long tsB = ((Number) b.getOrDefault("sortEpoch", 0L)).longValue();
                return Long.compare(tsB, tsA);
            });

            int unreadCount = 0;
            for (Map<String, Object> item : items) {
                item.remove("sortEpoch");
                if (!Boolean.TRUE.equals(item.get("read"))) {
                    unreadCount++;
                }
            }

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("unreadCount", unreadCount);
            result.put("items", items);
            return result;
        }
    }

    public void markNotificationsRead(TenantPrincipal principal, List<String> keys, boolean markAll) throws SQLException {
        int companyId = principal.getCompanyId();
        int userId = principal.getUserId();
        Integer employeeId = principal.getEmployeeId();

        List<String> targetKeys = new ArrayList<>();
        if (markAll) {
            Map<String, Object> current = getNotificationsForUser(principal);
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> items = (List<Map<String, Object>>) current.get("items");
            if (items != null) {
                for (Map<String, Object> item : items) {
                    Object k = item.get("key");
                    if (k != null) {
                        targetKeys.add(String.valueOf(k));
                    }
                }
            }
        } else if (keys != null) {
            for (String k : keys) {
                if (k != null && !k.isBlank()) {
                    targetKeys.add(k.trim());
                }
            }
        }

        if (targetKeys.isEmpty()) {
            return;
        }

        try (Connection connection = DBConnection.getConnection()) {
            ensureSchema(connection);
            String mergeSql = """
                    IF NOT EXISTS (
                        SELECT 1 FROM dbo.UserNotificationReads
                        WHERE CompanyId = ? AND UserId = ? AND NotificationKey = ?
                    )
                    BEGIN
                        INSERT INTO dbo.UserNotificationReads (CompanyId, UserId, NotificationKey)
                        VALUES (?, ?, ?);
                    END
                    """;
            try (PreparedStatement ps = connection.prepareStatement(mergeSql)) {
                for (String key : targetKeys) {
                    String safeKey = key.length() > 160 ? key.substring(0, 160) : key;
                    ps.setInt(1, companyId);
                    ps.setInt(2, userId);
                    ps.setString(3, safeKey);
                    ps.setInt(4, companyId);
                    ps.setInt(5, userId);
                    ps.setString(6, safeKey);
                    ps.addBatch();
                }
                ps.executeBatch();
            }

            if (employeeId != null && employeeId > 0) {
                for (String key : targetKeys) {
                    if (key.startsWith("payslip:")) {
                        try {
                            long notifId = Long.parseLong(key.substring("payslip:".length()));
                            try (PreparedStatement ps = connection.prepareStatement(
                                    "UPDATE dbo.PayrollNotifications SET ReadAt = SYSUTCDATETIME() WHERE NotificationId = ? AND CompanyId = ? AND EmployeeId = ? AND ReadAt IS NULL")) {
                                ps.setLong(1, notifId);
                                ps.setInt(2, companyId);
                                ps.setInt(3, employeeId);
                                ps.executeUpdate();
                            }
                        } catch (NumberFormatException ignored) {
                        }
                    }
                }
            }
        }
    }

    private Set<String> loadReadKeys(Connection connection, int companyId, int userId) throws SQLException {
        Set<String> set = new HashSet<>();
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT NotificationKey FROM dbo.UserNotificationReads WHERE CompanyId = ? AND UserId = ?")) {
            ps.setInt(1, companyId);
            ps.setInt(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    set.add(rs.getString("NotificationKey"));
                }
            }
        }
        return set;
    }

    private void collectUpdatePeriodNotifications(
            Connection connection,
            int companyId,
            Integer employeeId,
            String roleCode,
            boolean isAdminOrHr,
            Set<String> readKeys,
            List<Map<String, Object>> items
    ) throws SQLException {
        LocalDateTime nowUtc = LocalDateTime.now(ZoneOffset.UTC);

        // A. Active or Scheduled Profile Update Periods (ClosedAt IS NULL AND EndsAt > SYSUTCDATETIME())
        String openProfileSql = """
                SELECT TOP 8 p.PeriodId, p.Title, p.StartsAt, p.EndsAt, p.CreatedAt, ISNULL(p.ScopeType, 'ALL') AS ScopeType,
                       (SELECT COUNT(DISTINCT l.EmployeeId) FROM dbo.ProfileUpdatePeriodLogs l WHERE l.CompanyId = p.CompanyId AND l.PeriodId = p.PeriodId) AS UpdatedEmpCount
                FROM dbo.ProfileUpdatePeriods p
                WHERE p.CompanyId = ? AND p.PeriodType = 'PROFILE' AND p.ClosedAt IS NULL
                  AND p.EndsAt > SYSUTCDATETIME()
                ORDER BY p.PeriodId DESC
                """;
        try (PreparedStatement ps = connection.prepareStatement(openProfileSql)) {
            ps.setInt(1, companyId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    long pid = rs.getLong("PeriodId");
                    String scopeType = rs.getString("ScopeType");
                    if (!isAdminOrHr && "SELECTED".equalsIgnoreCase(scopeType)) {
                        if (employeeId == null || !isEmployeeInProfilePeriod(connection, pid, employeeId)) {
                            continue;
                        }
                    }
                    String title = rs.getString("Title");
                    Timestamp startsTs = rs.getTimestamp("StartsAt");
                    Timestamp endsTs = rs.getTimestamp("EndsAt");
                    Timestamp createdTs = rs.getTimestamp("CreatedAt");
                    int updatedCount = rs.getInt("UpdatedEmpCount");
                    boolean isOpenNow = startsTs != null && !nowUtc.isBefore(startsTs.toLocalDateTime());
                    String fieldsLabel = getProfilePeriodFieldLabels(connection, pid);
                    String key = "profile-period:" + pid;
                    String startStr = formatUtcTimestamp(startsTs);
                    String endStr = formatUtcTimestamp(endsTs);

                    String notifTitle = (isOpenNow ? "Đợt cập nhật hồ sơ đang mở: " : "Đợt cập nhật hồ sơ sắp mở: ") + title;
                    String body;
                    if (isAdminOrHr) {
                        body = "Thông tin cho phép: " + fieldsLabel + " · Hạn: " + endStr + " (Đã cập nhật: " + updatedCount + " NV).";
                    } else if (isOpenNow) {
                        body = "Bạn được cập nhật: " + fieldsLabel + " (Hạn cuối: " + endStr + "). Nhấn để cập nhật ngay.";
                    } else {
                        body = "Sẽ mở từ " + startStr + " đến " + endStr + " cho các mục: " + fieldsLabel + ".";
                    }
                    String url = isAdminOrHr ? "/employee/profile-update-periods" : "/employee/my-profile";
                    long sortEpoch = createdTs != null ? createdTs.getTime() : (startsTs != null ? startsTs.getTime() : System.currentTimeMillis());
                    items.add(buildItem(
                            key,
                            "UPDATE_PERIOD",
                            notifTitle,
                            body,
                            formatUtcTimestamp(createdTs != null ? createdTs : startsTs),
                            url,
                            readKeys.contains(key),
                            sortEpoch
                    ));
                }
            }
        }

        // B. Active or Scheduled Document Update Periods
        String openDocSql = """
                SELECT TOP 8 p.DocumentPeriodId, p.Title, p.StartsAt, p.EndsAt, p.CreatedAt, p.ScopeType,
                       (SELECT COUNT(DISTINCT d.EmployeeId) FROM dbo.EmployeeDocuments d WHERE d.DocumentPeriodId = p.DocumentPeriodId) AS UpdatedEmpCount
                FROM dbo.DocumentUpdatePeriods p
                WHERE p.CompanyId = ? AND p.ClosedAt IS NULL
                  AND p.EndsAt > SYSUTCDATETIME()
                ORDER BY p.DocumentPeriodId DESC
                """;
        try (PreparedStatement ps = connection.prepareStatement(openDocSql)) {
            ps.setInt(1, companyId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    long pid = rs.getLong("DocumentPeriodId");
                    String scopeType = rs.getString("ScopeType");
                    if (!isAdminOrHr && "SELECTED".equalsIgnoreCase(scopeType)) {
                        if (employeeId == null || !isEmployeeInDocPeriod(connection, pid, employeeId)) {
                            continue;
                        }
                    }
                    String title = rs.getString("Title");
                    Timestamp startsTs = rs.getTimestamp("StartsAt");
                    Timestamp endsTs = rs.getTimestamp("EndsAt");
                    Timestamp createdTs = rs.getTimestamp("CreatedAt");
                    int updatedCount = rs.getInt("UpdatedEmpCount");
                    boolean isOpenNow = startsTs != null && !nowUtc.isBefore(startsTs.toLocalDateTime());
                    String docTypesLabel = getDocPeriodTypeLabels(connection, pid);
                    String key = "doc-period:" + pid;
                    String startStr = formatUtcTimestamp(startsTs);
                    String endStr = formatUtcTimestamp(endsTs);

                    String notifTitle = (isOpenNow ? "Đợt cập nhật tài liệu đang mở: " : "Đợt cập nhật tài liệu sắp mở: ") + title;
                    String body;
                    if (isAdminOrHr) {
                        body = "Tài liệu yêu cầu: " + docTypesLabel + " · Hạn: " + endStr + " (Đã nộp: " + updatedCount + " NV).";
                    } else if (isOpenNow) {
                        body = "Yêu cầu bổ sung: " + docTypesLabel + " (Hạn cuối: " + endStr + "). Nhấn để tải tài liệu lên.";
                    } else {
                        body = "Sẽ mở từ " + startStr + " đến " + endStr + " cho: " + docTypesLabel + ".";
                    }
                    String url = isAdminOrHr ? "/employee/document-update-periods" : "/employee/my-profile";
                    long sortEpoch = createdTs != null ? createdTs.getTime() : (startsTs != null ? startsTs.getTime() : System.currentTimeMillis());
                    items.add(buildItem(
                            key,
                            "UPDATE_PERIOD",
                            notifTitle,
                            body,
                            formatUtcTimestamp(createdTs != null ? createdTs : startsTs),
                            url,
                            readKeys.contains(key),
                            sortEpoch
                    ));
                }
            }
        }

        // C. For Company Admin / HR: Recent Employee Profile & Document Submissions
        if (isAdminOrHr) {
            String recentProfileLogsSql = """
                    SELECT TOP 8 l.LogId, l.PeriodId, p.Title, e.FullName, e.EmployeeCode,
                           l.FieldLabel, l.NewValue, l.UpdatedAt
                    FROM dbo.ProfileUpdatePeriodLogs l
                    JOIN dbo.ProfileUpdatePeriods p ON p.PeriodId = l.PeriodId
                    JOIN dbo.Employees e ON e.EmployeeId = l.EmployeeId AND e.CompanyId = l.CompanyId
                    WHERE l.CompanyId = ?
                    ORDER BY l.UpdatedAt DESC, l.LogId DESC
                    """;
            try (PreparedStatement ps = connection.prepareStatement(recentProfileLogsSql)) {
                ps.setInt(1, companyId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        long logId = rs.getLong("LogId");
                        String key = "hr-profile-log:" + logId;
                        String empName = rs.getString("FullName");
                        String empCode = rs.getString("EmployeeCode");
                        String fieldLabel = rs.getString("FieldLabel");
                        String newVal = rs.getString("NewValue");
                        String periodTitle = rs.getString("Title");
                        Timestamp updatedTs = rs.getTimestamp("UpdatedAt");
                        items.add(buildItem(
                                key,
                                "UPDATE_PERIOD",
                                empName + " (" + empCode + ") đã cập nhật " + fieldLabel,
                                "Giá trị mới: " + (newVal == null || newVal.isBlank() ? "—" : newVal) + " · Đợt: " + periodTitle,
                                formatUtcTimestamp(updatedTs),
                                "/employee/profile-update-periods",
                                readKeys.contains(key),
                                updatedTs != null ? updatedTs.getTime() : 0L
                        ));
                    }
                }
            }

            String recentDocUploadsSql = """
                    SELECT TOP 8 d.DocumentId, d.DocumentName, d.DocumentType, d.UploadedDate,
                           e.FullName, e.EmployeeCode, p.Title
                    FROM dbo.EmployeeDocuments d
                    JOIN dbo.DocumentUpdatePeriods p ON p.DocumentPeriodId = d.DocumentPeriodId
                    JOIN dbo.Employees e ON e.EmployeeId = d.EmployeeId AND e.CompanyId = p.CompanyId
                    WHERE p.CompanyId = ? AND d.DocumentPeriodId IS NOT NULL
                    ORDER BY d.UploadedDate DESC, d.DocumentId DESC
                    """;
            try (PreparedStatement ps = connection.prepareStatement(recentDocUploadsSql)) {
                ps.setInt(1, companyId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        int docId = rs.getInt("DocumentId");
                        String key = "hr-doc-upload:" + docId;
                        String empName = rs.getString("FullName");
                        String empCode = rs.getString("EmployeeCode");
                        String docType = rs.getString("DocumentType");
                        String docName = rs.getString("DocumentName");
                        String periodTitle = rs.getString("Title");
                        Timestamp uploadedTs = rs.getTimestamp("UploadedDate");
                        items.add(buildItem(
                                key,
                                "UPDATE_PERIOD",
                                empName + " (" + empCode + ") đã nộp " + (docType != null ? docType : "tài liệu"),
                                "Tệp: " + docName + " · Đợt: " + periodTitle,
                                formatUtcTimestamp(uploadedTs),
                                "/employee/document-update-periods",
                                readKeys.contains(key),
                                uploadedTs != null ? uploadedTs.getTime() : 0L
                        ));
                    }
                }
            }
        }
    }

    private String getProfilePeriodFieldLabels(Connection connection, long periodId) throws SQLException {
        List<String> labels = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT FieldKey FROM dbo.ProfileUpdatePeriodFields WHERE PeriodId = ?")) {
            ps.setLong(1, periodId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String fk = rs.getString("FieldKey");
                    ProfileUpdateDAO.ProfileFieldDef def = ProfileUpdateDAO.FIELD_BY_KEY.get(fk);
                    if (def != null) {
                        labels.add(def.fieldLabel());
                    }
                }
            }
        }
        if (labels.isEmpty()) {
            return "Thông tin liên hệ & CCCD";
        }
        return String.join(", ", labels);
    }

    private String getDocPeriodTypeLabels(Connection connection, long periodId) throws SQLException {
        List<String> names = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement("""
                SELECT dt.TypeName
                FROM dbo.DocumentUpdatePeriodTypes pt
                JOIN dbo.CompanyDocumentTypes dt ON dt.DocumentTypeId = pt.DocumentTypeId
                WHERE pt.DocumentPeriodId = ?
                ORDER BY dt.DocumentTypeId ASC
                """)) {
            ps.setLong(1, periodId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    names.add(rs.getString("TypeName"));
                }
            }
        }
        if (names.isEmpty()) {
            return "Tất cả loại tài liệu";
        }
        return String.join(", ", names);
    }

    private void collectAttendanceNotifications(
            Connection connection,
            int companyId,
            Integer employeeId,
            String roleCode,
            boolean isAdminOrHr,
            boolean isManager,
            Set<String> readKeys,
            List<Map<String, Object>> items
    ) throws SQLException {
        LocalDate todayVn = LocalDate.now(VN_ZONE);
        String todayStr = todayVn.format(DATE_FMT);
        String personalAttUrl = isManager ? "/attendance/manager-personal-attendance" : "/attendance/employee-attendance";

        // A. Personal attendance status today (for employees / managers with an EmployeeId)
        if (employeeId != null && employeeId > 0 && !isAdminOrHr) {
            String attSql = """
                    SELECT AttendanceId, CheckInTime, CheckOutTime, TotalHours, Status
                    FROM dbo.Attendances
                    WHERE CompanyId = ? AND EmployeeId = ? AND AttendanceDate = ?
                    """;
            try (PreparedStatement ps = connection.prepareStatement(attSql)) {
                ps.setInt(1, companyId);
                ps.setInt(2, employeeId);
                ps.setDate(3, Date.valueOf(todayVn));
                try (ResultSet rs = ps.executeQuery()) {
                    long todayEpoch = LocalDateTime.now(ZoneOffset.UTC).toInstant(ZoneOffset.UTC).toEpochMilli();
                    if (!rs.next() || rs.getTime("CheckInTime") == null) {
                        String key = "att-checkin-reminder:" + employeeId + ":" + todayVn;
                        items.add(buildItem(
                                key,
                                "ATTENDANCE",
                                "Nhắc điểm danh hôm nay (" + todayStr + ")",
                                "Bạn chưa chấm công vào ca hôm nay. Nhấn để thực hiện điểm danh.",
                                "Hôm nay",
                                personalAttUrl,
                                readKeys.contains(key),
                                todayEpoch - 300000
                        ));
                    } else {
                        String checkIn = String.valueOf(rs.getTime("CheckInTime"));
                        Object checkOutObj = rs.getTime("CheckOutTime");
                        String status = rs.getString("Status");
                        if (checkOutObj == null) {
                            String key = "att-checkedin:" + employeeId + ":" + todayVn;
                            items.add(buildItem(
                                    key,
                                    "ATTENDANCE",
                                    "Điểm danh hôm nay: Đã vào ca lúc " + checkIn,
                                    "Đừng quên chấm công ra ca trước khi kết thúc ngày làm việc.",
                                    "Hôm nay " + checkIn,
                                    personalAttUrl,
                                    readKeys.contains(key),
                                    todayEpoch - 301000
                            ));
                        } else {
                            String checkOut = String.valueOf(checkOutObj);
                            String key = "att-completed:" + employeeId + ":" + todayVn;
                            items.add(buildItem(
                                    key,
                                    "ATTENDANCE",
                                    "Điểm danh hôm nay hoàn tất (" + translateAttendanceStatus(status) + ")",
                                    "Giờ vào: " + checkIn + " · Giờ ra: " + checkOut + " · Tổng giờ: " + rs.getBigDecimal("TotalHours") + "h",
                                    "Hôm nay " + checkOut,
                                    personalAttUrl,
                                    readKeys.contains(key),
                                    todayEpoch - 302000
                            ));
                        }
                    }
                }
            }

            if (hasTable(connection, "dbo.AttendanceExplanations")) {
                String myExpSql = """
                        SELECT TOP 5 ExplanationId, AttendanceDate, Status, ReviewedAt, CreatedAt
                        FROM dbo.AttendanceExplanations
                        WHERE CompanyId = ? AND EmployeeId = ?
                        ORDER BY COALESCE(ReviewedAt, CreatedAt) DESC
                        """;
                try (PreparedStatement ps = connection.prepareStatement(myExpSql)) {
                    ps.setInt(1, companyId);
                    ps.setInt(2, employeeId);
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            int expId = rs.getInt("ExplanationId");
                            String status = rs.getString("Status");
                            Date attDate = rs.getDate("AttendanceDate");
                            Timestamp ts = rs.getTimestamp("ReviewedAt") != null ? rs.getTimestamp("ReviewedAt") : rs.getTimestamp("CreatedAt");
                            String key = "att-exp-self:" + expId + ":" + status;
                            String statusVn = switch (status == null ? "" : status.toUpperCase()) {
                                case "APPROVED" -> "Đã được phê duyệt";
                                case "REJECTED" -> "Đã bị từ chối";
                                case "PENDING" -> "Đang chờ phê duyệt";
                                default -> status;
                            };
                            items.add(buildItem(
                                    key,
                                    "ATTENDANCE",
                                    "Giải trình điểm danh ngày " + (attDate != null ? attDate.toLocalDate().format(DATE_FMT) : ""),
                                    "Trạng thái đơn giải trình của bạn: " + statusVn + ".",
                                    formatLocalTimestamp(ts),
                                    personalAttUrl,
                                    readKeys.contains(key),
                                    ts != null ? ts.getTime() : 0L
                            ));
                        }
                    }
                }
            }
        }

        // B. For Manager / HR / Company Admin: Pending Attendance Explanations & Today's Attendance Summary
        if (isAdminOrHr || isManager) {
            if (hasTable(connection, "dbo.AttendanceExplanations")) {
                String pendingExpSql = isAdminOrHr
                        ? "SELECT COUNT(*) AS PendingCount, MAX(CreatedAt) AS LatestAt FROM dbo.AttendanceExplanations WHERE CompanyId = ? AND Status = 'PENDING'"
                        : """
                          SELECT COUNT(*) AS PendingCount, MAX(ae.CreatedAt) AS LatestAt
                          FROM dbo.AttendanceExplanations ae
                          JOIN dbo.Employees e ON e.EmployeeId = ae.EmployeeId AND e.CompanyId = ae.CompanyId
                          WHERE ae.CompanyId = ? AND ae.Status = 'PENDING' AND e.ManagerId = ?
                          """;
                try (PreparedStatement ps = connection.prepareStatement(pendingExpSql)) {
                    ps.setInt(1, companyId);
                    if (!isAdminOrHr) {
                        ps.setInt(2, employeeId != null ? employeeId : -1);
                    }
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next() && rs.getInt("PendingCount") > 0) {
                            int count = rs.getInt("PendingCount");
                            Timestamp latestTs = rs.getTimestamp("LatestAt");
                            String key = "att-exp-pending:" + companyId + ":" + count + ":" + (latestTs != null ? latestTs.getTime() : 0);
                            String url = isAdminOrHr ? "/attendance/company-attendance" : "/attendance/manager-attendance";
                            items.add(buildItem(
                                    key,
                                    "ATTENDANCE",
                                    "Có " + count + " đơn giải trình điểm danh chờ duyệt",
                                    "Nhân viên đã gửi giải trình chấm công mới cần bạn kiểm tra và phê duyệt.",
                                    formatLocalTimestamp(latestTs),
                                    url,
                                    readKeys.contains(key),
                                    latestTs != null ? latestTs.getTime() : System.currentTimeMillis()
                            ));
                        }
                    }
                }
            }

            String summarySql = isAdminOrHr
                    ? """
                      SELECT
                          (SELECT COUNT(*) FROM dbo.Employees WHERE CompanyId = ? AND Status = 'Active') AS TotalActive,
                          COUNT(CASE WHEN a.Status IN ('Present', 'Late', 'Early Leave') THEN 1 END) AS CheckedInCount,
                          COUNT(CASE WHEN a.Status = 'Late' THEN 1 END) AS LateCount
                      FROM dbo.Attendances a
                      WHERE a.CompanyId = ? AND a.AttendanceDate = ?
                      """
                    : """
                      SELECT
                          (SELECT COUNT(*) FROM dbo.Employees WHERE CompanyId = ? AND ManagerId = ? AND Status = 'Active') AS TotalActive,
                          COUNT(CASE WHEN a.Status IN ('Present', 'Late', 'Early Leave') THEN 1 END) AS CheckedInCount,
                          COUNT(CASE WHEN a.Status = 'Late' THEN 1 END) AS LateCount
                      FROM dbo.Attendances a
                      JOIN dbo.Employees e ON e.EmployeeId = a.EmployeeId AND e.CompanyId = a.CompanyId
                      WHERE a.CompanyId = ? AND e.ManagerId = ? AND a.AttendanceDate = ?
                      """;
            try (PreparedStatement ps = connection.prepareStatement(summarySql)) {
                if (isAdminOrHr) {
                    ps.setInt(1, companyId);
                    ps.setInt(2, companyId);
                    ps.setDate(3, Date.valueOf(todayVn));
                } else {
                    int mgrEmpId = employeeId != null ? employeeId : -1;
                    ps.setInt(1, companyId);
                    ps.setInt(2, mgrEmpId);
                    ps.setInt(3, companyId);
                    ps.setInt(4, mgrEmpId);
                    ps.setDate(5, Date.valueOf(todayVn));
                }
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        int total = rs.getInt("TotalActive");
                        int checkedIn = rs.getInt("CheckedInCount");
                        int late = rs.getInt("LateCount");
                        String key = "att-daily-summary:" + todayVn + ":" + checkedIn + ":" + late;
                        String url = isAdminOrHr ? "/attendance/company-attendance" : "/attendance/manager-attendance";
                        items.add(buildItem(
                                key,
                                "ATTENDANCE",
                                "Điểm danh nhân sự hôm nay (" + todayStr + ")",
                                "Đã chấm công: " + checkedIn + " / " + total + " nhân viên" + (late > 0 ? " (Đi muộn: " + late + ")." : "."),
                                "Hôm nay",
                                url,
                                readKeys.contains(key),
                                System.currentTimeMillis() - 600000
                        ));
                    }
                }
            }
        }
    }

    private void collectLeaveAndPayslipNotifications(
            Connection connection,
            int companyId,
            Integer employeeId,
            String roleCode,
            boolean isAdminOrHr,
            boolean isManager,
            Set<String> readKeys,
            List<Map<String, Object>> items
    ) throws SQLException {
        if (("COMPANY_ADMIN".equals(roleCode) || isManager) && hasTable(connection, "dbo.LeaveRequests")) {
            String leavePendingSql = """
                      SELECT COUNT(*) AS PendingCount, MAX(lr.CreatedAt) AS LatestAt
                      FROM dbo.LeaveRequests lr
                      JOIN dbo.Employees e ON e.EmployeeId=lr.EmployeeId AND e.CompanyId=lr.CompanyId
                      JOIN dbo.Users u ON u.UserId=e.UserId AND u.CompanyId=e.CompanyId
                      JOIN dbo.Roles r ON r.RoleId=u.RoleId
                      WHERE lr.CompanyId=? AND lr.Status='Pending'
                      """ + (isManager ? " AND r.RoleCode='EMPLOYEE' AND e.ManagerId=?" : " AND r.RoleCode='MANAGER'");
            try (PreparedStatement ps = connection.prepareStatement(leavePendingSql)) {
                ps.setInt(1, companyId);
                if (isManager) {
                    ps.setInt(2, employeeId != null ? employeeId : -1);
                }
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next() && rs.getInt("PendingCount") > 0) {
                        int count = rs.getInt("PendingCount");
                        Timestamp latestTs = rs.getTimestamp("LatestAt");
                        String key = "leave-pending:" + companyId + ":" + count + ":" + (latestTs != null ? latestTs.getTime() : 0);
                        items.add(buildItem(
                                key,
                                "LEAVE_PAYROLL",
                                "Có " + count + " đơn xin nghỉ phép đang chờ duyệt",
                                "Nhấn để xem chi tiết và phê duyệt đơn nghỉ phép của nhân sự.",
                                formatUtcTimestamp(latestTs),
                                "/leave/pending-approvals",
                                readKeys.contains(key),
                                latestTs != null ? latestTs.getTime() : System.currentTimeMillis()
                        ));
                    }
                }
            }
        }

        if (employeeId != null && employeeId > 0) {
            if (hasTable(connection, "dbo.LeaveRequests")) {
                String myLeaveSql = """
                        SELECT TOP 5 lr.LeaveRequestId, lr.StartDate, lr.EndDate, lr.Status, lr.ReviewedAt, lr.CreatedAt
                        FROM dbo.LeaveRequests lr
                        WHERE lr.CompanyId = ? AND lr.EmployeeId = ? AND lr.Status IN ('Approved', 'Rejected')
                        ORDER BY COALESCE(lr.ReviewedAt, lr.CreatedAt) DESC
                        """;
                try (PreparedStatement ps = connection.prepareStatement(myLeaveSql)) {
                    ps.setInt(1, companyId);
                    ps.setInt(2, employeeId);
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            int reqId = rs.getInt("LeaveRequestId");
                            String status = rs.getString("Status");
                            String typeName = "Đơn nghỉ phép #" + reqId;
                            Date start = rs.getDate("StartDate");
                            Date end = rs.getDate("EndDate");
                            Timestamp ts = rs.getTimestamp("ReviewedAt") != null ? rs.getTimestamp("ReviewedAt") : rs.getTimestamp("CreatedAt");
                            String key = "leave-self:" + reqId + ":" + status;
                            String statusVn = "Approved".equalsIgnoreCase(status) ? "Đã được duyệt" : "Đã bị từ chối";
                            items.add(buildItem(
                                    key,
                                    "LEAVE_PAYROLL",
                                    "Đơn xin nghỉ phép: " + statusVn,
                                    typeName + " (" + (start != null ? start.toLocalDate().format(DATE_FMT) : "")
                                            + " → " + (end != null ? end.toLocalDate().format(DATE_FMT) : "") + ")",
                                    formatUtcTimestamp(ts),
                                    "/leave/my-requests",
                                    readKeys.contains(key),
                                    ts != null ? ts.getTime() : 0L
                            ));
                        }
                    }
                }
            }

            if (hasTable(connection, "dbo.PayrollNotifications")) {
                String payslipSql = """
                        SELECT TOP 5 pn.NotificationId, pn.CreatedAt, pn.ReadAt, pp.PeriodName
                        FROM dbo.PayrollNotifications pn
                        JOIN dbo.Payslips ps ON ps.PayslipId = pn.PayslipId
                        JOIN dbo.PayrollPeriods pp ON pp.PayrollPeriodId = ps.PayrollPeriodId
                        WHERE pn.CompanyId = ? AND pn.EmployeeId = ?
                        ORDER BY pn.CreatedAt DESC
                        """;
                try (PreparedStatement ps = connection.prepareStatement(payslipSql)) {
                    ps.setInt(1, companyId);
                    ps.setInt(2, employeeId);
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            long notifId = rs.getLong("NotificationId");
                            String key = "payslip:" + notifId;
                            Timestamp createdTs = rs.getTimestamp("CreatedAt");
                            boolean isRead = rs.getTimestamp("ReadAt") != null || readKeys.contains(key);
                            items.add(buildItem(
                                    key,
                                    "LEAVE_PAYROLL",
                                    "Phiếu lương mới: " + rs.getString("PeriodName"),
                                    "Phiếu lương của bạn đã được phát hành. Nhấn để xem chi tiết.",
                                    formatUtcTimestamp(createdTs),
                                    "/payslips",
                                    isRead,
                                    createdTs != null ? createdTs.getTime() : 0L
                            ));
                        }
                    }
                }
            }
        }
    }

    private boolean isEmployeeInProfilePeriod(Connection connection, long periodId, int employeeId) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT 1 FROM dbo.ProfileUpdatePeriodEmployees WHERE PeriodId = ? AND EmployeeId = ?")) {
            ps.setLong(1, periodId);
            ps.setInt(2, employeeId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private boolean isEmployeeInDocPeriod(Connection connection, long periodId, int employeeId) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT 1 FROM dbo.DocumentUpdatePeriodEmployees WHERE DocumentPeriodId = ? AND EmployeeId = ?")) {
            ps.setLong(1, periodId);
            ps.setInt(2, employeeId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private boolean hasTable(Connection connection, String tableName) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("SELECT OBJECT_ID(?, 'U')")) {
            ps.setString(1, tableName);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getObject(1) != null;
            }
        }
    }

    private String translateAttendanceStatus(String status) {
        if (status == null) return "Đã ghi nhận";
        return switch (status) {
            case "Present" -> "Đúng giờ";
            case "Late" -> "Đi muộn";
            case "Early Leave" -> "Về sớm";
            case "On Leave" -> "Nghỉ phép";
            case "Absent" -> "Vắng mặt";
            default -> status;
        };
    }

    private String formatUtcTimestamp(Timestamp ts) {
        if (ts == null) return "";
        return ts.toLocalDateTime().atOffset(ZoneOffset.UTC).atZoneSameInstant(VN_ZONE).format(DATETIME_FMT);
    }

    private String formatLocalTimestamp(Timestamp ts) {
        if (ts == null) return "";
        return ts.toLocalDateTime().format(DATETIME_FMT);
    }

    private Map<String, Object> buildItem(
            String key,
            String category,
            String title,
            String message,
            String timeLabel,
            String link,
            boolean read,
            long sortEpoch
    ) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("key", key);
        m.put("category", category);
        m.put("title", title);
        m.put("message", message);
        m.put("timeLabel", timeLabel);
        m.put("link", link);
        m.put("read", read);
        m.put("sortEpoch", sortEpoch);
        return m;
    }
}

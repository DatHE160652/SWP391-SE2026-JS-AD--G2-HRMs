package com.hrms.dao;

import com.hrms.model.AuditLog;
import com.hrms.model.ProfileUpdatePeriod;
import com.hrms.service.FieldValidationException;
import com.hrms.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class ProfileUpdateDAO {
    private final String periodType;

    public record ProfileFieldDef(String fieldKey, String columnName, String fieldLabel, boolean defaultChecked) {}

    public static final List<ProfileFieldDef> PROFILE_FIELD_CATALOG = List.of(
            new ProfileFieldDef("email", "Email", "Email liên hệ", true),
            new ProfileFieldDef("phone", "Phone", "Số điện thoại", true),
            new ProfileFieldDef("identityNumber", "IdentityNumber", "Số CCCD/CMND", true),
            new ProfileFieldDef("identityIssuedDate", "IdentityIssuedDate", "Ngày cấp CCCD/CMND", true),
            new ProfileFieldDef("identityIssuedPlace", "IdentityIssuedPlace", "Nơi cấp CCCD/CMND", true),
            new ProfileFieldDef("permanentAddress", "PermanentAddress", "Địa chỉ thường trú", true),
            new ProfileFieldDef("hometown", "Hometown", "Quê quán", true)
    );

    public static final Map<String, ProfileFieldDef> FIELD_BY_KEY;
    public static final List<String> DEFAULT_FIELD_KEYS;

    static {
        Map<String, ProfileFieldDef> map = new LinkedHashMap<>();
        List<String> defaults = new ArrayList<>();
        for (ProfileFieldDef def : PROFILE_FIELD_CATALOG) {
            map.put(def.fieldKey(), def);
            if (def.defaultChecked()) {
                defaults.add(def.fieldKey());
            }
        }
        FIELD_BY_KEY = Map.copyOf(map);
        DEFAULT_FIELD_KEYS = List.copyOf(defaults);
    }

    public record ActiveEmployeeProfileContext(
            long periodId,
            String title,
            LocalDateTime startsAt,
            LocalDateTime endsAt,
            List<String> allowedFieldKeys,
            List<String> allowedFieldLabels,
            List<Map<String, Object>> activePeriods
    ) {}

    private static final DateTimeFormatter DISPLAY_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public ProfileUpdateDAO() {
        this("PROFILE");
    }

    public ProfileUpdateDAO(String periodType) {
        if (!Set.of("PROFILE", "DOCUMENT").contains(periodType)) {
            throw new IllegalArgumentException("Loại đợt cập nhật không hợp lệ.");
        }
        this.periodType = periodType;
    }

    public synchronized void ensureSchema(Connection connection, int companyId) throws SQLException {
        String ddl = """
                IF COL_LENGTH('dbo.ProfileUpdatePeriods', 'ScopeType') IS NULL
                BEGIN
                    ALTER TABLE dbo.ProfileUpdatePeriods
                        ADD ScopeType VARCHAR(20) NOT NULL CONSTRAINT DF_ProfileUpdatePeriods_ScopeType DEFAULT ('ALL');
                END;

                IF OBJECT_ID('dbo.ProfileUpdatePeriodFields', 'U') IS NULL
                BEGIN
                    CREATE TABLE dbo.ProfileUpdatePeriodFields (
                        PeriodId BIGINT NOT NULL,
                        FieldKey VARCHAR(50) NOT NULL,
                        CONSTRAINT PK_ProfileUpdatePeriodFields PRIMARY KEY (PeriodId, FieldKey),
                        CONSTRAINT FK_ProfileUpdatePeriodFields_Period FOREIGN KEY (PeriodId)
                            REFERENCES dbo.ProfileUpdatePeriods(PeriodId) ON DELETE CASCADE
                    );
                END;

                IF OBJECT_ID('dbo.ProfileUpdatePeriodEmployees', 'U') IS NULL
                BEGIN
                    CREATE TABLE dbo.ProfileUpdatePeriodEmployees (
                        PeriodId BIGINT NOT NULL,
                        EmployeeId INT NOT NULL,
                        CONSTRAINT PK_ProfileUpdatePeriodEmployees PRIMARY KEY (PeriodId, EmployeeId),
                        CONSTRAINT FK_ProfileUpdatePeriodEmployees_Period FOREIGN KEY (PeriodId)
                            REFERENCES dbo.ProfileUpdatePeriods(PeriodId) ON DELETE CASCADE,
                        CONSTRAINT FK_ProfileUpdatePeriodEmployees_Employee FOREIGN KEY (EmployeeId)
                            REFERENCES dbo.Employees(EmployeeId) ON DELETE CASCADE
                    );
                END;

                IF OBJECT_ID('dbo.ProfileUpdatePeriodLogs', 'U') IS NULL
                BEGIN
                    CREATE TABLE dbo.ProfileUpdatePeriodLogs (
                        LogId BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
                        PeriodId BIGINT NOT NULL,
                        CompanyId INT NOT NULL,
                        EmployeeId INT NOT NULL,
                        FieldKey VARCHAR(50) NOT NULL,
                        FieldLabel NVARCHAR(100) NOT NULL,
                        OldValue NVARCHAR(500) NULL,
                        NewValue NVARCHAR(500) NULL,
                        UpdatedAt DATETIME2(7) NOT NULL CONSTRAINT DF_ProfileUpdatePeriodLogs_UpdatedAt DEFAULT (SYSUTCDATETIME()),
                        CONSTRAINT FK_ProfileUpdatePeriodLogs_Period FOREIGN KEY (PeriodId)
                            REFERENCES dbo.ProfileUpdatePeriods(PeriodId) ON DELETE CASCADE,
                        CONSTRAINT FK_ProfileUpdatePeriodLogs_Employee FOREIGN KEY (EmployeeId)
                            REFERENCES dbo.Employees(EmployeeId) ON DELETE CASCADE
                    );
                    CREATE INDEX IX_ProfileUpdatePeriodLogs_Period
                        ON dbo.ProfileUpdatePeriodLogs(CompanyId, PeriodId, EmployeeId, UpdatedAt DESC);
                END;
                """;
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(ddl);
        }

        if (companyId <= 0) {
            return;
        }

        // Seed allowed fields for any existing PROFILE periods that don't have rows in ProfileUpdatePeriodFields yet
        List< ProfileUpdatePeriod> unseededPeriods = new ArrayList<>();
        String findUnseeded = """
                SELECT p.PeriodId, p.CompanyId, p.Title, p.StartsAt, p.EndsAt, p.ClosedAt, p.PeriodType
                FROM dbo.ProfileUpdatePeriods p
                WHERE p.CompanyId = ? AND p.PeriodType = 'PROFILE'
                  AND NOT EXISTS (
                      SELECT 1 FROM dbo.ProfileUpdatePeriodFields pf WHERE pf.PeriodId = p.PeriodId
                  )
                """;
        try (PreparedStatement ps = connection.prepareStatement(findUnseeded)) {
            ps.setInt(1, companyId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    unseededPeriods.add(mapPeriod(rs));
                }
            }
        }
        if (!unseededPeriods.isEmpty()) {
            try (PreparedStatement ins = connection.prepareStatement(
                    "INSERT INTO dbo.ProfileUpdatePeriodFields (PeriodId, FieldKey) VALUES (?, ?)")) {
                for (ProfileUpdatePeriod p : unseededPeriods) {
                    String lowerTitle = p.title() == null ? "" : p.title().toLowerCase();
                    List<String> keysToSeed;
                    if (lowerTitle.contains("email") && !lowerTitle.contains("thông tin") && !lowerTitle.contains("hồ sơ")) {
                        keysToSeed = List.of("email");
                    } else if (lowerTitle.contains("điện thoại") || lowerTitle.contains("sđt")) {
                        keysToSeed = List.of("phone");
                    } else {
                        keysToSeed = DEFAULT_FIELD_KEYS;
                    }
                    for (String fk : keysToSeed) {
                        ins.setLong(1, p.periodId());
                        ins.setString(2, fk);
                        ins.addBatch();
                    }
                }
                ins.executeBatch();
            }
        }

        // Backfill ProfileUpdatePeriodLogs from AuditLogs (UPDATE_SELF_PROFILE) if no logs exist for this company yet
        boolean hasLogs = false;
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT TOP 1 1 FROM dbo.ProfileUpdatePeriodLogs WHERE CompanyId = ?")) {
            ps.setInt(1, companyId);
            try (ResultSet rs = ps.executeQuery()) {
                hasLogs = rs.next();
            }
        }
        if (!hasLogs) {
            backfillLogsFromAudit(connection, companyId);
        }
    }

    private void backfillLogsFromAudit(Connection connection, int companyId) throws SQLException {
        String sql = """
                SELECT a.RecordId, a.OldValues, a.NewValues, a.CreatedAt,
                       (SELECT TOP 1 p.PeriodId
                        FROM dbo.ProfileUpdatePeriods p
                        WHERE p.CompanyId = a.CompanyId AND p.PeriodType = 'PROFILE'
                          AND a.CreatedAt >= p.StartsAt AND a.CreatedAt <= DATEADD(MINUTE, 5, p.EndsAt)
                        ORDER BY p.StartsAt DESC) AS MatchedPeriodId
                FROM dbo.AuditLogs a
                WHERE a.CompanyId = ? AND a.[Action] = 'UPDATE_SELF_PROFILE' AND a.TableName = 'Employees'
                ORDER BY a.CreatedAt ASC
                """;
        Map<String, String> labelToKey = new LinkedHashMap<>();
        for (ProfileFieldDef def : PROFILE_FIELD_CATALOG) {
            labelToKey.put(def.fieldLabel().toLowerCase(), def.fieldKey());
        }
        labelToKey.put("email", "email");
        labelToKey.put("điện thoại", "phone");
        labelToKey.put("ngày cấp căn cước", "identityIssuedDate");
        labelToKey.put("nơi cấp căn cước", "identityIssuedPlace");

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, companyId);
            try (ResultSet rs = ps.executeQuery();
                 PreparedStatement ins = connection.prepareStatement("""
                         INSERT INTO dbo.ProfileUpdatePeriodLogs
                             (PeriodId, CompanyId, EmployeeId, FieldKey, FieldLabel, OldValue, NewValue, UpdatedAt)
                         VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                         """)) {
                boolean anyBatch = false;
                while (rs.next()) {
                    long periodId = rs.getLong("MatchedPeriodId");
                    if (rs.wasNull() || periodId <= 0) {
                        continue;
                    }
                    int empId;
                    try {
                        empId = Integer.parseInt(rs.getString("RecordId"));
                    } catch (NumberFormatException e) {
                        continue;
                    }
                    Timestamp createdAt = rs.getTimestamp("CreatedAt");
                    Map<String, String> oldMap = parseAuditLines(rs.getString("OldValues"));
                    Map<String, String> newMap = parseAuditLines(rs.getString("NewValues"));
                    for (Map.Entry<String, String> entry : newMap.entrySet()) {
                        String rawLabel = entry.getKey();
                        if (rawLabel.equalsIgnoreCase("Đợt cập nhật")) {
                            continue;
                        }
                        String fieldKey = labelToKey.getOrDefault(rawLabel.toLowerCase(), "other");
                        ProfileFieldDef def = FIELD_BY_KEY.get(fieldKey);
                        String canonicalLabel = def != null ? def.fieldLabel() : rawLabel;
                        String oldVal = oldMap.getOrDefault(rawLabel, "Chưa cập nhật");
                        String newVal = entry.getValue();
                        ins.setLong(1, periodId);
                        ins.setInt(2, companyId);
                        ins.setInt(3, empId);
                        ins.setString(4, fieldKey);
                        ins.setString(5, canonicalLabel);
                        ins.setString(6, oldVal);
                        ins.setString(7, newVal);
                        ins.setTimestamp(8, createdAt);
                        ins.addBatch();
                        anyBatch = true;
                    }
                }
                if (anyBatch) {
                    ins.executeBatch();
                }
            }
        }
    }

    private Map<String, String> parseAuditLines(String text) {
        Map<String, String> map = new LinkedHashMap<>();
        if (text == null || text.isBlank()) {
            return map;
        }
        for (String line : text.split("\\r?\\n")) {
            int idx = line.indexOf(':');
            if (idx > 0) {
                String k = line.substring(0, idx).trim();
                String v = line.substring(idx + 1).trim();
                map.put(k, v);
            }
        }
        return map;
    }

    public List<Map<String, Object>> getFieldCatalogMaps() {
        List<Map<String, Object>> list = new ArrayList<>();
        for (ProfileFieldDef def : PROFILE_FIELD_CATALOG) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("fieldKey", def.fieldKey());
            m.put("fieldLabel", def.fieldLabel());
            m.put("defaultChecked", def.defaultChecked());
            list.add(m);
        }
        return list;
    }

    public List<ProfileUpdatePeriod> getPeriods(int companyId) throws SQLException {
        try (Connection connection = DBConnection.getConnection()) {
            ensureSchema(connection, companyId);
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT TOP 100 * FROM ProfileUpdatePeriods WHERE CompanyId = ? AND PeriodType=? ORDER BY PeriodId DESC")) {
                statement.setInt(1, companyId);
                statement.setString(2, periodType);
                try (ResultSet resultSet = statement.executeQuery()) {
                    List<ProfileUpdatePeriod> periods = new ArrayList<>();
                    while (resultSet.next()) {
                        periods.add(mapPeriod(resultSet));
                    }
                    return periods;
                }
            }
        }
    }

    public List<Map<String, Object>> getDetailedPeriods(int companyId) throws SQLException {
        try (Connection connection = DBConnection.getConnection()) {
            ensureSchema(connection, companyId);
            int totalActiveCompanyEmployees = countActiveCompanyEmployees(connection, companyId);
            List<Map<String, Object>> periods = new ArrayList<>();
            String sql = """
                    SELECT TOP 100
                        p.PeriodId, p.CompanyId, p.Title, p.StartsAt, p.EndsAt,
                        ISNULL(p.ScopeType, 'ALL') AS ScopeType, p.ClosedAt,
                        (SELECT COUNT(DISTINCT l.EmployeeId)
                         FROM dbo.ProfileUpdatePeriodLogs l
                         WHERE l.CompanyId = p.CompanyId AND l.PeriodId = p.PeriodId) AS UpdatedEmployeeCount,
                        (SELECT COUNT(*)
                         FROM dbo.ProfileUpdatePeriodLogs l
                         WHERE l.CompanyId = p.CompanyId AND l.PeriodId = p.PeriodId) AS UpdatedFieldCount
                    FROM dbo.ProfileUpdatePeriods p
                    WHERE p.CompanyId = ? AND p.PeriodType = 'PROFILE'
                    ORDER BY p.PeriodId DESC
                    """;
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setInt(1, companyId);
                try (ResultSet rs = ps.executeQuery()) {
                    LocalDateTime nowUtc = LocalDateTime.now(ZoneOffset.UTC);
                    while (rs.next()) {
                        long periodId = rs.getLong("PeriodId");
                        LocalDateTime startsAt = rs.getTimestamp("StartsAt").toLocalDateTime();
                        LocalDateTime endsAt = rs.getTimestamp("EndsAt").toLocalDateTime();
                        Timestamp closedTs = rs.getTimestamp("ClosedAt");
                        LocalDateTime closedAt = closedTs == null ? null : closedTs.toLocalDateTime();
                        String scopeType = rs.getString("ScopeType");
                        boolean open = closedAt == null && !nowUtc.isBefore(startsAt) && nowUtc.isBefore(endsAt);

                        Map<String, Object> item = new LinkedHashMap<>();
                        item.put("periodId", periodId);
                        item.put("companyId", rs.getInt("CompanyId"));
                        item.put("title", rs.getString("Title"));
                        item.put("periodType", "PROFILE");
                        item.put("startsAt", startsAt.atOffset(ZoneOffset.UTC).toString());
                        item.put("endsAt", endsAt.atOffset(ZoneOffset.UTC).toString());
                        item.put("scopeType", scopeType);
                        item.put("closed", closedAt != null);
                        item.put("open", open);
                        item.put("updatedEmployeeCount", rs.getInt("UpdatedEmployeeCount"));
                        item.put("updatedFieldCount", rs.getInt("UpdatedFieldCount"));
                        item.put("allowedFields", getPeriodFields(connection, periodId));

                        if ("SELECTED".equalsIgnoreCase(scopeType)) {
                            List<Map<String, Object>> targets = getPeriodTargetEmployees(connection, companyId, periodId);
                            item.put("targetEmployeeCount", targets.size());
                            item.put("targetEmployees", targets);
                        } else {
                            item.put("targetEmployeeCount", totalActiveCompanyEmployees);
                            item.put("targetEmployees", List.of());
                        }
                        periods.add(item);
                    }
                }
            }
            return periods;
        }
    }

    public Map<String, Object> getPeriodDetail(int companyId, long periodId) throws SQLException {
        try (Connection connection = DBConnection.getConnection()) {
            ensureSchema(connection, companyId);
            Map<String, Object> detail = new LinkedHashMap<>();
            String sql = """
                    SELECT p.PeriodId, p.CompanyId, p.Title, p.StartsAt, p.EndsAt,
                           ISNULL(p.ScopeType, 'ALL') AS ScopeType, p.ClosedAt
                    FROM dbo.ProfileUpdatePeriods p
                    WHERE p.CompanyId = ? AND p.PeriodId = ? AND p.PeriodType = 'PROFILE'
                    """;
            String scopeType;
            LocalDateTime startsAt;
            LocalDateTime endsAt;
            LocalDateTime closedAt;
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setInt(1, companyId);
                ps.setLong(2, periodId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        throw new IllegalArgumentException("Không tìm thấy đợt cập nhật hồ sơ.");
                    }
                    startsAt = rs.getTimestamp("StartsAt").toLocalDateTime();
                    endsAt = rs.getTimestamp("EndsAt").toLocalDateTime();
                    Timestamp closedTs = rs.getTimestamp("ClosedAt");
                    closedAt = closedTs == null ? null : closedTs.toLocalDateTime();
                    scopeType = rs.getString("ScopeType");
                    LocalDateTime nowUtc = LocalDateTime.now(ZoneOffset.UTC);

                    detail.put("periodId", periodId);
                    detail.put("title", rs.getString("Title"));
                    detail.put("startsAt", startsAt.atOffset(ZoneOffset.UTC).toString());
                    detail.put("endsAt", endsAt.atOffset(ZoneOffset.UTC).toString());
                    detail.put("scopeType", scopeType);
                    detail.put("closed", closedAt != null);
                    detail.put("open", closedAt == null && !nowUtc.isBefore(startsAt) && nowUtc.isBefore(endsAt));
                }
            }

            List<Map<String, Object>> allowedFields = getPeriodFields(connection, periodId);
            detail.put("allowedFields", allowedFields);
            List<String> requiredFieldLabels = allowedFields.stream()
                    .map(m -> String.valueOf(m.get("fieldLabel")))
                    .toList();

            List<Map<String, Object>> targetEmployees;
            if ("SELECTED".equalsIgnoreCase(scopeType)) {
                targetEmployees = getPeriodTargetEmployees(connection, companyId, periodId);
            } else {
                targetEmployees = new DocumentUpdatePeriodDAO().getActiveCompanyEmployees(companyId);
            }

            List<Map<String, Object>> fieldChanges = new ArrayList<>();
            Map<Integer, List<Map<String, Object>>> changesByEmployee = new LinkedHashMap<>();
            String logSql = """
                    SELECT l.LogId, l.EmployeeId, e.EmployeeCode, e.FullName,
                           d.DepartmentName, jt.JobTitleName AS PositionTitle,
                           l.FieldKey, l.FieldLabel, l.OldValue, l.NewValue, l.UpdatedAt
                    FROM dbo.ProfileUpdatePeriodLogs l
                    JOIN dbo.Employees e ON e.EmployeeId = l.EmployeeId AND e.CompanyId = l.CompanyId
                    LEFT JOIN dbo.Departments d ON d.DepartmentId = e.DepartmentId AND d.CompanyId = e.CompanyId
                    LEFT JOIN dbo.Positions pos ON pos.PositionId = e.PositionId AND pos.CompanyId = e.CompanyId
                    LEFT JOIN dbo.JobTitles jt ON jt.JobTitleId = pos.JobTitleId AND jt.CompanyId = pos.CompanyId
                    WHERE l.CompanyId = ? AND l.PeriodId = ?
                    ORDER BY l.UpdatedAt DESC, l.LogId DESC
                    """;
            try (PreparedStatement ps = connection.prepareStatement(logSql)) {
                ps.setInt(1, companyId);
                ps.setLong(2, periodId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        int empId = rs.getInt("EmployeeId");
                        Timestamp updatedTs = rs.getTimestamp("UpdatedAt");
                        String formattedDate = updatedTs != null
                                ? updatedTs.toLocalDateTime().atOffset(ZoneOffset.UTC)
                                .atZoneSameInstant( java.time.ZoneId.of("Asia/Ho_Chi_Minh")).format(DISPLAY_FMT)
                                : "";
                        Map<String, Object> change = new LinkedHashMap<>();
                        change.put("logId", rs.getLong("LogId"));
                        change.put("employeeId", empId);
                        change.put("employeeCode", rs.getString("EmployeeCode"));
                        change.put("fullName", rs.getString("FullName"));
                        change.put("departmentName", rs.getString("DepartmentName"));
                        change.put("positionTitle", rs.getString("PositionTitle"));
                        change.put("fieldKey", rs.getString("FieldKey"));
                        change.put("fieldLabel", rs.getString("FieldLabel"));
                        change.put("oldValue", formatFieldValue(rs.getString("FieldKey"), rs.getString("OldValue")));
                        change.put("newValue", formatFieldValue(rs.getString("FieldKey"), rs.getString("NewValue")));
                        change.put("updatedAt", formattedDate);
                        fieldChanges.add(change);
                        changesByEmployee.computeIfAbsent(empId, k -> new ArrayList<>()).add(change);
                    }
                }
            }

            Map<Integer, Map<String, Object>> targetMap = new LinkedHashMap<>();
            for (Map<String, Object> emp : targetEmployees) {
                targetMap.put((Integer) emp.get("employeeId"), new LinkedHashMap<>(emp));
            }
            for (Map<String, Object> change : fieldChanges) {
                int empId = (Integer) change.get("employeeId");
                targetMap.computeIfAbsent(empId, id -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("employeeId", id);
                    m.put("employeeCode", change.get("employeeCode"));
                    m.put("fullName", change.get("fullName"));
                    m.put("departmentName", change.get("departmentName"));
                    m.put("positionTitle", change.get("positionTitle"));
                    return m;
                });
            }

            List<Map<String, Object>> employeeStatuses = new ArrayList<>();
            int updatedEmployeeCount = 0;
            for (Map<String, Object> emp : targetMap.values()) {
                int empId = (Integer) emp.get("employeeId");
                List<Map<String, Object>> empChanges = changesByEmployee.getOrDefault(empId, List.of());
                if (!empChanges.isEmpty()) {
                    updatedEmployeeCount++;
                }
                Set<String> updatedLabels = new LinkedHashSet<>();
                for (Map<String, Object> c : empChanges) {
                    if (c.get("fieldLabel") != null) {
                        updatedLabels.add(String.valueOf(c.get("fieldLabel")));
                    }
                }
                List<String> missingLabels = requiredFieldLabels.stream()
                        .filter(req -> updatedLabels.stream().noneMatch(u -> u.equalsIgnoreCase(req)))
                        .toList();

                Map<String, Object> row = new LinkedHashMap<>(emp);
                row.put("updatedCount", empChanges.size());
                row.put("updatedFields", new ArrayList<>(updatedLabels));
                row.put("missingFields", missingLabels);
                row.put("lastUpdatedAt", empChanges.isEmpty() ? null : empChanges.get(0).get("updatedAt"));
                row.put("changes", empChanges);
                row.put("completionStatus", empChanges.isEmpty() ? "PENDING" : "UPDATED");
                employeeStatuses.add(row);
            }

            detail.put("targetEmployeeCount", targetMap.size());
            detail.put("updatedEmployeeCount", updatedEmployeeCount);
            detail.put("updatedFieldCount", fieldChanges.size());
            detail.put("fieldChanges", fieldChanges);
            detail.put("employeeStatuses", employeeStatuses);
            return detail;
        }
    }

    private String formatFieldValue(String fieldKey, String val) {
        if (val == null || val.isBlank() || "null".equalsIgnoreCase(val)) {
            return "Chưa cập nhật";
        }
        if ("gender".equals(fieldKey)) {
            return switch (val) {
                case "Male" -> "Nam";
                case "Female" -> "Nữ";
                case "Other" -> "Khác";
                default -> val;
            };
        }
        return val;
    }

    private int countActiveCompanyEmployees(Connection connection, int companyId) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT COUNT(*) FROM dbo.Employees WHERE CompanyId = ? AND Status = 'Active'")) {
            ps.setInt(1, companyId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    private List<Map<String, Object>> getPeriodFields(Connection connection, long periodId) throws SQLException {
        List<Map<String, Object>> fields = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT FieldKey FROM dbo.ProfileUpdatePeriodFields WHERE PeriodId = ?")) {
            ps.setLong(1, periodId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String key = rs.getString("FieldKey");
                    ProfileFieldDef def = FIELD_BY_KEY.get(key);
                    if (def != null) {
                        Map<String, Object> m = new LinkedHashMap<>();
                        m.put("fieldKey", def.fieldKey());
                        m.put("fieldLabel", def.fieldLabel());
                        fields.add(m);
                    }
                }
            }
        }
        if (fields.isEmpty()) {
            for (String key : DEFAULT_FIELD_KEYS) {
                ProfileFieldDef def = FIELD_BY_KEY.get(key);
                if (def != null) {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("fieldKey", def.fieldKey());
                    m.put("fieldLabel", def.fieldLabel());
                    fields.add(m);
                }
            }
        }
        return fields;
    }

    private List<Map<String, Object>> getPeriodTargetEmployees(Connection connection, int companyId, long periodId) throws SQLException {
        List<Map<String, Object>> list = new ArrayList<>();
        String sql = """
                SELECT e.EmployeeId, e.EmployeeCode, e.FullName, d.DepartmentName, jt.JobTitleName AS PositionTitle
                FROM dbo.ProfileUpdatePeriodEmployees pe
                JOIN dbo.Employees e ON e.EmployeeId = pe.EmployeeId AND e.CompanyId = ?
                LEFT JOIN dbo.Departments d ON d.DepartmentId = e.DepartmentId AND d.CompanyId = e.CompanyId
                LEFT JOIN dbo.Positions p ON p.PositionId = e.PositionId AND p.CompanyId = e.CompanyId
                LEFT JOIN dbo.JobTitles jt ON jt.JobTitleId = p.JobTitleId AND jt.CompanyId = p.CompanyId
                WHERE pe.PeriodId = ?
                ORDER BY e.FullName ASC, e.EmployeeId ASC
                """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, companyId);
            ps.setLong(2, periodId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> emp = new LinkedHashMap<>();
                    emp.put("employeeId", rs.getInt("EmployeeId"));
                    emp.put("employeeCode", rs.getString("EmployeeCode"));
                    emp.put("fullName", rs.getString("FullName"));
                    emp.put("departmentName", rs.getString("DepartmentName"));
                    emp.put("positionTitle", rs.getString("PositionTitle"));
                    list.add(emp);
                }
            }
        }
        return list;
    }

    public ProfileUpdatePeriod getAvailablePeriod(int companyId) throws SQLException {
        try (Connection connection = DBConnection.getConnection()) {
            ensureSchema(connection, companyId);
            return findAvailablePeriod(connection, companyId, false);
        }
    }

    public ActiveEmployeeProfileContext getActiveContextForEmployee(int companyId, int employeeId) throws SQLException {
        try (Connection connection = DBConnection.getConnection()) {
            ensureSchema(connection, companyId);
            return findActiveContextForEmployee(connection, companyId, employeeId, false);
        }
    }

    public ActiveEmployeeProfileContext findActiveContextForEmployee(Connection connection, int companyId, int employeeId, boolean lock) throws SQLException {
        String sql = "SELECT p.PeriodId, p.Title, p.StartsAt, p.EndsAt, ISNULL(p.ScopeType, 'ALL') AS ScopeType "
                + "FROM dbo.ProfileUpdatePeriods p " + (lock ? "WITH (UPDLOCK, HOLDLOCK) " : "")
                + "WHERE p.CompanyId = ? AND p.PeriodType = 'PROFILE' AND p.ClosedAt IS NULL "
                + "AND p.StartsAt <= SYSUTCDATETIME() AND p.EndsAt > SYSUTCDATETIME() "
                + "AND (ISNULL(p.ScopeType, 'ALL') = 'ALL' OR EXISTS ("
                + "    SELECT 1 FROM dbo.ProfileUpdatePeriodEmployees pe "
                + "    WHERE pe.PeriodId = p.PeriodId AND pe.EmployeeId = ?"
                + ")) "
                + "ORDER BY CASE WHEN ISNULL(p.ScopeType, 'ALL') = 'SELECTED' THEN 0 ELSE 1 END, p.StartsAt DESC";
        List<Map<String, Object>> activePeriods = new ArrayList<>();
        Set<String> allowedKeys = new LinkedHashSet<>();
        long primaryPeriodId = 0;
        String primaryTitle = null;
        LocalDateTime primaryStartsAt = null;
        LocalDateTime maxEndsAt = null;

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, companyId);
            ps.setInt(2, employeeId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    long pid = rs.getLong("PeriodId");
                    String title = rs.getString("Title");
                    LocalDateTime startsAt = rs.getTimestamp("StartsAt").toLocalDateTime();
                    LocalDateTime endsAt = rs.getTimestamp("EndsAt").toLocalDateTime();
                    String scopeType = rs.getString("ScopeType");
                    List<Map<String, Object>> pFields = getPeriodFields(connection, pid);
                    List<String> fieldKeys = new ArrayList<>();
                    List<String> fieldLabels = new ArrayList<>();
                    for (Map<String, Object> f : pFields) {
                        String fk = String.valueOf(f.get("fieldKey"));
                        String fl = String.valueOf(f.get("fieldLabel"));
                        fieldKeys.add(fk);
                        fieldLabels.add(fl);
                        allowedKeys.add(fk);
                    }
                    if (primaryPeriodId == 0) {
                        primaryPeriodId = pid;
                        primaryTitle = title;
                        primaryStartsAt = startsAt;
                    }
                    if (maxEndsAt == null || endsAt.isAfter(maxEndsAt)) {
                        maxEndsAt = endsAt;
                    }
                    Map<String, Object> pm = new LinkedHashMap<>();
                    pm.put("periodId", pid);
                    pm.put("title", title);
                    pm.put("scopeType", scopeType);
                    pm.put("startsAt", startsAt.atOffset(ZoneOffset.UTC).toString());
                    pm.put("endsAt", endsAt.atOffset(ZoneOffset.UTC).toString());
                    pm.put("allowedFieldKeys", fieldKeys);
                    pm.put("allowedFieldLabels", fieldLabels);
                    activePeriods.add(pm);
                }
            }
        }
        if (primaryPeriodId == 0) {
            return null;
        }
        List<String> orderedKeys = new ArrayList<>();
        List<String> orderedLabels = new ArrayList<>();
        for (ProfileFieldDef def : PROFILE_FIELD_CATALOG) {
            if (allowedKeys.contains(def.fieldKey())) {
                orderedKeys.add(def.fieldKey());
                orderedLabels.add(def.fieldLabel());
            }
        }
        return new ActiveEmployeeProfileContext(
                primaryPeriodId,
                primaryTitle,
                primaryStartsAt,
                maxEndsAt,
                orderedKeys,
                orderedLabels,
                activePeriods
        );
    }

    public ProfileUpdatePeriod requireAvailablePeriod(Connection connection, int companyId) throws SQLException {
        OrganizationTransaction.lockCompany(connection, companyId);
        ProfileUpdatePeriod period = findAvailablePeriod(connection, companyId, true);
        if (period == null) {
            throw new SecurityException("Không có đợt cập nhật tài liệu đang mở. Bạn chưa được thay đổi tài liệu.");
        }
        return period;
    }

    public long create(int companyId, int userId, String title, LocalDateTime startsAt, LocalDateTime endsAt,
                       String ipAddress) throws SQLException {
        return create(companyId, userId, title, startsAt, endsAt, "ALL", DEFAULT_FIELD_KEYS, List.of(), ipAddress);
    }

    public long create(int companyId, int userId, String title, LocalDateTime startsAt, LocalDateTime endsAt,
                       String scopeType, List<String> fieldKeys, List<Integer> employeeIds, String ipAddress) throws SQLException {
        return transaction(connection -> {
            ensureSchema(connection, companyId);
            return create(connection, companyId, userId, title, startsAt, endsAt, scopeType, fieldKeys, employeeIds, ipAddress);
        });
    }

    long create(Connection connection, int companyId, int userId, String title, LocalDateTime startsAt,
                LocalDateTime endsAt, String ipAddress) throws SQLException {
        return create(connection, companyId, userId, title, startsAt, endsAt, "ALL", DEFAULT_FIELD_KEYS, List.of(), ipAddress);
    }

    long create(Connection connection, int companyId, int userId, String title, LocalDateTime startsAt,
                LocalDateTime endsAt, String scopeType, List<String> fieldKeys, List<Integer> employeeIds,
                String ipAddress) throws SQLException {
        requireAdministrator(connection, companyId, userId);
        String normalizedScope = (scopeType == null || scopeType.isBlank()) ? "ALL" : scopeType.trim().toUpperCase();
        if (!Set.of("ALL", "SELECTED").contains(normalizedScope)) {
            throw new FieldValidationException("scopeType", "Phạm vi áp dụng không hợp lệ.");
        }
        List<String> validFieldKeys = new ArrayList<>();
        if (fieldKeys != null) {
            for (String key : fieldKeys) {
                if (key != null && FIELD_BY_KEY.containsKey(key.trim())) {
                    String trimmed = key.trim();
                    if (!validFieldKeys.contains(trimmed)) {
                        validFieldKeys.add(trimmed);
                    }
                }
            }
        }
        if (validFieldKeys.isEmpty()) {
            throw new FieldValidationException("allowedFields", "Vui lòng chọn ít nhất 1 loại thông tin hồ sơ cần cập nhật.");
        }
        Set<Integer> uniqueEmpIds = new LinkedHashSet<>();
        if ("SELECTED".equals(normalizedScope)) {
            if (employeeIds != null) {
                for (Integer id : employeeIds) {
                    if (id != null && id > 0) {
                        uniqueEmpIds.add(id);
                    }
                }
            }
            if (uniqueEmpIds.isEmpty()) {
                throw new FieldValidationException("employeeIds", "Vui lòng chọn ít nhất 1 nhân viên cho đợt cập nhật hồ sơ cá nhân.");
            }
        }

        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT CompanyId FROM Companies WITH (UPDLOCK, HOLDLOCK) WHERE CompanyId = ?")) {
            statement.setInt(1, companyId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    throw new SecurityException("Công ty không tồn tại.");
                }
            }
        }
        if ("ALL".equals(normalizedScope)) {
            try (PreparedStatement statement = connection.prepareStatement("""
                    SELECT PeriodId FROM ProfileUpdatePeriods WITH (UPDLOCK, HOLDLOCK)
                    WHERE CompanyId = ? AND ClosedAt IS NULL AND StartsAt < ? AND EndsAt > ?
                      AND PeriodType = ? AND ISNULL(ScopeType, 'ALL') = 'ALL'
                    """)) {
                statement.setInt(1, companyId);
                statement.setTimestamp(2, Timestamp.valueOf(endsAt));
                statement.setTimestamp(3, Timestamp.valueOf(startsAt));
                statement.setString(4, periodType);
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        throw new FieldValidationException("startsAt", "Thời gian này trùng với một đợt cập nhật toàn công ty chưa đóng.");
                    }
                }
            }
        }

        boolean hasScopeColumn = false;
        try (PreparedStatement checkCol = connection.prepareStatement(
                "SELECT COL_LENGTH('dbo.ProfileUpdatePeriods', 'ScopeType')")) {
            try (ResultSet rs = checkCol.executeQuery()) {
                if (rs.next() && rs.getObject(1) != null) {
                    hasScopeColumn = true;
                }
            }
        }

        long periodId;
        String insertSql = hasScopeColumn
                ? """
                  INSERT INTO ProfileUpdatePeriods (CompanyId, Title, StartsAt, EndsAt, CreatedByUserId, PeriodType, ScopeType)
                  VALUES (?, ?, ?, ?, ?, ?, ?)
                  """
                : """
                  INSERT INTO ProfileUpdatePeriods (CompanyId, Title, StartsAt, EndsAt, CreatedByUserId, PeriodType)
                  VALUES (?, ?, ?, ?, ?, ?)
                  """;
        try (PreparedStatement statement = connection.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setInt(1, companyId);
            statement.setString(2, title);
            statement.setTimestamp(3, Timestamp.valueOf(startsAt));
            statement.setTimestamp(4, Timestamp.valueOf(endsAt));
            statement.setInt(5, userId);
            statement.setString(6, periodType);
            if (hasScopeColumn) {
                statement.setString(7, normalizedScope);
            }
            statement.executeUpdate();
            try (ResultSet resultSet = statement.getGeneratedKeys()) {
                if (!resultSet.next()) {
                    throw new SQLException("Không thể tạo đợt cập nhật hồ sơ.");
                }
                periodId = resultSet.getLong(1);
            }
        }

        if (hasTable(connection, "dbo.ProfileUpdatePeriodFields")) {
            try (PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO dbo.ProfileUpdatePeriodFields (PeriodId, FieldKey) VALUES (?, ?)")) {
                for (String fk : validFieldKeys) {
                    ps.setLong(1, periodId);
                    ps.setString(2, fk);
                    ps.addBatch();
                }
                ps.executeBatch();
            }
        }

        if ("SELECTED".equals(normalizedScope) && hasTable(connection, "dbo.ProfileUpdatePeriodEmployees")) {
            try (PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO dbo.ProfileUpdatePeriodEmployees (PeriodId, EmployeeId) VALUES (?, ?)")) {
                for (Integer empId : uniqueEmpIds) {
                    ps.setLong(1, periodId);
                    ps.setInt(2, empId);
                    ps.addBatch();
                }
                ps.executeBatch();
            }
        }

        String fieldSummary = validFieldKeys.stream()
                .map(k -> FIELD_BY_KEY.containsKey(k) ? FIELD_BY_KEY.get(k).fieldLabel() : k)
                .collect(Collectors.joining(", "));
        audit(connection, companyId, userId, "OPEN_" + periodType + "_UPDATE_PERIOD", "ProfileUpdatePeriods", periodId,
                null, title + " [" + normalizedScope + "] (" + fieldSummary + "); " + startsAt + " - " + endsAt, ipAddress);
        return periodId;
    }

    private boolean hasTable(Connection connection, String tableName) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("SELECT OBJECT_ID(?, 'U')")) {
            ps.setString(1, tableName);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getObject(1) != null;
            }
        }
    }

    public void close(int companyId, int userId, long periodId, String ipAddress) throws SQLException {
        transaction(connection -> {
            close(connection, companyId, userId, periodId, ipAddress);
            return null;
        });
    }

    void close(Connection connection, int companyId, int userId, long periodId, String ipAddress) throws SQLException {
        requireAdministrator(connection, companyId, userId);
        try (PreparedStatement statement = connection.prepareStatement("""
                UPDATE ProfileUpdatePeriods SET ClosedAt = SYSUTCDATETIME()
                WHERE PeriodId = ? AND CompanyId = ? AND ClosedAt IS NULL AND PeriodType=?
                """)) {
            statement.setLong(1, periodId);
            statement.setInt(2, companyId);
            statement.setString(3, periodType);
            if (statement.executeUpdate() == 0) {
                throw new SecurityException("Đợt cập nhật không tồn tại trong công ty hoặc đã đóng.");
            }
        }
        audit(connection, companyId, userId, "CLOSE_" + periodType + "_UPDATE_PERIOD", "ProfileUpdatePeriods", periodId,
                "Open", "Closed", ipAddress);
    }

    public void updatePhone(int companyId, int userId, int employeeId, String phone, String ipAddress) throws SQLException {
        transaction(connection -> {
            updatePhone(connection, companyId, userId, employeeId, phone, ipAddress);
            return null;
        });
    }

    void updatePhone(Connection connection, int companyId, int userId, int employeeId, String phone,
                     String ipAddress) throws SQLException {
        updateProfile(connection, companyId, userId, employeeId, Map.of("phone", phone), ipAddress);
    }

    public void updateProfile(int companyId, int userId, int employeeId, Map<String, String> changes, String ipAddress) throws SQLException {
        updateProfile(companyId, userId, employeeId, changes, null, ipAddress);
    }

    public void updateProfile(int companyId, int userId, int employeeId, Map<String, String> changes, String expectedUpdatedAt, String ipAddress) throws SQLException {
        transaction(connection -> {
            ensureSchema(connection, companyId);
            updateProfile(connection, companyId, userId, employeeId, changes, expectedUpdatedAt, ipAddress);
            return null;
        });
    }

    public void updateProfile(Connection connection, int companyId, int userId, int employeeId, Map<String, String> changes, String ipAddress) throws SQLException {
        updateProfile(connection, companyId, userId, employeeId, changes, null, ipAddress);
    }

    public void updateProfile(Connection connection, int companyId, int userId, int employeeId, Map<String, String> changes, String expectedUpdatedAt, String ipAddress) throws SQLException {
        if (!"PROFILE".equals(periodType)) {
            throw new SecurityException("Đợt tài liệu không được dùng để cập nhật thông tin hồ sơ.");
        }
        if (changes == null || changes.isEmpty() || !FIELD_BY_KEY.keySet().containsAll(changes.keySet())) {
            throw new FieldValidationException("phone", "Các trường cập nhật hồ sơ không hợp lệ.");
        }
        OrganizationTransaction.lockCompany(connection, companyId);

        long activePeriodId;
        String activePeriodTitle;
        Set<String> allowedFieldsForPeriod = new LinkedHashSet<>();

        if (hasTable(connection, "dbo.ProfileUpdatePeriodFields")) {
            ActiveEmployeeProfileContext ctx = findActiveContextForEmployee(connection, companyId, employeeId, true);
            if (ctx == null) {
                throw new SecurityException("Hiện không có đợt cập nhật hồ sơ đang mở dành cho bạn. Bạn chưa thể sửa thông tin cá nhân.");
            }
            activePeriodId = ctx.periodId();
            activePeriodTitle = ctx.title();
            allowedFieldsForPeriod.addAll(ctx.allowedFieldKeys());
        } else {
            ProfileUpdatePeriod period = findAvailablePeriod(connection, companyId, true);
            if (period == null) {
                throw new SecurityException("Hiện không có đợt cập nhật hồ sơ đang mở. Bạn chưa thể sửa thông tin cá nhân.");
            }
            activePeriodId = period.periodId();
            activePeriodTitle = period.title();
            allowedFieldsForPeriod.addAll(DEFAULT_FIELD_KEYS);
        }

        for (String requestedField : changes.keySet()) {
            if (!allowedFieldsForPeriod.contains(requestedField)) {
                ProfileFieldDef def = FIELD_BY_KEY.get(requestedField);
                String label = def != null ? def.fieldLabel() : requestedField;
                throw new FieldValidationException(requestedField,
                        "Trường \"" + label + "\" không nằm trong danh mục cho phép của đợt cập nhật hồ sơ hiện tại.");
            }
        }

        Map<String, String> before = new LinkedHashMap<>();
        String sql = """
                SELECT e.Email, e.Phone, e.IdentityNumber, e.IdentityIssuedDate, e.IdentityIssuedPlace,
                       e.PermanentAddress, e.Hometown, e.DateOfBirth, e.Gender, e.Major,
                       CONVERT(varchar(33), COALESCE(e.UpdatedAt, e.CreatedAt), 126) AS ProfileVersion
                FROM Employees e WITH (UPDLOCK, HOLDLOCK)
                JOIN Users u ON u.UserId = e.UserId AND u.CompanyId = e.CompanyId
                JOIN Roles r ON r.RoleId = u.RoleId
                WHERE e.CompanyId = ? AND e.EmployeeId = ? AND e.UserId = ? AND e.Status = 'Active'
                  AND u.IsActive = 1 AND u.IsLocked = 0 AND r.RoleCode IN ('EMPLOYEE', 'MANAGER')
                """ + " AND " + ContractAccessPolicy.ALLOWED;
        java.sql.Date currentDob = null;
        java.sql.Date currentIssuedDate = null;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, companyId);
            statement.setInt(2, employeeId);
            statement.setInt(3, userId);
            try (ResultSet rows = statement.executeQuery()) {
                if (!rows.next()) {
                    throw new SecurityException("Bạn không có quyền cập nhật hồ sơ này.");
                }
                if (expectedUpdatedAt != null && !expectedUpdatedAt.equals(rows.getString("ProfileVersion"))) {
                    throw new FieldValidationException("phone", "Hồ sơ đã được thay đổi. Vui lòng tải lại để tránh ghi đè thông tin mới.");
                }
                for (String field : changes.keySet()) {
                    ProfileFieldDef def = FIELD_BY_KEY.get(field);
                    before.put(field, rows.getString(def.columnName()));
                }
                currentDob = rows.getDate("DateOfBirth");
                currentIssuedDate = rows.getDate("IdentityIssuedDate");
            }
        }

        java.sql.Date effectiveDob = changes.containsKey("dateOfBirth")
                ? (changes.get("dateOfBirth") == null ? null : java.sql.Date.valueOf(changes.get("dateOfBirth")))
                : currentDob;
        java.sql.Date effectiveIssuedDate = changes.containsKey("identityIssuedDate")
                ? (changes.get("identityIssuedDate") == null ? null : java.sql.Date.valueOf(changes.get("identityIssuedDate")))
                : currentIssuedDate;

        if (effectiveIssuedDate != null && effectiveDob != null && effectiveIssuedDate.before(effectiveDob)) {
            throw new FieldValidationException("identityIssuedDate", "Ngày cấp căn cước không được trước ngày sinh.");
        }

        for (String field : List.of("phone", "email", "identityNumber")) {
            if (!changes.containsKey(field) || changes.get(field) == null) {
                continue;
            }
            ProfileFieldDef def = FIELD_BY_KEY.get(field);
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT EmployeeId FROM Employees WITH (UPDLOCK, HOLDLOCK) WHERE CompanyId = ? AND "
                            + def.columnName() + " = ? AND EmployeeId <> ?")) {
                statement.setInt(1, companyId);
                statement.setString(2, changes.get(field));
                statement.setInt(3, employeeId);
                try (ResultSet rows = statement.executeQuery()) {
                    if (rows.next()) {
                        throw new FieldValidationException(field, def.fieldLabel() + " đã tồn tại trong công ty.");
                    }
                }
            }
        }

        List<String> changed = changes.keySet().stream()
                .filter(field -> !Objects.equals(before.get(field), changes.get(field)))
                .toList();
        if (changed.isEmpty()) {
            return;
        }

        String assignments = changed.stream()
                .map(field -> FIELD_BY_KEY.get(field).columnName() + " = ?")
                .collect(Collectors.joining(", "));
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE Employees SET " + assignments + ", UpdatedAt = SYSUTCDATETIME() "
                        + "WHERE CompanyId = ? AND EmployeeId = ? AND UserId = ? "
                        + "AND EXISTS (SELECT 1 FROM ProfileUpdatePeriods WHERE PeriodId = ? AND CompanyId = ? "
                        + "AND ClosedAt IS NULL AND StartsAt <= SYSUTCDATETIME() AND EndsAt > SYSUTCDATETIME())")) {
            int parameter = 1;
            for (String field : changed) {
                boolean isDate = "identityIssuedDate".equals(field) || "dateOfBirth".equals(field);
                statement.setObject(parameter++, isDate && changes.get(field) != null
                        ? java.sql.Date.valueOf(changes.get(field))
                        : changes.get(field));
            }
            statement.setInt(parameter++, companyId);
            statement.setInt(parameter++, employeeId);
            statement.setInt(parameter++, userId);
            statement.setLong(parameter++, activePeriodId);
            statement.setInt(parameter, companyId);
            if (statement.executeUpdate() != 1) {
                throw new SecurityException("Đợt cập nhật đã kết thúc hoặc hồ sơ không còn khả dụng.");
            }
        }

        if (hasTable(connection, "dbo.ProfileUpdatePeriodLogs")) {
            try (PreparedStatement ps = connection.prepareStatement("""
                    INSERT INTO dbo.ProfileUpdatePeriodLogs
                        (PeriodId, CompanyId, EmployeeId, FieldKey, FieldLabel, OldValue, NewValue)
                    VALUES (?, ?, ?, ?, ?, ?, ?)
                    """)) {
                for (String field : changed) {
                    ProfileFieldDef def = FIELD_BY_KEY.get(field);
                    ps.setLong(1, activePeriodId);
                    ps.setInt(2, companyId);
                    ps.setInt(3, employeeId);
                    ps.setString(4, field);
                    ps.setString(5, def.fieldLabel());
                    ps.setString(6, before.get(field));
                    ps.setString(7, changes.get(field));
                    ps.addBatch();
                }
                ps.executeBatch();
            }
        }

        String oldValues = changed.stream()
                .map(field -> FIELD_BY_KEY.get(field).fieldLabel() + ": " + (before.get(field) == null ? "Chưa cập nhật" : before.get(field)))
                .collect(Collectors.joining("\n"));
        String newValues = changed.stream()
                .map(field -> FIELD_BY_KEY.get(field).fieldLabel() + ": " + (changes.get(field) == null ? "Chưa cập nhật" : changes.get(field)))
                .collect(Collectors.joining("\n"));
        audit(connection, companyId, userId, "UPDATE_SELF_PROFILE", "Employees", employeeId,
                oldValues, newValues + "\nĐợt cập nhật: " + activePeriodTitle, ipAddress);
    }

    private ProfileUpdatePeriod findAvailablePeriod(Connection connection, int companyId, boolean lock) throws SQLException {
        String sql = "SELECT TOP 1 * FROM ProfileUpdatePeriods " + (lock ? "WITH (UPDLOCK, HOLDLOCK) " : "")
                + "WHERE CompanyId = ? AND ClosedAt IS NULL AND StartsAt <= SYSUTCDATETIME() "
                + "AND EndsAt > SYSUTCDATETIME() AND PeriodType=? ORDER BY StartsAt DESC";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, companyId);
            statement.setString(2, periodType);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? mapPeriod(resultSet) : null;
            }
        }
    }

    private void requireAdministrator(Connection connection, int companyId, int userId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT u.UserId FROM Users u JOIN Roles r ON r.RoleId = u.RoleId
                WHERE u.UserId = ? AND u.CompanyId = ? AND u.IsActive = 1 AND u.IsLocked = 0
                  AND r.RoleCode IN ('COMPANY_ADMIN', 'HR')
                """)) {
            statement.setInt(1, userId);
            statement.setInt(2, companyId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    throw new SecurityException("Bạn không có quyền quản lý đợt cập nhật của công ty này.");
                }
            }
        }
    }

    private ProfileUpdatePeriod mapPeriod(ResultSet resultSet) throws SQLException {
        Timestamp closedAt = resultSet.getTimestamp("ClosedAt");
        return new ProfileUpdatePeriod(resultSet.getLong("PeriodId"), resultSet.getInt("CompanyId"),
                resultSet.getString("Title"), resultSet.getTimestamp("StartsAt").toLocalDateTime(),
                resultSet.getTimestamp("EndsAt").toLocalDateTime(), closedAt == null ? null : closedAt.toLocalDateTime(), resultSet.getString("PeriodType"));
    }

    private void audit(Connection connection, int companyId, int userId, String action, String table, long recordId,
                       String oldValue, String newValue, String ipAddress) throws SQLException {
        AuditLog log = new AuditLog();
        log.setCompanyId(companyId);
        log.setUserId(userId);
        log.setAction(action);
        log.setTableName(table);
        log.setRecordId(String.valueOf(recordId));
        log.setOldValues(oldValue);
        log.setNewValues(newValue);
        log.setIpAddress(ipAddress);
        new AuditLogDAO().insert(connection, log);
    }

    private <Result> Result transaction(SqlWork<Result> work) throws SQLException {
        try (Connection connection = DBConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                Result result = work.execute(connection);
                connection.commit();
                return result;
            } catch (SQLException | RuntimeException exception) {
                connection.rollback();
                throw exception;
            }
        }
    }

    private interface SqlWork<Result> {
        Result execute(Connection connection) throws SQLException;
    }
}

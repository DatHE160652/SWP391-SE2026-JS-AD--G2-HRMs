package com.hrms.dao;

import com.hrms.model.AuditLog;
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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DocumentUpdatePeriodDAO {

    private static final List<String> DEFAULT_DOCUMENT_TYPES = List.of(
            "CCCD / CMND",
            "Bảng điểm",
            "Hợp đồng lao động",
            "Bằng đại học",
            "Bằng thạc sĩ",
            "Chứng chỉ chuyên môn",
            "Giấy khám sức khỏe",
            "Sơ yếu lý lịch"
    );

    public record DocumentTypeItem(int documentTypeId, int companyId, String typeName, boolean isActive) {}

    public record ActiveEmployeeDocumentContext(
            long periodId,
            String title,
            LocalDateTime startsAt,
            LocalDateTime endsAt,
            List<String> allowedDocumentTypes,
            List<Map<String, Object>> activePeriods
    ) {}

    public synchronized void ensureSchemaAndCompanyDefaults(Connection connection, int companyId) throws SQLException {
        String ddl = """
                IF OBJECT_ID('dbo.CompanyDocumentTypes', 'U') IS NULL
                BEGIN
                    CREATE TABLE dbo.CompanyDocumentTypes (
                        DocumentTypeId INT IDENTITY(1,1) NOT NULL PRIMARY KEY,
                        CompanyId INT NOT NULL,
                        TypeName NVARCHAR(100) NOT NULL,
                        IsActive BIT NOT NULL CONSTRAINT DF_CompanyDocumentTypes_IsActive DEFAULT (1),
                        CreatedAt DATETIME2(7) NOT NULL CONSTRAINT DF_CompanyDocumentTypes_CreatedAt DEFAULT (SYSUTCDATETIME()),
                        CONSTRAINT FK_CompanyDocumentTypes_Companies FOREIGN KEY (CompanyId) REFERENCES dbo.Companies(CompanyId),
                        CONSTRAINT UQ_CompanyDocumentTypes_Company_Name UNIQUE (CompanyId, TypeName)
                    );
                END;

                IF OBJECT_ID('dbo.DocumentUpdatePeriods', 'U') IS NULL
                BEGIN
                    CREATE TABLE dbo.DocumentUpdatePeriods (
                        DocumentPeriodId BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
                        CompanyId INT NOT NULL,
                        Title NVARCHAR(150) NOT NULL,
                        StartsAt DATETIME2(7) NOT NULL,
                        EndsAt DATETIME2(7) NOT NULL,
                        ScopeType VARCHAR(20) NOT NULL CONSTRAINT DF_DocumentUpdatePeriods_ScopeType DEFAULT ('ALL'),
                        ClosedAt DATETIME2(7) NULL,
                        CreatedByUserId INT NOT NULL,
                        CreatedAt DATETIME2(7) NOT NULL CONSTRAINT DF_DocumentUpdatePeriods_CreatedAt DEFAULT (SYSUTCDATETIME()),
                        CONSTRAINT FK_DocumentUpdatePeriods_Companies FOREIGN KEY (CompanyId) REFERENCES dbo.Companies(CompanyId),
                        CONSTRAINT FK_DocumentUpdatePeriods_Users FOREIGN KEY (CreatedByUserId) REFERENCES dbo.Users(UserId),
                        CONSTRAINT CK_DocumentUpdatePeriods_Dates CHECK (EndsAt > StartsAt),
                        CONSTRAINT CK_DocumentUpdatePeriods_Scope CHECK (ScopeType IN ('ALL', 'SELECTED'))
                    );
                    CREATE INDEX IX_DocumentUpdatePeriods_CompanyDates
                        ON dbo.DocumentUpdatePeriods(CompanyId, ClosedAt, StartsAt, EndsAt);
                END;

                IF OBJECT_ID('dbo.DocumentUpdatePeriodTypes', 'U') IS NULL
                BEGIN
                    CREATE TABLE dbo.DocumentUpdatePeriodTypes (
                        DocumentPeriodId BIGINT NOT NULL,
                        DocumentTypeId INT NOT NULL,
                        CONSTRAINT PK_DocumentUpdatePeriodTypes PRIMARY KEY (DocumentPeriodId, DocumentTypeId),
                        CONSTRAINT FK_DocumentUpdatePeriodTypes_Period FOREIGN KEY (DocumentPeriodId)
                            REFERENCES dbo.DocumentUpdatePeriods(DocumentPeriodId) ON DELETE CASCADE,
                        CONSTRAINT FK_DocumentUpdatePeriodTypes_Type FOREIGN KEY (DocumentTypeId)
                            REFERENCES dbo.CompanyDocumentTypes(DocumentTypeId) ON DELETE CASCADE
                    );
                END;

                IF OBJECT_ID('dbo.DocumentUpdatePeriodEmployees', 'U') IS NULL
                BEGIN
                    CREATE TABLE dbo.DocumentUpdatePeriodEmployees (
                        DocumentPeriodId BIGINT NOT NULL,
                        EmployeeId INT NOT NULL,
                        CONSTRAINT PK_DocumentUpdatePeriodEmployees PRIMARY KEY (DocumentPeriodId, EmployeeId),
                        CONSTRAINT FK_DocumentUpdatePeriodEmployees_Period FOREIGN KEY (DocumentPeriodId)
                            REFERENCES dbo.DocumentUpdatePeriods(DocumentPeriodId) ON DELETE CASCADE,
                        CONSTRAINT FK_DocumentUpdatePeriodEmployees_Employee FOREIGN KEY (EmployeeId)
                            REFERENCES dbo.Employees(EmployeeId) ON DELETE CASCADE
                    );
                END;

                IF COL_LENGTH('dbo.EmployeeDocuments', 'DocumentPeriodId') IS NULL
                BEGIN
                    ALTER TABLE dbo.EmployeeDocuments ADD DocumentPeriodId BIGINT NULL;
                END;
                """;
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(ddl);
        }

        if (companyId <= 0) {
            return;
        }

        boolean hasTypes;
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT TOP 1 1 FROM dbo.CompanyDocumentTypes WHERE CompanyId = ?")) {
            ps.setInt(1, companyId);
            try (ResultSet rs = ps.executeQuery()) {
                hasTypes = rs.next();
            }
        }
        if (!hasTypes) {
            try (PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO dbo.CompanyDocumentTypes (CompanyId, TypeName, IsActive) VALUES (?, ?, 1)")) {
                for (String typeName : DEFAULT_DOCUMENT_TYPES) {
                    ps.setInt(1, companyId);
                    ps.setString(2, typeName);
                    ps.addBatch();
                }
                ps.executeBatch();
            }
        }

        // Migrate any legacy DOCUMENT periods from ProfileUpdatePeriods into DocumentUpdatePeriods
        if (hasLegacyDocumentPeriods(connection, companyId)) {
            migrateLegacyDocumentPeriods(connection, companyId);
        }
    }

    private boolean hasLegacyDocumentPeriods(Connection connection, int companyId) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("""
                SELECT TOP 1 1 FROM dbo.ProfileUpdatePeriods
                WHERE CompanyId = ? AND PeriodType = 'DOCUMENT'
                """)) {
            ps.setInt(1, companyId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private void migrateLegacyDocumentPeriods(Connection connection, int companyId) throws SQLException {
        List<Integer> allTypeIds = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT DocumentTypeId FROM dbo.CompanyDocumentTypes WHERE CompanyId = ? AND IsActive = 1 ORDER BY DocumentTypeId")) {
            ps.setInt(1, companyId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    allTypeIds.add(rs.getInt(1));
                }
            }
        }
        try (PreparedStatement selectLegacy = connection.prepareStatement("""
                SELECT PeriodId, Title, StartsAt, EndsAt, ClosedAt, CreatedByUserId, CreatedAt
                FROM dbo.ProfileUpdatePeriods
                WHERE CompanyId = ? AND PeriodType = 'DOCUMENT'
                ORDER BY PeriodId ASC
                """);
             PreparedStatement insertNew = connection.prepareStatement("""
                INSERT INTO dbo.DocumentUpdatePeriods (CompanyId, Title, StartsAt, EndsAt, ScopeType, ClosedAt, CreatedByUserId, CreatedAt)
                VALUES (?, ?, ?, ?, 'ALL', ?, ?, ?)
                """, Statement.RETURN_GENERATED_KEYS);
             PreparedStatement insertType = connection.prepareStatement("""
                INSERT INTO dbo.DocumentUpdatePeriodTypes (DocumentPeriodId, DocumentTypeId) VALUES (?, ?)
                """)) {
            selectLegacy.setInt(1, companyId);
            try (ResultSet rs = selectLegacy.executeQuery()) {
                while (rs.next()) {
                    insertNew.setInt(1, companyId);
                    insertNew.setString(2, rs.getString("Title"));
                    insertNew.setTimestamp(3, rs.getTimestamp("StartsAt"));
                    insertNew.setTimestamp(4, rs.getTimestamp("EndsAt"));
                    insertNew.setTimestamp(5, rs.getTimestamp("ClosedAt"));
                    insertNew.setInt(6, rs.getInt("CreatedByUserId"));
                    insertNew.setTimestamp(7, rs.getTimestamp("CreatedAt"));
                    insertNew.executeUpdate();
                    try (ResultSet keys = insertNew.getGeneratedKeys()) {
                        if (keys.next()) {
                            long newPeriodId = keys.getLong(1);
                            for (int typeId : allTypeIds) {
                                insertType.setLong(1, newPeriodId);
                                insertType.setInt(2, typeId);
                                insertType.addBatch();
                            }
                            insertType.executeBatch();
                        }
                    }
                }
            }
        }
        try (PreparedStatement deleteLegacy = connection.prepareStatement(
                "DELETE FROM dbo.ProfileUpdatePeriods WHERE CompanyId = ? AND PeriodType = 'DOCUMENT'")) {
            deleteLegacy.setInt(1, companyId);
            deleteLegacy.executeUpdate();
        }
    }

    public List<DocumentTypeItem> getDocumentTypes(int companyId, boolean activeOnly) throws SQLException {
        try (Connection connection = DBConnection.getConnection()) {
            ensureSchemaAndCompanyDefaults(connection, companyId);
            String sql = "SELECT DocumentTypeId, CompanyId, TypeName, IsActive FROM dbo.CompanyDocumentTypes WHERE CompanyId = ? "
                    + (activeOnly ? "AND IsActive = 1 " : "")
                    + "ORDER BY IsActive DESC, DocumentTypeId ASC";
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setInt(1, companyId);
                try (ResultSet rs = ps.executeQuery()) {
                    List<DocumentTypeItem> list = new ArrayList<>();
                    while (rs.next()) {
                        list.add(new DocumentTypeItem(
                                rs.getInt("DocumentTypeId"),
                                rs.getInt("CompanyId"),
                                rs.getString("TypeName"),
                                rs.getBoolean("IsActive")
                        ));
                    }
                    return list;
                }
            }
        }
    }

    public DocumentTypeItem addDocumentType(int companyId, int userId, String rawName, String ipAddress) throws SQLException {
        String typeName = rawName == null ? "" : rawName.trim();
        if (typeName.isEmpty() || typeName.length() > 100) {
            throw new FieldValidationException("typeName", "Vui lòng nhập tên loại tài liệu (tối đa 100 ký tự).");
        }
        try (Connection connection = DBConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                ensureSchemaAndCompanyDefaults(connection, companyId);
                requireAdministrator(connection, companyId, userId);

                try (PreparedStatement check = connection.prepareStatement(
                        "SELECT DocumentTypeId, IsActive FROM dbo.CompanyDocumentTypes WHERE CompanyId = ? AND LOWER(LTRIM(RTRIM(TypeName))) = LOWER(?)")) {
                    check.setInt(1, companyId);
                    check.setString(2, typeName);
                    try (ResultSet rs = check.executeQuery()) {
                        if (rs.next()) {
                            int existingId = rs.getInt("DocumentTypeId");
                            boolean active = rs.getBoolean("IsActive");
                            if (!active) {
                                try (PreparedStatement reactivate = connection.prepareStatement(
                                        "UPDATE dbo.CompanyDocumentTypes SET IsActive = 1, TypeName = ? WHERE DocumentTypeId = ? AND CompanyId = ?")) {
                                    reactivate.setString(1, typeName);
                                    reactivate.setInt(2, existingId);
                                    reactivate.setInt(3, companyId);
                                    reactivate.executeUpdate();
                                }
                                connection.commit();
                                return new DocumentTypeItem(existingId, companyId, typeName, true);
                            }
                            throw new FieldValidationException("typeName", "Loại tài liệu này đã tồn tại trong công ty.");
                        }
                    }
                }

                int newId;
                try (PreparedStatement insert = connection.prepareStatement(
                        "INSERT INTO dbo.CompanyDocumentTypes (CompanyId, TypeName, IsActive) VALUES (?, ?, 1)",
                        Statement.RETURN_GENERATED_KEYS)) {
                    insert.setInt(1, companyId);
                    insert.setString(2, typeName);
                    insert.executeUpdate();
                    try (ResultSet keys = insert.getGeneratedKeys()) {
                        if (!keys.next()) {
                            throw new SQLException("Không thể tạo loại tài liệu.");
                        }
                        newId = keys.getInt(1);
                    }
                }
                audit(connection, companyId, userId, "CREATE_DOCUMENT_TYPE", "CompanyDocumentTypes", newId, null, typeName, ipAddress);
                connection.commit();
                return new DocumentTypeItem(newId, companyId, typeName, true);
            } catch (SQLException | RuntimeException e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    public void deleteOrDeactivateDocumentType(int companyId, int userId, int documentTypeId, String ipAddress) throws SQLException {
        try (Connection connection = DBConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                ensureSchemaAndCompanyDefaults(connection, companyId);
                requireAdministrator(connection, companyId, userId);

                String typeName = null;
                try (PreparedStatement ps = connection.prepareStatement(
                        "SELECT TypeName FROM dbo.CompanyDocumentTypes WHERE DocumentTypeId = ? AND CompanyId = ?")) {
                    ps.setInt(1, documentTypeId);
                    ps.setInt(2, companyId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            typeName = rs.getString("TypeName");
                        }
                    }
                }
                if (typeName == null) {
                    throw new IllegalArgumentException("Không tìm thấy loại tài liệu.");
                }

                boolean usedInPeriods;
                try (PreparedStatement ps = connection.prepareStatement(
                        "SELECT TOP 1 1 FROM dbo.DocumentUpdatePeriodTypes WHERE DocumentTypeId = ?")) {
                    ps.setInt(1, documentTypeId);
                    try (ResultSet rs = ps.executeQuery()) {
                        usedInPeriods = rs.next();
                    }
                }

                if (usedInPeriods) {
                    try (PreparedStatement ps = connection.prepareStatement(
                            "UPDATE dbo.CompanyDocumentTypes SET IsActive = 0 WHERE DocumentTypeId = ? AND CompanyId = ?")) {
                        ps.setInt(1, documentTypeId);
                        ps.setInt(2, companyId);
                        ps.executeUpdate();
                    }
                } else {
                    try (PreparedStatement ps = connection.prepareStatement(
                            "DELETE FROM dbo.CompanyDocumentTypes WHERE DocumentTypeId = ? AND CompanyId = ?")) {
                        ps.setInt(1, documentTypeId);
                        ps.setInt(2, companyId);
                        ps.executeUpdate();
                    }
                }
                audit(connection, companyId, userId, "DELETE_DOCUMENT_TYPE", "CompanyDocumentTypes", documentTypeId, typeName, "Removed", ipAddress);
                connection.commit();
            } catch (SQLException | RuntimeException e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    public List<Map<String, Object>> getActiveCompanyEmployees(int companyId) throws SQLException {
        try (Connection connection = DBConnection.getConnection()) {
            String sql = """
                    SELECT e.EmployeeId, e.EmployeeCode, e.FullName,
                           d.DepartmentName, p.PositionTitle
                    FROM dbo.Employees e
                    LEFT JOIN dbo.Departments d ON d.DepartmentId = e.DepartmentId AND d.CompanyId = e.CompanyId
                    LEFT JOIN dbo.Positions p ON p.PositionId = e.PositionId AND p.CompanyId = e.CompanyId
                    WHERE e.CompanyId = ? AND e.Status = 'Active'
                    ORDER BY e.FullName ASC, e.EmployeeId ASC
                    """;
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setInt(1, companyId);
                try (ResultSet rs = ps.executeQuery()) {
                    List<Map<String, Object>> employees = new ArrayList<>();
                    while (rs.next()) {
                        Map<String, Object> item = new LinkedHashMap<>();
                        item.put("employeeId", rs.getInt("EmployeeId"));
                        item.put("employeeCode", rs.getString("EmployeeCode"));
                        item.put("fullName", rs.getString("FullName"));
                        item.put("departmentName", rs.getString("DepartmentName"));
                        item.put("positionTitle", rs.getString("PositionTitle"));
                        employees.add(item);
                    }
                    return employees;
                }
            }
        }
    }

    public List<Map<String, Object>> getPeriods(int companyId) throws SQLException {
        try (Connection connection = DBConnection.getConnection()) {
            ensureSchemaAndCompanyDefaults(connection, companyId);

            int totalActiveEmployees = 0;
            try (PreparedStatement ps = connection.prepareStatement(
                    "SELECT COUNT(*) FROM dbo.Employees WHERE CompanyId = ? AND Status = 'Active'")) {
                ps.setInt(1, companyId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        totalActiveEmployees = rs.getInt(1);
                    }
                }
            }

            List<Map<String, Object>> periods = new ArrayList<>();
            Map<Long, Map<String, Object>> periodMap = new LinkedHashMap<>();

            String sql = """
                    SELECT TOP 100 p.DocumentPeriodId, p.CompanyId, p.Title, p.StartsAt, p.EndsAt,
                           p.ScopeType, p.ClosedAt, p.CreatedAt
                    FROM dbo.DocumentUpdatePeriods p
                    WHERE p.CompanyId = ?
                    ORDER BY p.DocumentPeriodId DESC
                    """;
            LocalDateTime nowUtc = LocalDateTime.now(ZoneOffset.UTC);
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setInt(1, companyId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        long periodId = rs.getLong("DocumentPeriodId");
                        LocalDateTime startsAt = rs.getTimestamp("StartsAt").toLocalDateTime();
                        LocalDateTime endsAt = rs.getTimestamp("EndsAt").toLocalDateTime();
                        Timestamp closedAt = rs.getTimestamp("ClosedAt");
                        String scopeType = rs.getString("ScopeType");
                        boolean closed = closedAt != null;
                        boolean open = !closed && !nowUtc.isBefore(startsAt) && nowUtc.isBefore(endsAt);

                        Map<String, Object> item = new LinkedHashMap<>();
                        item.put("periodId", periodId);
                        item.put("title", rs.getString("Title"));
                        item.put("periodType", "DOCUMENT");
                        item.put("startsAt", startsAt.atOffset(ZoneOffset.UTC).toString());
                        item.put("endsAt", endsAt.atOffset(ZoneOffset.UTC).toString());
                        item.put("scopeType", scopeType);
                        item.put("closed", closed);
                        item.put("open", open);
                        item.put("documentTypes", new ArrayList<Map<String, Object>>());
                        item.put("targetEmployees", new ArrayList<Map<String, Object>>());
                        item.put("targetEmployeeCount", "ALL".equalsIgnoreCase(scopeType) ? totalActiveEmployees : 0);
                        item.put("updatedEmployeeCount", 0);
                        item.put("uploadedDocumentCount", 0);
                        periods.add(item);
                        periodMap.put(periodId, item);
                    }
                }
            }

            if (periods.isEmpty()) {
                return periods;
            }

            // Load Document Types for each period
            String typeSql = """
                    SELECT pt.DocumentPeriodId, dt.DocumentTypeId, dt.TypeName
                    FROM dbo.DocumentUpdatePeriodTypes pt
                    JOIN dbo.CompanyDocumentTypes dt ON dt.DocumentTypeId = pt.DocumentTypeId
                    JOIN dbo.DocumentUpdatePeriods p ON p.DocumentPeriodId = pt.DocumentPeriodId
                    WHERE p.CompanyId = ?
                    ORDER BY dt.DocumentTypeId ASC
                    """;
            try (PreparedStatement ps = connection.prepareStatement(typeSql)) {
                ps.setInt(1, companyId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        Map<String, Object> period = periodMap.get(rs.getLong("DocumentPeriodId"));
                        if (period != null) {
                            @SuppressWarnings("unchecked")
                            List<Map<String, Object>> types = (List<Map<String, Object>>) period.get("documentTypes");
                            Map<String, Object> t = new LinkedHashMap<>();
                            t.put("documentTypeId", rs.getInt("DocumentTypeId"));
                            t.put("typeName", rs.getString("TypeName"));
                            types.add(t);
                        }
                    }
                }
            }

            // Load Selected Employees for SELECTED periods
            String empSql = """
                    SELECT pe.DocumentPeriodId, e.EmployeeId, e.EmployeeCode, e.FullName, d.DepartmentName
                    FROM dbo.DocumentUpdatePeriodEmployees pe
                    JOIN dbo.Employees e ON e.EmployeeId = pe.EmployeeId
                    LEFT JOIN dbo.Departments d ON d.DepartmentId = e.DepartmentId AND d.CompanyId = e.CompanyId
                    JOIN dbo.DocumentUpdatePeriods p ON p.DocumentPeriodId = pe.DocumentPeriodId
                    WHERE p.CompanyId = ?
                    ORDER BY e.FullName ASC
                    """;
            try (PreparedStatement ps = connection.prepareStatement(empSql)) {
                ps.setInt(1, companyId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        Map<String, Object> period = periodMap.get(rs.getLong("DocumentPeriodId"));
                        if (period != null) {
                            @SuppressWarnings("unchecked")
                            List<Map<String, Object>> emps = (List<Map<String, Object>>) period.get("targetEmployees");
                            Map<String, Object> emp = new LinkedHashMap<>();
                            emp.put("employeeId", rs.getInt("EmployeeId"));
                            emp.put("employeeCode", rs.getString("EmployeeCode"));
                            emp.put("fullName", rs.getString("FullName"));
                            emp.put("departmentName", rs.getString("DepartmentName"));
                            emps.add(emp);
                            if ("SELECTED".equalsIgnoreCase(String.valueOf(period.get("scopeType")))) {
                                period.put("targetEmployeeCount", emps.size());
                            }
                        }
                    }
                }
            }

            // Count uploaded documents & updated employees per period
            for (Map<String, Object> period : periods) {
                long periodId = (Long) period.get("periodId");
                List<Map<String, Object>> docs = queryPeriodUploadedDocuments(connection, companyId, periodId);
                Set<Integer> distinctEmployees = new LinkedHashSet<>();
                for (Map<String, Object> d : docs) {
                    distinctEmployees.add((Integer) d.get("employeeId"));
                }
                period.put("uploadedDocumentCount", docs.size());
                period.put("updatedEmployeeCount", distinctEmployees.size());
            }

            return periods;
        }
    }

    public Map<String, Object> getPeriodDetail(int companyId, long periodId) throws SQLException {
        try (Connection connection = DBConnection.getConnection()) {
            ensureSchemaAndCompanyDefaults(connection, companyId);

            Map<String, Object> detail = null;
            LocalDateTime nowUtc = LocalDateTime.now(ZoneOffset.UTC);
            String sql = """
                    SELECT DocumentPeriodId, CompanyId, Title, StartsAt, EndsAt, ScopeType, ClosedAt, CreatedAt
                    FROM dbo.DocumentUpdatePeriods
                    WHERE CompanyId = ? AND DocumentPeriodId = ?
                    """;
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setInt(1, companyId);
                ps.setLong(2, periodId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        LocalDateTime startsAt = rs.getTimestamp("StartsAt").toLocalDateTime();
                        LocalDateTime endsAt = rs.getTimestamp("EndsAt").toLocalDateTime();
                        Timestamp closedAt = rs.getTimestamp("ClosedAt");
                        boolean closed = closedAt != null;
                        boolean open = !closed && !nowUtc.isBefore(startsAt) && nowUtc.isBefore(endsAt);

                        detail = new LinkedHashMap<>();
                        detail.put("periodId", rs.getLong("DocumentPeriodId"));
                        detail.put("title", rs.getString("Title"));
                        detail.put("startsAt", startsAt.atOffset(ZoneOffset.UTC).toString());
                        detail.put("endsAt", endsAt.atOffset(ZoneOffset.UTC).toString());
                        detail.put("scopeType", rs.getString("ScopeType"));
                        detail.put("closed", closed);
                        detail.put("open", open);
                    }
                }
            }
            if (detail == null) {
                throw new IllegalArgumentException("Không tìm thấy đợt cập nhật tài liệu.");
            }

            List<Map<String, Object>> requiredTypes = new ArrayList<>();
            List<String> requiredTypeNames = new ArrayList<>();
            try (PreparedStatement ps = connection.prepareStatement("""
                    SELECT dt.DocumentTypeId, dt.TypeName
                    FROM dbo.DocumentUpdatePeriodTypes pt
                    JOIN dbo.CompanyDocumentTypes dt ON dt.DocumentTypeId = pt.DocumentTypeId
                    WHERE pt.DocumentPeriodId = ?
                    ORDER BY dt.DocumentTypeId ASC
                    """)) {
                ps.setLong(1, periodId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        Map<String, Object> t = new LinkedHashMap<>();
                        t.put("documentTypeId", rs.getInt("DocumentTypeId"));
                        t.put("typeName", rs.getString("TypeName"));
                        requiredTypes.add(t);
                        requiredTypeNames.add(rs.getString("TypeName"));
                    }
                }
            }
            detail.put("documentTypes", requiredTypes);

            List<Map<String, Object>> uploadedDocuments = queryPeriodUploadedDocuments(connection, companyId, periodId);
            detail.put("uploadedDocuments", uploadedDocuments);

            // Build target employees status list
            String scopeType = String.valueOf(detail.get("scopeType"));
            String targetEmpSql = "SELECTED".equalsIgnoreCase(scopeType)
                    ? """
                      SELECT e.EmployeeId, e.EmployeeCode, e.FullName, d.DepartmentName, p.PositionTitle
                      FROM dbo.DocumentUpdatePeriodEmployees pe
                      JOIN dbo.Employees e ON e.EmployeeId = pe.EmployeeId AND e.CompanyId = ?
                      LEFT JOIN dbo.Departments d ON d.DepartmentId = e.DepartmentId AND d.CompanyId = e.CompanyId
                      LEFT JOIN dbo.Positions p ON p.PositionId = e.PositionId AND p.CompanyId = e.CompanyId
                      WHERE pe.DocumentPeriodId = ?
                      ORDER BY e.FullName ASC
                      """
                    : """
                      SELECT e.EmployeeId, e.EmployeeCode, e.FullName, d.DepartmentName, p.PositionTitle
                      FROM dbo.Employees e
                      LEFT JOIN dbo.Departments d ON d.DepartmentId = e.DepartmentId AND d.CompanyId = e.CompanyId
                      LEFT JOIN dbo.Positions p ON p.PositionId = e.PositionId AND p.CompanyId = e.CompanyId
                      WHERE e.CompanyId = ? AND e.Status = 'Active'
                      ORDER BY e.FullName ASC
                      """;

            Map<Integer, List<Map<String, Object>>> docsByEmp = new LinkedHashMap<>();
            for (Map<String, Object> doc : uploadedDocuments) {
                int empId = (Integer) doc.get("employeeId");
                docsByEmp.computeIfAbsent(empId, k -> new ArrayList<>()).add(doc);
            }

            List<Map<String, Object>> employeeStatuses = new ArrayList<>();
            int updatedCount = 0;
            try (PreparedStatement ps = connection.prepareStatement(targetEmpSql)) {
                ps.setInt(1, companyId);
                if ("SELECTED".equalsIgnoreCase(scopeType)) {
                    ps.setLong(2, periodId);
                }
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        int empId = rs.getInt("EmployeeId");
                        List<Map<String, Object>> empDocs = docsByEmp.getOrDefault(empId, List.of());
                        Set<String> uploadedTypeSet = new LinkedHashSet<>();
                        for (Map<String, Object> d : empDocs) {
                            String dt = (String) d.get("documentType");
                            if (dt != null && !dt.isBlank()) {
                                uploadedTypeSet.add(dt.trim());
                            }
                        }
                        List<String> missingTypes = new ArrayList<>();
                        for (String reqType : requiredTypeNames) {
                            boolean found = uploadedTypeSet.stream().anyMatch(u -> u.equalsIgnoreCase(reqType));
                            if (!found) {
                                missingTypes.add(reqType);
                            }
                        }
                        if (!empDocs.isEmpty()) {
                            updatedCount++;
                        }
                        String completionStatus = empDocs.isEmpty()
                                ? "NONE"
                                : (missingTypes.isEmpty() ? "FULL" : "PARTIAL");

                        Map<String, Object> empRow = new LinkedHashMap<>();
                        empRow.put("employeeId", empId);
                        empRow.put("employeeCode", rs.getString("EmployeeCode"));
                        empRow.put("fullName", rs.getString("FullName"));
                        empRow.put("departmentName", rs.getString("DepartmentName"));
                        empRow.put("positionTitle", rs.getString("PositionTitle"));
                        empRow.put("uploadedCount", empDocs.size());
                        empRow.put("uploadedTypes", new ArrayList<>(uploadedTypeSet));
                        empRow.put("missingTypes", missingTypes);
                        empRow.put("completionStatus", completionStatus);
                        empRow.put("documents", empDocs);
                        employeeStatuses.add(empRow);
                    }
                }
            }

            detail.put("employeeStatuses", employeeStatuses);
            detail.put("targetEmployeeCount", employeeStatuses.size());
            detail.put("updatedEmployeeCount", updatedCount);
            detail.put("uploadedDocumentCount", uploadedDocuments.size());
            return detail;
        }
    }

    private List<Map<String, Object>> queryPeriodUploadedDocuments(Connection connection, int companyId, long periodId) throws SQLException {
        String sql = """
                SELECT ed.DocumentId, ed.EmployeeId, e.EmployeeCode, e.FullName, d.DepartmentName,
                       ed.DocumentName, ed.DocumentType, ed.FileName, ed.UploadedDate, ed.Status,
                       u.Username AS UploadedByName
                FROM dbo.EmployeeDocuments ed
                JOIN dbo.Employees e ON e.EmployeeId = ed.EmployeeId AND e.CompanyId = ?
                LEFT JOIN dbo.Departments d ON d.DepartmentId = e.DepartmentId AND d.CompanyId = e.CompanyId
                LEFT JOIN dbo.Users u ON u.UserId = ed.UploadedBy
                JOIN dbo.DocumentUpdatePeriods p ON p.DocumentPeriodId = ? AND p.CompanyId = e.CompanyId
                WHERE ed.DocumentPeriodId = p.DocumentPeriodId
                   OR (
                       ed.DocumentPeriodId IS NULL
                       AND ed.UploadedDate >= p.StartsAt
                       AND ed.UploadedDate <= p.EndsAt
                       AND (
                           p.ScopeType = 'ALL'
                           OR EXISTS (
                               SELECT 1 FROM dbo.DocumentUpdatePeriodEmployees pe
                               WHERE pe.DocumentPeriodId = p.DocumentPeriodId AND pe.EmployeeId = ed.EmployeeId
                           )
                       )
                   )
                ORDER BY ed.UploadedDate DESC, ed.DocumentId DESC
                """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, companyId);
            ps.setLong(2, periodId);
            try (ResultSet rs = ps.executeQuery()) {
                List<Map<String, Object>> list = new ArrayList<>();
                while (rs.next()) {
                    Map<String, Object> doc = new LinkedHashMap<>();
                    doc.put("documentId", rs.getInt("DocumentId"));
                    doc.put("employeeId", rs.getInt("EmployeeId"));
                    doc.put("employeeCode", rs.getString("EmployeeCode"));
                    doc.put("fullName", rs.getString("FullName"));
                    doc.put("departmentName", rs.getString("DepartmentName"));
                    doc.put("documentName", rs.getString("DocumentName"));
                    doc.put("documentType", rs.getString("DocumentType"));
                    doc.put("fileName", rs.getString("FileName"));
                    Timestamp uploadedDate = rs.getTimestamp("UploadedDate");
                    doc.put("uploadedDate", uploadedDate == null ? null : uploadedDate.toString());
                    doc.put("status", rs.getString("Status"));
                    doc.put("uploadedByName", rs.getString("UploadedByName"));
                    list.add(doc);
                }
                return list;
            }
        }
    }

    public long createPeriod(int companyId, int userId, String rawTitle, LocalDateTime startsAt, LocalDateTime endsAt,
                             String rawScopeType, List<Integer> documentTypeIds, List<Integer> employeeIds,
                             String ipAddress) throws SQLException {
        String title = rawTitle == null ? "" : rawTitle.trim();
        if (title.isEmpty() || title.length() > 150) {
            throw new FieldValidationException("title", "Vui lòng nhập tên đợt cập nhật, tối đa 150 ký tự.");
        }
        if (startsAt == null) {
            throw new FieldValidationException("startsAt", "Vui lòng chọn thời gian bắt đầu.");
        }
        LocalDateTime nowUtc = LocalDateTime.now(ZoneOffset.UTC);
        if (endsAt == null || !endsAt.isAfter(startsAt) || !endsAt.isAfter(nowUtc)) {
            throw new FieldValidationException("endsAt", "Thời gian kết thúc phải sau thời gian bắt đầu và thời điểm hiện tại.");
        }
        String scopeType = "SELECTED".equalsIgnoreCase(rawScopeType) ? "SELECTED" : "ALL";
        if (documentTypeIds == null || documentTypeIds.isEmpty()) {
            throw new FieldValidationException("documentTypeIds", "Vui lòng chọn ít nhất 1 loại tài liệu cần cập nhật trong đợt này.");
        }
        if ("SELECTED".equals(scopeType) && (employeeIds == null || employeeIds.isEmpty())) {
            throw new FieldValidationException("employeeIds", "Vui lòng chọn ít nhất 1 nhân viên cho đợt cập nhật cá nhân.");
        }

        try (Connection connection = DBConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                ensureSchemaAndCompanyDefaults(connection, companyId);
                requireAdministrator(connection, companyId, userId);

                Set<Integer> validTypeIds = new LinkedHashSet<>();
                List<String> selectedTypeNames = new ArrayList<>();
                try (PreparedStatement ps = connection.prepareStatement(
                        "SELECT DocumentTypeId, TypeName FROM dbo.CompanyDocumentTypes WHERE CompanyId = ? AND IsActive = 1")) {
                    ps.setInt(1, companyId);
                    try (ResultSet rs = ps.executeQuery()) {
                        Map<Integer, String> companyTypeMap = new LinkedHashMap<>();
                        while (rs.next()) {
                            companyTypeMap.put(rs.getInt("DocumentTypeId"), rs.getString("TypeName"));
                        }
                        for (Integer id : documentTypeIds) {
                            if (id != null && companyTypeMap.containsKey(id)) {
                                validTypeIds.add(id);
                                selectedTypeNames.add(companyTypeMap.get(id));
                            }
                        }
                    }
                }
                if (validTypeIds.isEmpty()) {
                    throw new FieldValidationException("documentTypeIds", "Loại tài liệu đã chọn không hợp lệ.");
                }

                Set<Integer> validEmployeeIds = new LinkedHashSet<>();
                if ("SELECTED".equals(scopeType)) {
                    try (PreparedStatement ps = connection.prepareStatement(
                            "SELECT EmployeeId FROM dbo.Employees WHERE CompanyId = ? AND Status = 'Active'")) {
                        ps.setInt(1, companyId);
                        try (ResultSet rs = ps.executeQuery()) {
                            Set<Integer> companyEmpSet = new LinkedHashSet<>();
                            while (rs.next()) {
                                companyEmpSet.add(rs.getInt("EmployeeId"));
                            }
                            for (Integer empId : employeeIds) {
                                if (empId != null && companyEmpSet.contains(empId)) {
                                    validEmployeeIds.add(empId);
                                }
                            }
                        }
                    }
                    if (validEmployeeIds.isEmpty()) {
                        throw new FieldValidationException("employeeIds", "Nhân viên được chọn không hợp lệ hoặc đã ngừng làm việc.");
                    }
                }

                long periodId;
                try (PreparedStatement ps = connection.prepareStatement("""
                        INSERT INTO dbo.DocumentUpdatePeriods (CompanyId, Title, StartsAt, EndsAt, ScopeType, CreatedByUserId)
                        VALUES (?, ?, ?, ?, ?, ?)
                        """, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setInt(1, companyId);
                    ps.setString(2, title);
                    ps.setTimestamp(3, Timestamp.valueOf(startsAt));
                    ps.setTimestamp(4, Timestamp.valueOf(endsAt));
                    ps.setString(5, scopeType);
                    ps.setInt(6, userId);
                    ps.executeUpdate();
                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        if (!keys.next()) {
                            throw new SQLException("Không thể tạo đợt cập nhật tài liệu.");
                        }
                        periodId = keys.getLong(1);
                    }
                }

                try (PreparedStatement ps = connection.prepareStatement(
                        "INSERT INTO dbo.DocumentUpdatePeriodTypes (DocumentPeriodId, DocumentTypeId) VALUES (?, ?)")) {
                    for (int typeId : validTypeIds) {
                        ps.setLong(1, periodId);
                        ps.setInt(2, typeId);
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }

                if ("SELECTED".equals(scopeType)) {
                    try (PreparedStatement ps = connection.prepareStatement(
                            "INSERT INTO dbo.DocumentUpdatePeriodEmployees (DocumentPeriodId, EmployeeId) VALUES (?, ?)")) {
                        for (int empId : validEmployeeIds) {
                            ps.setLong(1, periodId);
                            ps.setInt(2, empId);
                            ps.addBatch();
                        }
                        ps.executeBatch();
                    }
                }

                String scopeSummary = "ALL".equals(scopeType)
                        ? "Toàn công ty"
                        : (validEmployeeIds.size() + " nhân viên");
                audit(connection, companyId, userId, "OPEN_DOCUMENT_UPDATE_PERIOD", "DocumentUpdatePeriods", periodId,
                        null, title + " | Loại: " + String.join(", ", selectedTypeNames) + " | Phạm vi: " + scopeSummary
                                + " | " + startsAt + " - " + endsAt, ipAddress);
                connection.commit();
                return periodId;
            } catch (SQLException | RuntimeException e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    public void closePeriod(int companyId, int userId, long periodId, String ipAddress) throws SQLException {
        try (Connection connection = DBConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                ensureSchemaAndCompanyDefaults(connection, companyId);
                requireAdministrator(connection, companyId, userId);
                try (PreparedStatement ps = connection.prepareStatement("""
                        UPDATE dbo.DocumentUpdatePeriods
                        SET ClosedAt = SYSUTCDATETIME()
                        WHERE DocumentPeriodId = ? AND CompanyId = ? AND ClosedAt IS NULL
                        """)) {
                    ps.setLong(1, periodId);
                    ps.setInt(2, companyId);
                    if (ps.executeUpdate() == 0) {
                        throw new SecurityException("Đợt cập nhật tài liệu không tồn tại hoặc đã đóng.");
                    }
                }
                audit(connection, companyId, userId, "CLOSE_DOCUMENT_UPDATE_PERIOD", "DocumentUpdatePeriods", periodId,
                        "Open", "Closed", ipAddress);
                connection.commit();
            } catch (SQLException | RuntimeException e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    public ActiveEmployeeDocumentContext getActiveContextForEmployee(int companyId, int employeeId) throws SQLException {
        try (Connection connection = DBConnection.getConnection()) {
            return getActiveContextForEmployee(connection, companyId, employeeId, false);
        }
    }

    public ActiveEmployeeDocumentContext getActiveContextForEmployee(Connection connection, int companyId, int employeeId, boolean lock) throws SQLException {
        ensureSchemaAndCompanyDefaults(connection, companyId);
        String sql = """
                SELECT p.DocumentPeriodId, p.Title, p.StartsAt, p.EndsAt, p.ScopeType
                FROM dbo.DocumentUpdatePeriods p
                """ + (lock ? "WITH (UPDLOCK, HOLDLOCK) " : "") + """
                WHERE p.CompanyId = ?
                  AND p.ClosedAt IS NULL
                  AND p.StartsAt <= SYSUTCDATETIME()
                  AND p.EndsAt > SYSUTCDATETIME()
                  AND (
                      p.ScopeType = 'ALL'
                      OR EXISTS (
                          SELECT 1 FROM dbo.DocumentUpdatePeriodEmployees pe
                          WHERE pe.DocumentPeriodId = p.DocumentPeriodId AND pe.EmployeeId = ?
                      )
                  )
                ORDER BY p.EndsAt DESC, p.DocumentPeriodId DESC
                """;

        List<Map<String, Object>> activePeriods = new ArrayList<>();
        Set<String> allowedTypes = new LinkedHashSet<>();
        long primaryPeriodId = 0;
        String primaryTitle = null;
        LocalDateTime earliestStart = null;
        LocalDateTime latestEnd = null;

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, companyId);
            ps.setInt(2, employeeId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    long pid = rs.getLong("DocumentPeriodId");
                    String title = rs.getString("Title");
                    LocalDateTime startsAt = rs.getTimestamp("StartsAt").toLocalDateTime();
                    LocalDateTime endsAt = rs.getTimestamp("EndsAt").toLocalDateTime();
                    if (primaryPeriodId == 0) {
                        primaryPeriodId = pid;
                        primaryTitle = title;
                        earliestStart = startsAt;
                        latestEnd = endsAt;
                    } else if (endsAt.isAfter(latestEnd)) {
                        latestEnd = endsAt;
                    }
                    List<String> periodTypeNames = new ArrayList<>();
                    try (PreparedStatement tps = connection.prepareStatement("""
                            SELECT dt.TypeName
                            FROM dbo.DocumentUpdatePeriodTypes pt
                            JOIN dbo.CompanyDocumentTypes dt ON dt.DocumentTypeId = pt.DocumentTypeId
                            WHERE pt.DocumentPeriodId = ?
                            ORDER BY dt.DocumentTypeId ASC
                            """)) {
                        tps.setLong(1, pid);
                        try (ResultSet trs = tps.executeQuery()) {
                            while (trs.next()) {
                                String tn = trs.getString("TypeName");
                                periodTypeNames.add(tn);
                                allowedTypes.add(tn);
                            }
                        }
                    }
                    Map<String, Object> pMap = new LinkedHashMap<>();
                    pMap.put("periodId", pid);
                    pMap.put("title", title);
                    pMap.put("startsAt", startsAt.atOffset(ZoneOffset.UTC).toString());
                    pMap.put("endsAt", endsAt.atOffset(ZoneOffset.UTC).toString());
                    pMap.put("documentTypes", periodTypeNames);
                    activePeriods.add(pMap);
                }
            }
        }

        if (activePeriods.isEmpty()) {
            return null;
        }

        String combinedTitle = activePeriods.size() == 1
                ? primaryTitle
                : activePeriods.stream().map(m -> String.valueOf(m.get("title"))).distinct().reduce((a, b) -> a + ", " + b).orElse(primaryTitle);

        return new ActiveEmployeeDocumentContext(
                primaryPeriodId,
                combinedTitle,
                earliestStart,
                latestEnd,
                new ArrayList<>(allowedTypes),
                activePeriods
        );
    }

    public ActiveEmployeeDocumentContext requireAvailablePeriodForUpload(Connection connection, int companyId, int employeeId, String documentType) throws SQLException {
        OrganizationTransaction.lockCompany(connection, companyId);
        ActiveEmployeeDocumentContext ctx = getActiveContextForEmployee(connection, companyId, employeeId, true);
        if (ctx == null) {
            throw new SecurityException("Không có đợt cập nhật tài liệu đang mở dành cho bạn. Bạn chưa được thay đổi tài liệu.");
        }
        if (documentType != null && !ctx.allowedDocumentTypes().isEmpty()) {
            String trimmed = documentType.trim();
            boolean matched = ctx.allowedDocumentTypes().stream().anyMatch(t -> t.equalsIgnoreCase(trimmed));
            if (!matched) {
                throw new FieldValidationException("documentType",
                        "Loại tài liệu không nằm trong đợt cập nhật hiện tại (" + String.join(", ", ctx.allowedDocumentTypes()) + ").");
            }
            // Find the specific periodId that includes this documentType
            for (Map<String, Object> p : ctx.activePeriods()) {
                @SuppressWarnings("unchecked")
                List<String> pTypes = (List<String>) p.get("documentTypes");
                if (pTypes != null && pTypes.stream().anyMatch(t -> t.equalsIgnoreCase(trimmed))) {
                    long matchedPeriodId = (Long) p.get("periodId");
                    String matchedTitle = (String) p.get("title");
                    return new ActiveEmployeeDocumentContext(
                            matchedPeriodId,
                            matchedTitle,
                            ctx.startsAt(),
                            ctx.endsAt(),
                            ctx.allowedDocumentTypes(),
                            ctx.activePeriods()
                    );
                }
            }
        }
        return ctx;
    }

    private void requireAdministrator(Connection connection, int companyId, int userId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT u.UserId FROM dbo.Users u JOIN dbo.Roles r ON r.RoleId = u.RoleId
                WHERE u.UserId = ? AND u.CompanyId = ? AND u.IsActive = 1 AND u.IsLocked = 0
                  AND r.RoleCode IN ('COMPANY_ADMIN', 'HR')
                """)) {
            statement.setInt(1, userId);
            statement.setInt(2, companyId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    throw new SecurityException("Bạn không có quyền quản lý đợt cập nhật tài liệu của công ty này.");
                }
            }
        }
    }

    private void audit(Connection connection, int companyId, int userId, String action, String tableName,
                       long recordId, String oldValues, String newValues, String ipAddress) throws SQLException {
        AuditLog log = new AuditLog();
        log.setCompanyId(companyId);
        log.setUserId(userId);
        log.setAction(action);
        log.setTableName(tableName);
        log.setRecordId(String.valueOf(recordId));
        log.setOldValues(oldValues);
        log.setNewValues(newValues);
        log.setIpAddress(ipAddress);
        new AuditLogDAO().insert(connection, log);
    }
}

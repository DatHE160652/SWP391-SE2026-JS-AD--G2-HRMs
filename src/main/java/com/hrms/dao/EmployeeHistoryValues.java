package com.hrms.dao;

import java.sql.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class EmployeeHistoryValues {
    private static final Pattern ASSIGNMENT = Pattern.compile(
            "\"?(DepartmentId|PositionId|ManagerId)\"?\\s*[:=]\\s*(null|-|[0-9]+)", Pattern.CASE_INSENSITIVE);
    private static final List<String> FIELDS = List.of("DepartmentId", "PositionId", "ManagerId");
    private final Connection connection;
    private final Integer companyId;
    private final Map<String,String> names = new HashMap<>();

    EmployeeHistoryValues(Connection connection, Integer companyId) {
        this.connection = connection;
        this.companyId = companyId;
    }

    Values format(String oldValue, String newValue) throws SQLException {
        Map<String,String> before = parse(oldValue);
        Map<String,String> after = parse(newValue);
        if (before == null || after == null || before.isEmpty() && after.isEmpty()) return new Values(oldValue, newValue);
        List<String> oldLines = new ArrayList<>();
        List<String> newLines = new ArrayList<>();
        for (String field : FIELDS) {
            if (!(before.containsKey(field) || after.containsKey(field)) || Objects.equals(before.get(field), after.get(field))) continue;
            oldLines.add(label(field) + ": " + resolve(field, before.get(field)));
            newLines.add(label(field) + ": " + resolve(field, after.get(field)));
        }
        return oldLines.isEmpty() ? new Values("Không thay đổi", "Không thay đổi")
                : new Values(String.join("\n", oldLines), String.join("\n", newLines));
    }

    private Map<String,String> parse(String value) {
        Map<String,String> fields = new HashMap<>();
        if (value == null || value.isBlank() || "-".equals(value.trim())) return fields;
        Matcher matcher = ASSIGNMENT.matcher(value);
        while (matcher.find()) {
            String field = FIELDS.stream().filter(candidate -> candidate.equalsIgnoreCase(matcher.group(1))).findFirst().orElseThrow();
            String identifier = matcher.group(2);
            fields.put(field, "null".equalsIgnoreCase(identifier) || "-".equals(identifier) ? null : identifier);
        }
        String remainder = matcher.replaceAll("").replaceAll("[\\s{},;]", "");
        return !fields.isEmpty() && remainder.isEmpty() ? fields : null;
    }

    private String label(String field) {
        return switch (field) {
            case "DepartmentId" -> "Phòng ban";
            case "PositionId" -> "Chức vụ";
            default -> "Quản lý trực tiếp";
        };
    }

    private String resolve(String field, String identifier) throws SQLException {
        if (identifier == null) return switch (field) {
            case "DepartmentId" -> "Chưa có phòng ban";
            case "PositionId" -> "Chưa có chức vụ";
            default -> "Chưa có người quản lý";
        };
        String key = field + ":" + identifier;
        if (names.containsKey(key)) return names.get(key);
        int id;
        try { id = Integer.parseInt(identifier); }
        catch (NumberFormatException exception) { return label(field) + " không còn khả dụng (ID: " + identifier + ")"; }
        String sql = switch (field) {
            case "DepartmentId" -> "SELECT DepartmentName AS DisplayName FROM Departments WHERE CompanyId=? AND DepartmentId=?";
            case "PositionId" -> "SELECT PositionTitle AS DisplayName FROM Positions WHERE CompanyId=? AND PositionId=?";
            default -> "SELECT CONCAT(FullName,N' (',EmployeeCode,N')') AS DisplayName FROM Employees WHERE CompanyId=? AND EmployeeId=?";
        };
        String name = label(field) + " không còn khả dụng (ID: " + identifier + ")";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, companyId, Types.INTEGER);
            statement.setInt(2, id);
            try (ResultSet rows = statement.executeQuery()) { if (rows.next()) name = rows.getString("DisplayName"); }
        }
        names.put(key, name);
        return name;
    }

    record Values(String oldValue, String newValue) {}
}

package com.hrms.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class EmployeeHistoryDisplay {
    private static final Pattern REFERENCE = Pattern.compile("^\\s*(DocumentId|UserId|ContractId|ChangeId)\\s*[:=]\\s*([0-9]+)\\s*(?:;\\s*(.*))?$", Pattern.DOTALL);
    private final Connection connection;
    private final int companyId;
    private final int employeeId;
    private final Map<String,String> names = new HashMap<>();

    EmployeeHistoryDisplay(Connection connection, int companyId, int employeeId) {
        this.connection = connection;
        this.companyId = companyId;
        this.employeeId = employeeId;
    }

    EmployeeHistoryValues.Values format(String oldValue, String newValue) throws SQLException {
        return new EmployeeHistoryValues.Values(display(oldValue), display(newValue));
    }

    private String display(String value) throws SQLException {
        if (value == null) return null;
        Matcher reference = REFERENCE.matcher(value);
        if (reference.matches()) {
            String field = reference.group(1);
            String suffix = reference.group(3);
            String name = resolve(field, reference.group(2));
            if (suffix != null && !suffix.isBlank() && !name.endsWith(": " + suffix)) {
                return name + "\n" + status(suffix);
            }
            return name;
        }
        return status(value).replaceAll(" \\(ID: [0-9]+\\)", "");
    }

    private String resolve(String field, String identifier) throws SQLException {
        String key = field + ":" + identifier;
        if (names.containsKey(key)) return names.get(key);
        String label = switch (field) {
            case "DocumentId" -> "Tài liệu";
            case "UserId" -> "Tài khoản";
            case "ContractId" -> "Hợp đồng";
            default -> "Đề xuất chức danh";
        };
        String sql = switch (field) {
            case "DocumentId" -> """
                    SELECT document.DocumentName AS DisplayName FROM EmployeeDocuments document
                    JOIN Employees employee ON employee.EmployeeId=document.EmployeeId
                    WHERE employee.CompanyId=? AND employee.EmployeeId=? AND document.DocumentId=?
                    """;
            case "UserId" -> """
                    SELECT COALESCE(NULLIF(account.Email,''),NULLIF(account.Username,''),account.DisplayName) AS DisplayName
                    FROM Users account JOIN Employees employee ON employee.CompanyId=account.CompanyId AND employee.UserId=account.UserId
                    WHERE employee.CompanyId=? AND employee.EmployeeId=? AND account.UserId=?
                    """;
            case "ContractId" -> "SELECT ContractCode AS DisplayName FROM EmploymentContracts WHERE CompanyId=? AND EmployeeId=? AND ContractId=?";
            default -> """
                    SELECT positionRow.PositionTitle AS DisplayName FROM EmployeePositionChanges changeRow
                    JOIN Positions positionRow ON positionRow.CompanyId=changeRow.CompanyId AND positionRow.PositionId=changeRow.NewPositionId
                    WHERE changeRow.CompanyId=? AND changeRow.EmployeeId=? AND changeRow.PositionChangeId=?
                    """;
        };
        String name = label + " không còn khả dụng";
        try {
            int referenceId = Integer.parseInt(identifier);
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setInt(1, companyId);
                statement.setInt(2, employeeId);
                statement.setInt(3, referenceId);
                try (ResultSet rows = statement.executeQuery()) {
                    if (rows.next() && rows.getString("DisplayName") != null) name = label + ": " + rows.getString("DisplayName");
                }
            }
        } catch (NumberFormatException ignored) {
            name = label + " không còn khả dụng";
        }
        names.put(key, name);
        return name;
    }

    private String status(String value) {
        return switch (value) {
            case "Active", "ACTIVE" -> "Đang hoạt động";
            case "Inactive", "INACTIVE" -> "Ngừng hoạt động";
            case "DRAFT" -> "Bản nháp";
            case "PENDING" -> "Chờ duyệt";
            case "APPROVED" -> "Đã duyệt";
            case "REJECTED" -> "Đã từ chối";
            case "APPLIED" -> "Đã áp dụng";
            case "EXPIRED" -> "Hết hạn";
            case "TERMINATED" -> "Đã chấm dứt";
            default -> value;
        };
    }
}

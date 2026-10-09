package com.hrms.service;

import com.hrms.dao.EmployeeDAO;
import com.hrms.dao.UserDAO;
import com.hrms.model.Employee;
import com.hrms.model.User;
import com.hrms.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.Normalizer;
import java.security.SecureRandom;
import java.util.List;
import java.util.Locale;

public class EmployeeService {
    private static final String DEFAULT_EMPLOYEE_PASSWORD = "1";

    private final EmployeeDAO employeeDAO = new EmployeeDAO();
    private final UserDAO userDAO = new UserDAO();
    private final BCryptPasswordHasher passwordHasher = new BCryptPasswordHasher();
    private final SecureRandom random = new SecureRandom();

    public List<Employee> getAllEmployees() throws SQLException {
        return employeeDAO.getAllEmployees();
    }

    public Employee getEmployeeById(int employeeId) throws SQLException {
        return employeeDAO.getEmployeeById(employeeId);
    }

    public boolean createEmployee(Employee employee) throws SQLException {
        validate(employee, true);
        validateUniquePhone(employee);

        try (Connection connection = DBConnection.getConnection()) {
            ensureEmployeeCreationSchema(connection);
            connection.setAutoCommit(false);

            try {
                CompanyInfo company = getOrCreateDefaultCompany(connection);
                String companyPrefix = companyPrefix(company.companyName());
                String randomDigits = generateUniqueRandomDigits(connection, company.companyId(), companyPrefix,
                        employee.getFullName());
                employee.setEmployeeCode(companyPrefix.toUpperCase(Locale.ROOT) + randomDigits);

                int employeeId = employeeDAO.insertEmployee(connection, employee, company.companyId());
                employee.setEmployeeId(employeeId);
                String accountLocalPart = buildAccountLocalPart(employee.getFullName(), randomDigits);
                String email = accountLocalPart + "@" + companyPrefix + ".hrms";

                User user = new User();
                user.setCompanyId(company.companyId());
                user.setUsername(generateAvailableUsername(connection, accountLocalPart));
                user.setDisplayName(employee.getFullName());
                user.setEmail(email);
                user.setPasswordHash(passwordHasher.hash(DEFAULT_EMPLOYEE_PASSWORD));

                int userId = userDAO.createEmployeeAccount(connection, user);
                boolean linked = employeeDAO.updateEmployeeUserId(connection, employeeId, userId);
                if (!linked) {
                    throw new SQLException("Linking generated user account to employee failed.");
                }

                connection.commit();
                return true;
            } catch (SQLException | RuntimeException e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    public boolean updateEmployee(Employee employee) throws SQLException {
        validate(employee, false);
        validateUniquePhone(employee);
        return employeeDAO.updateEmployee(employee);
    }

    public boolean deleteEmployee(int employeeId) throws SQLException {
        return employeeDAO.deleteEmployee(employeeId);
    }

    private void validate(Employee employee, boolean creating) {
        if (!creating && employee.getEmployeeId() <= 0) {
            throw new IllegalArgumentException("Employee id is required.");
        }
        if (employee.getFullName() == null || employee.getFullName().isBlank()) {
            throw new IllegalArgumentException("Full name is required.");
        }
        if (employee.getPhone() == null || employee.getPhone().isBlank()) {
            throw new IllegalArgumentException("Phone is required.");
        }
        if (employee.getGender() == null || employee.getGender().isBlank()) {
            throw new IllegalArgumentException("Gender is required.");
        }
        if (employee.getDateOfBirth() == null) {
            throw new IllegalArgumentException("Date of birth is required.");
        }
        if (employee.getStartDate() == null) {
            throw new IllegalArgumentException("Start date is required.");
        }
        if (employee.getBaseSalary() == null) {
            throw new IllegalArgumentException("Base salary is required.");
        }
        if (employee.getStatus() == null || employee.getStatus().isBlank()) {
            throw new IllegalArgumentException("Status is required.");
        }
    }

    private void validateUniquePhone(Employee employee) throws SQLException {
        Integer excludeEmployeeId = employee.getEmployeeId() > 0 ? employee.getEmployeeId() : null;
        if (employeeDAO.phoneExists(employee.getPhone().trim(), excludeEmployeeId)) {
            throw new IllegalArgumentException("Phone number already exists.");
        }
    }

    private String generateAvailableUsername(Connection connection, String fullName) throws SQLException {
        String baseUsername = toUsernameBase(fullName);
        String username = baseUsername;
        int suffix = 1;

        while (userDAO.usernameExists(connection, username)) {
            username = baseUsername + String.format("%02d", suffix);
            suffix++;
        }

        return username;
    }

    private String toUsernameBase(String fullName) {
        String normalized = Normalizer.normalize(fullName, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]", "");

        return normalized.isBlank() ? "employee" : normalized;
    }

    private String buildAccountLocalPart(String fullName, String randomDigits) {
        String[] parts = normalizedWords(fullName);
        String givenName = parts.length == 0 ? "employee" : parts[parts.length - 1];
        String familyInitial = parts.length >= 1 ? parts[0].substring(0, 1) : "x";
        String middleInitial = parts.length >= 3 ? parts[1].substring(0, 1) : "x";
        return givenName + familyInitial + middleInitial + randomDigits;
    }

    private String generateUniqueRandomDigits(Connection connection, int companyId, String companyPrefix, String fullName)
            throws SQLException {
        for (int attempt = 0; attempt < 30; attempt++) {
            String digits = String.format("%06d", random.nextInt(1_000_000));
            String employeeCode = companyPrefix.toUpperCase(Locale.ROOT) + digits;
            String email = buildAccountLocalPart(fullName, digits) + "@" + companyPrefix + ".hrms";
            if (!employeeCodeExists(connection, companyId, employeeCode) && !userDAO.emailExists(connection, email)) {
                return digits;
            }
        }
        throw new SQLException("Could not generate a unique employee code. Please try again.");
    }

    private boolean employeeCodeExists(Connection connection, int companyId, String employeeCode) throws SQLException {
        String sql = "SELECT COUNT(*) FROM Employees WHERE CompanyId = ? AND EmployeeCode = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, companyId);
            statement.setString(2, employeeCode);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() && resultSet.getInt(1) > 0;
            }
        }
    }

    private String companyPrefix(String companyName) {
        String normalized = normalizeAscii(companyName).replaceAll("[^a-z0-9]", "");
        if (normalized.length() >= 3) {
            return normalized.substring(0, 3);
        }
        return (normalized + "xxx").substring(0, 3);
    }

    private String[] normalizedWords(String value) {
        String normalized = normalizeAscii(value).replaceAll("[^a-z0-9 ]", " ").trim();
        return normalized.isBlank() ? new String[0] : normalized.split("\\s+");
    }

    private String normalizeAscii(String value) {
        return Normalizer.normalize(value == null ? "" : value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT);
    }

    private CompanyInfo getOrCreateDefaultCompany(Connection connection) throws SQLException {
        String selectSql = "SELECT TOP 1 CompanyId, CompanyName FROM Companies ORDER BY CompanyId";

        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(selectSql)) {
            if (resultSet.next()) {
                return new CompanyInfo(resultSet.getInt("CompanyId"), resultSet.getString("CompanyName"));
            }
        }

        String insertSql = """
                INSERT INTO Companies (WorkspaceCode, CompanyName, CompanyNameNormalized, Region, ContactEmail)
                VALUES (?, ?, ?, ?, ?)
                """;

        try (PreparedStatement statement = connection.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, "demo");
            statement.setString(2, "Demo Company");
            statement.setString(3, "demo company");
            statement.setString(4, "NORTH");
            statement.setString(5, "admin@hrms.local");
            statement.executeUpdate();

            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return new CompanyInfo(generatedKeys.getInt(1), "Demo Company");
                }
            }
        }

        throw new SQLException("Default company could not be created.");
    }

    private void ensureEmployeeCreationSchema(Connection connection) throws SQLException {
        makeColumnNullableIfNeeded(connection, "Employees", "UserId");
        makeColumnNullableIfNeeded(connection, "Employees", "DepartmentId");
        makeColumnNullableIfNeeded(connection, "Employees", "PositionId");
    }

    private void makeColumnNullableIfNeeded(Connection connection, String tableName, String columnName)
            throws SQLException {
        String metadataSql = """
                SELECT IS_NULLABLE, DATA_TYPE
                FROM INFORMATION_SCHEMA.COLUMNS
                WHERE TABLE_NAME = ? AND COLUMN_NAME = ?
                """;

        try (PreparedStatement statement = connection.prepareStatement(metadataSql)) {
            statement.setString(1, tableName);
            statement.setString(2, columnName);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next() || "YES".equalsIgnoreCase(resultSet.getString("IS_NULLABLE"))) {
                    return;
                }
            }
        }

        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("ALTER TABLE " + tableName + " ALTER COLUMN " + columnName + " INT NULL");
        }
    }

    private record CompanyInfo(int companyId, String companyName) {
    }
}

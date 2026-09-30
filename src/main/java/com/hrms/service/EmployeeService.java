package com.hrms.service;

import com.hrms.dao.EmployeeDAO;
import com.hrms.dao.UserDAO;
import com.hrms.model.Employee;
import com.hrms.model.User;
import com.hrms.util.DBConnection;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.Normalizer;
import java.util.List;
import java.util.Locale;

public class EmployeeService {
    private static final String DEFAULT_PASSWORD_HASH = "CHANGE_ME";

    private final EmployeeDAO employeeDAO = new EmployeeDAO();
    private final UserDAO userDAO = new UserDAO();

    public List<Employee> getAllEmployees() throws SQLException {
        return employeeDAO.getAllEmployees();
    }

    public Employee getEmployeeById(int employeeId) throws SQLException {
        return employeeDAO.getEmployeeById(employeeId);
    }

    public boolean createEmployee(Employee employee) throws SQLException {
        normalize(employee);

        try (Connection connection = DBConnection.getConnection()) {
            ensureEmployeeCreationSchema(connection);
            connection.setAutoCommit(false);

            try {
                int companyId = getOrCreateDefaultCompanyId(connection);
                int employeeId = employeeDAO.insertEmployee(connection, employee, companyId);
                String username = generateAvailableUsername(connection, employee.getFullName());

                User user = new User();
                user.setCompanyId(companyId);
                user.setUsername(username);
                user.setEmail(buildGeneratedEmail(username));
                user.setPasswordHash(DEFAULT_PASSWORD_HASH);

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
        normalize(employee);
        return employeeDAO.updateEmployee(employee);
    }

    public boolean deleteEmployee(int employeeId) throws SQLException {
        return employeeDAO.deleteEmployee(employeeId);
    }

    private void normalize(Employee employee) {
        if (employee.getEmployeeCode() == null || employee.getEmployeeCode().isBlank()) {
            throw new IllegalArgumentException("Employee code is required.");
        }
        if (employee.getFullName() == null || employee.getFullName().isBlank()) {
            throw new IllegalArgumentException("Full name is required.");
        }
        if (employee.getStartDate() == null) {
            throw new IllegalArgumentException("Start date is required.");
        }
        if (employee.getBaseSalary() == null) {
            employee.setBaseSalary(BigDecimal.ZERO);
        }
        if (employee.getStatus() == null || employee.getStatus().isBlank()) {
            employee.setStatus("Active");
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

    private String buildGeneratedEmail(String username) {
        return username + "@hrms.local";
    }

    private int getOrCreateDefaultCompanyId(Connection connection) throws SQLException {
        String selectSql = "SELECT TOP 1 CompanyId FROM Companies ORDER BY CompanyId";

        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(selectSql)) {
            if (resultSet.next()) {
                return resultSet.getInt("CompanyId");
            }
        }

        String insertSql = """
                INSERT INTO Companies (WorkspaceCode, CompanyName, ContactEmail)
                VALUES (?, ?, ?)
                """;

        try (PreparedStatement statement = connection.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, "demo");
            statement.setString(2, "Demo Company");
            statement.setString(3, "admin@hrms.local");
            statement.executeUpdate();

            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getInt(1);
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
}

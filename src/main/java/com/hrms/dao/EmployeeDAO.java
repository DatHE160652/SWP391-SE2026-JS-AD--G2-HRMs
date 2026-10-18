package com.hrms.dao;

import com.hrms.model.Employee;
import com.hrms.util.DBConnection;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

public class EmployeeDAO {
    public List<Employee> getAllEmployees() throws SQLException {
        String sql = """
                SELECT e.EmployeeId, e.EmployeeCode, e.FullName, e.Phone, e.Gender,
                       e.DateOfBirth, e.IdentityNumber, e.Status,
                       u.UserId, u.Username, u.Email AS AccountEmail, u.IsActive AS AccountActive
                FROM Employees e
                LEFT JOIN Users u ON e.UserId = u.UserId
                ORDER BY e.EmployeeId DESC
                """;

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            List<Employee> employees = new ArrayList<>();
            while (resultSet.next()) {
                employees.add(mapEmployee(resultSet));
            }
            return employees;
        }
    }

    public Employee getEmployeeById(int employeeId) throws SQLException {
        String sql = """
                SELECT e.EmployeeId, e.EmployeeCode, e.FullName, e.Phone, e.Gender,
                       e.DateOfBirth, e.IdentityNumber, e.Status,
                       u.UserId, u.Username, u.Email AS AccountEmail, u.IsActive AS AccountActive
                FROM Employees e
                LEFT JOIN Users u ON e.UserId = u.UserId
                WHERE e.EmployeeId = ?
                """;

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, employeeId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? mapEmployee(resultSet) : null;
            }
        }
    }

    public int insertEmployee(Connection connection, Employee employee, int companyId) throws SQLException {
        String sql = """
                INSERT INTO Employees (
                    CompanyId, EmployeeCode, FullName, Phone, Gender,
                    DateOfBirth, IdentityNumber, Status
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setInt(1, companyId);
            statement.setString(2, employee.getEmployeeCode());
            statement.setString(3, employee.getFullName());
            statement.setString(4, employee.getPhone());
            statement.setString(5, employee.getGender());
            setNullableDate(statement, 6, employee.getDateOfBirth());
            statement.setString(7, employee.getIdentityNumber());
            statement.setString(8, employee.getStatus());

            int affectedRows = statement.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Creating employee failed.");
            }

            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getInt(1);
                }
            }

            throw new SQLException("Creating employee failed, no generated EmployeeId returned.");
        }
    }

    public boolean updateEmployeeUserId(Connection connection, int employeeId, int userId) throws SQLException {
        String sql = "UPDATE Employees SET UserId = ?, UpdatedAt = SYSUTCDATETIME() WHERE EmployeeId = ?";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, userId);
            statement.setInt(2, employeeId);
            return statement.executeUpdate() > 0;
        }
    }

    public boolean updateEmployee(Employee employee) throws SQLException {
        String sql = """
                UPDATE Employees
                SET FullName = ?,
                    Phone = ?,
                    Gender = ?,
                    DateOfBirth = ?,
                    IdentityNumber = ?,
                    Status = ?,
                    UpdatedAt = SYSUTCDATETIME()
                WHERE EmployeeId = ?
                """;

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, employee.getFullName());
            statement.setString(2, employee.getPhone());
            statement.setString(3, employee.getGender());
            setNullableDate(statement, 4, employee.getDateOfBirth());
            statement.setString(5, employee.getIdentityNumber());
            statement.setString(6, employee.getStatus());
            statement.setInt(7, employee.getEmployeeId());
            return statement.executeUpdate() > 0;
        }
    }

    public boolean phoneExists(String phone, Integer excludeEmployeeId) throws SQLException {
        String sql = """
                SELECT COUNT(*)
                FROM Employees
                WHERE Phone = ?
                  AND (? IS NULL OR EmployeeId <> ?)
                """;

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, phone);
            if (excludeEmployeeId == null) {
                statement.setNull(2, Types.INTEGER);
                statement.setNull(3, Types.INTEGER);
            } else {
                statement.setInt(2, excludeEmployeeId);
                statement.setInt(3, excludeEmployeeId);
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() && resultSet.getInt(1) > 0;
            }
        }
    }

    public boolean deleteEmployee(int employeeId) throws SQLException {
        String sql = "DELETE FROM Employees WHERE EmployeeId = ?";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, employeeId);
            return statement.executeUpdate() > 0;
        }
    }

    private Employee mapEmployee(ResultSet resultSet) throws SQLException {
        Employee employee = new Employee();
        employee.setEmployeeId(resultSet.getInt("EmployeeId"));
        employee.setEmployeeCode(resultSet.getString("EmployeeCode"));
        employee.setFullName(resultSet.getString("FullName"));
        employee.setPhone(resultSet.getString("Phone"));
        employee.setGender(resultSet.getString("Gender"));
        employee.setDateOfBirth(resultSet.getDate("DateOfBirth"));
        employee.setIdentityNumber(resultSet.getString("IdentityNumber"));
        employee.setStatus(resultSet.getString("Status"));
        int userId = resultSet.getInt("UserId");
        employee.setUserId(resultSet.wasNull() ? null : userId);
        employee.setUsername(resultSet.getString("Username"));
        employee.setAccountEmail(resultSet.getString("AccountEmail"));
        boolean accountActive = resultSet.getBoolean("AccountActive");
        employee.setAccountActive(resultSet.wasNull() ? null : accountActive);
        return employee;
    }

    private void setNullableDate(PreparedStatement statement, int index, Date value) throws SQLException {
        if (value == null) {
            statement.setNull(index, Types.DATE);
        } else {
            statement.setDate(index, value);
        }
    }

}

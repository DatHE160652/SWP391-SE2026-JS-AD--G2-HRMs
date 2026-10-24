package com.hrms.dao;

import com.hrms.model.Department;
import com.hrms.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class DepartmentDAO {
    public List<Department> getAllDepartments(int companyId) throws SQLException {
        String sql = """
                SELECT DepartmentId, CompanyId, DepartmentCode, DepartmentName, IsActive, CreatedAt
                FROM Departments
                WHERE CompanyId = ?
                ORDER BY DepartmentId DESC
                """;

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, companyId);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<Department> departments = new ArrayList<>();
                while (resultSet.next()) {
                    departments.add(mapDepartment(resultSet));
                }
                return departments;
            }
        }
    }

    public Department getDepartmentById(int companyId, int id) throws SQLException {
        String sql = """
                SELECT DepartmentId, CompanyId, DepartmentCode, DepartmentName, IsActive, CreatedAt
                FROM Departments
                WHERE DepartmentId = ? AND CompanyId = ?
                """;

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            statement.setInt(2, companyId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? mapDepartment(resultSet) : null;
            }
        }
    }

    public boolean insertDepartment(Department department) throws SQLException {
        String sql = "INSERT INTO Departments (CompanyId, DepartmentCode, DepartmentName) VALUES (?, ?, ?)";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, department.getCompanyId());
            statement.setString(2, department.getDepartmentCode());
            statement.setString(3, department.getDepartmentName());
            return statement.executeUpdate() > 0;
        }
    }

    public boolean updateDepartment(Department department) throws SQLException {
        String sql = """
                UPDATE Departments
                SET DepartmentCode = ?, DepartmentName = ?
                WHERE DepartmentId = ? AND CompanyId = ?
                """;

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, department.getDepartmentCode());
            statement.setString(2, department.getDepartmentName());
            statement.setInt(3, department.getDepartmentId());
            statement.setInt(4, department.getCompanyId());
            return statement.executeUpdate() > 0;
        }
    }

    public boolean setDepartmentActive(int companyId, int id, boolean active) throws SQLException {
        return OrganizationTransaction.execute(companyId, connection -> {
            try (PreparedStatement statement = connection.prepareStatement(
                    "UPDATE Departments SET IsActive=? WHERE CompanyId=? AND DepartmentId=?")) {
                statement.setBoolean(1, active);
                statement.setInt(2, companyId);
                statement.setInt(3, id);
                return statement.executeUpdate() == 1;
            }
        });
    }

    public boolean departmentCodeExists(int companyId, String departmentCode, Integer excludeDepartmentId)
            throws SQLException {
        String sql = """
                SELECT COUNT(*)
                FROM Departments
                WHERE CompanyId = ? AND DepartmentCode = ?
                  AND (? IS NULL OR DepartmentId <> ?)
                """;

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, companyId);
            statement.setString(2, departmentCode);
            if (excludeDepartmentId == null) {
                statement.setNull(3, java.sql.Types.INTEGER);
                statement.setNull(4, java.sql.Types.INTEGER);
            } else {
                statement.setInt(3, excludeDepartmentId);
                statement.setInt(4, excludeDepartmentId);
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() && resultSet.getInt(1) > 0;
            }
        }
    }

    public boolean companyExists(int companyId) throws SQLException {
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT 1 FROM Companies WHERE CompanyId=? AND IsActive=1")) {
            statement.setInt(1, companyId);
            try (ResultSet rows = statement.executeQuery()) { return rows.next(); }
        }
    }

    private Department mapDepartment(ResultSet resultSet) throws SQLException {
        Department department = new Department();
        department.setDepartmentId(resultSet.getInt("DepartmentId"));
        department.setCompanyId(resultSet.getInt("CompanyId"));
        department.setDepartmentCode(resultSet.getString("DepartmentCode"));
        department.setDepartmentName(resultSet.getString("DepartmentName"));
        department.setActive(resultSet.getBoolean("IsActive"));
        Timestamp createdAt = resultSet.getTimestamp("CreatedAt");
        department.setCreatedAt(createdAt == null ? null : createdAt.toLocalDateTime());
        return department;
    }
}

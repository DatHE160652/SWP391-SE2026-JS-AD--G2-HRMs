package com.hrms.service;

import com.hrms.dao.DepartmentDAO;
import com.hrms.model.Department;
import com.hrms.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

public class DepartmentService {
    private final DepartmentDAO departmentDAO = new DepartmentDAO();

    public List<Department> getDepartments() throws SQLException {
        return departmentDAO.getAllDepartments(getOrCreateDefaultCompanyId());
    }

    public Department getDepartmentDetail(int id) throws SQLException {
        return departmentDAO.getDepartmentById(id);
    }

    public boolean createDepartment(Department department) throws SQLException {
        department.setCompanyId(getOrCreateDefaultCompanyId());
        validate(department, null);
        return departmentDAO.insertDepartment(department);
    }

    public boolean updateDepartment(Department department) throws SQLException {
        Department existing = departmentDAO.getDepartmentById(department.getDepartmentId());
        if (existing == null) {
            throw new IllegalArgumentException("Department not found.");
        }
        department.setCompanyId(existing.getCompanyId());
        validate(department, department.getDepartmentId());
        return departmentDAO.updateDepartment(department);
    }

    public boolean deleteDepartment(int id) throws SQLException {
        return departmentDAO.deleteDepartment(id);
    }

    private void validate(Department department, Integer excludeDepartmentId) throws SQLException {
        if (department.getDepartmentCode() == null || department.getDepartmentCode().isBlank()) {
            throw new IllegalArgumentException("Department code is required.");
        }
        if (department.getDepartmentName() == null || department.getDepartmentName().isBlank()) {
            throw new IllegalArgumentException("Department name is required.");
        }
        if (departmentDAO.departmentCodeExists(department.getCompanyId(), department.getDepartmentCode(),
                excludeDepartmentId)) {
            throw new IllegalArgumentException("Department code already exists.");
        }
    }

    private int getOrCreateDefaultCompanyId() throws SQLException {
        try (Connection connection = DBConnection.getConnection()) {
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
        }

        throw new SQLException("Default company could not be created.");
    }
}

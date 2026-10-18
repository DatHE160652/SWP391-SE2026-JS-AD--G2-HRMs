package com.hrms.dao;

import com.hrms.model.Employee;
import com.hrms.util.DBConnection;
import org.junit.Test;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class EmployeeDAOIntegrationTest {
    private final EmployeeDAO employeeDAO = new EmployeeDAO();

    @Test
    public void readsEmployeesUsingIdentityNumberSchema() throws Exception {
        for (Employee employee : employeeDAO.getAllEmployees()) {
            Employee detail = employeeDAO.getEmployeeById(employee.getEmployeeId());
            assertNotNull(detail);
            assertEquals(employee.getIdentityNumber(), detail.getIdentityNumber());
        }
    }

    @Test
    public void storesIdentityNumberWithoutStartDateOrBaseSalary() throws Exception {
        try (Connection connection = DBConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                int companyId;
                try (PreparedStatement statement = connection.prepareStatement("SELECT TOP 1 CompanyId FROM Companies");
                     ResultSet resultSet = statement.executeQuery()) {
                    assertTrue("A company is required for this integration test", resultSet.next());
                    companyId = resultSet.getInt(1);
                }

                Employee employee = new Employee();
                employee.setEmployeeCode("TEST-" + UUID.randomUUID());
                employee.setFullName("Identity persistence test");
                employee.setPhone("0901234567");
                employee.setGender("Other");
                employee.setDateOfBirth(Date.valueOf("2000-01-01"));
                employee.setIdentityNumber("001234567890");
                employee.setStatus("Active");

                int employeeId = employeeDAO.insertEmployee(connection, employee, companyId);
                assertTrue(employeeId > 0);
                try (PreparedStatement statement = connection.prepareStatement(
                        "SELECT IdentityNumber FROM Employees WHERE EmployeeId = ?")) {
                    statement.setInt(1, employeeId);
                    try (ResultSet resultSet = statement.executeQuery()) {
                        assertTrue(resultSet.next());
                        assertEquals("001234567890", resultSet.getString("IdentityNumber"));
                    }
                }
            } finally {
                connection.rollback();
            }
        }
    }
}

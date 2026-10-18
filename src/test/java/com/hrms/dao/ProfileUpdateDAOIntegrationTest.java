package com.hrms.dao;

import com.hrms.service.FieldValidationException;
import com.hrms.util.DBConnection;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.junit.Assert.*;

public class ProfileUpdateDAOIntegrationTest {
    private final ProfileUpdateDAO dao = new ProfileUpdateDAO();
    private Connection connection;
    private int companyId;
    private int otherCompanyId;
    private int administratorId;
    private int otherAdministratorId;
    private int hrId;
    private int employeeUserId;
    private int employeeId;
    private int managerUserId;
    private int managerId;
    private int otherEmployeeUserId;
    private int otherEmployeeId;
    private LocalDateTime now;

    @Before
    public void setUp() throws Exception {
        connection = DBConnection.getConnection();
        connection.setAutoCommit(false);
        try (Statement statement = connection.createStatement()) {
            statement.execute("SET ANSI_NULLS ON; SET ANSI_PADDING ON; SET ANSI_WARNINGS ON; SET ARITHABORT ON; "
                    + "SET CONCAT_NULL_YIELDS_NULL ON; SET QUOTED_IDENTIFIER ON; SET NUMERIC_ROUNDABORT OFF;");
        }
        now = LocalDateTime.now(ZoneOffset.UTC);
        companyId = createCompany();
        otherCompanyId = createCompany();
        administratorId = createUser(companyId, "COMPANY_ADMIN");
        otherAdministratorId = createUser(otherCompanyId, "COMPANY_ADMIN");
        hrId = createUser(companyId, "HR");
        employeeUserId = createUser(companyId, "EMPLOYEE");
        managerUserId = createUser(companyId, "MANAGER");
        otherEmployeeUserId = createUser(otherCompanyId, "EMPLOYEE");
        employeeId = createEmployee(companyId, employeeUserId, "0901000001");
        managerId = createEmployee(companyId, managerUserId, "0901000002");
        otherEmployeeId = createEmployee(otherCompanyId, otherEmployeeUserId, "0901000003");
    }

    @After
    public void tearDown() throws Exception {
        if (connection != null) {
            try {
                connection.rollback();
            } finally {
                connection.close();
            }
        }
    }

    @Test
    public void noRequestScheduledExpiredOrClosedPeriodsNeverPermitUpdates() throws Exception {
        assertDenied();
        long scheduled = dao.create(connection, companyId, administratorId, "Scheduled", now.plusDays(1), now.plusDays(2), "test");
        assertDenied();
        dao.close(connection, companyId, administratorId, scheduled, "test");
        dao.create(connection, companyId, administratorId, "Expired", now.minusDays(2), now.minusDays(1), "test");
        assertDenied();
        long opened = openPeriod(administratorId);
        dao.close(connection, companyId, administratorId, opened, "test");
        assertDenied();
        assertEquals("0901000001", phone(employeeId));
    }

    @Test
    public void employeeAndManagerUpdateOnlyPhoneWithAtomicAudit() throws Exception {
        long periodId = openPeriod(hrId);
        String original = protectedFields(employeeId);
        dao.updatePhone(connection, companyId, employeeUserId, employeeId, "0902000001", "test");
        dao.updatePhone(connection, companyId, managerUserId, managerId, "0902000002", "test");
        assertEquals("0902000001", phone(employeeId));
        assertEquals("0902000002", phone(managerId));
        assertEquals(original, protectedFields(employeeId));
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT NewValues FROM AuditLogs WHERE CompanyId = ? AND Action = 'UPDATE_PHONE'")) {
            statement.setInt(1, companyId);
            try (ResultSet resultSet = statement.executeQuery()) {
                int count = 0;
                while (resultSet.next()) {
                    assertTrue(resultSet.getString(1).contains("PeriodId: " + periodId));
                    count++;
                }
                assertEquals(2, count);
            }
        }
    }

    @Test
    public void requestsAndEmployeeOwnershipAreIsolatedByCompany() throws Exception {
        long periodId = openPeriod(administratorId);
        assertThrows(SecurityException.class,
                () -> dao.updatePhone(connection, otherCompanyId, otherEmployeeUserId, otherEmployeeId, "0902000003", "test"));
        assertThrows(SecurityException.class,
                () -> dao.updatePhone(connection, companyId, otherEmployeeUserId, otherEmployeeId, "0902000003", "test"));
        assertThrows(SecurityException.class,
                () -> dao.updatePhone(connection, companyId, employeeUserId, managerId, "0902000003", "test"));
        assertThrows(SecurityException.class,
                () -> dao.close(connection, otherCompanyId, otherAdministratorId, periodId, "test"));
        assertEquals("0901000003", phone(otherEmployeeId));
    }

    @Test
    public void ordinaryEmployeesCannotCreatePeriodsAndOverlapsAreRejected() throws Exception {
        assertThrows(SecurityException.class, () -> openPeriod(employeeUserId));
        openPeriod(administratorId);
        FieldValidationException duplicate = assertThrows(FieldValidationException.class,
                () -> dao.create(connection, companyId, hrId, "Overlap", now, now.plusHours(2), "test"));
        assertEquals("startsAt", duplicate.getField());
        dao.create(connection, companyId, hrId, "Next", now.plusHours(1), now.plusHours(2), "test");
        dao.create(connection, otherCompanyId, otherAdministratorId, "Other company", now.minusHours(1), now.plusHours(1), "test");
    }

    @Test
    public void duplicatePhoneFailsWithoutChangingTheEmployee() throws Exception {
        openPeriod(administratorId);
        FieldValidationException exception = assertThrows(FieldValidationException.class,
                () -> dao.updatePhone(connection, companyId, employeeUserId, employeeId, "0901000002", "test"));
        assertEquals("phone", exception.getField());
        assertEquals("0901000001", phone(employeeId));
    }

    private long openPeriod(int userId) throws SQLException {
        return dao.create(connection, companyId, userId, "Open", now.minusHours(1), now.plusHours(1), "test");
    }

    private void assertDenied() {
        assertThrows(SecurityException.class,
                () -> dao.updatePhone(connection, companyId, employeeUserId, employeeId, "0902000001", "test"));
    }

    private int createCompany() throws SQLException {
        String code = "test" + UUID.randomUUID().toString().replace("-", "");
        return insert("INSERT INTO Companies (WorkspaceCode, CompanyName, CompanyNameNormalized, Region, ContactEmail) "
                + "VALUES (?, ?, ?, 'NORTH', ?)", code, code, code, code + "@example.test");
    }

    private int createUser(int company, String role) throws SQLException {
        String identifier = "test" + UUID.randomUUID().toString().replace("-", "");
        return insert("INSERT INTO Users (CompanyId, RoleId, Username, Email, DisplayName, PasswordHash, IsActive, IsLocked) "
                + "VALUES (?, (SELECT RoleId FROM Roles WHERE RoleCode = ?), ?, ?, ?, 'test-only', 1, 0)",
                company, role, identifier, identifier + "@example.test", identifier);
    }

    private int createEmployee(int company, int user, String phone) throws SQLException {
        return insert("INSERT INTO Employees (CompanyId, UserId, EmployeeCode, FullName, Phone, Gender, DateOfBirth, IdentityNumber, Status) "
                + "VALUES (?, ?, ?, 'Protected Name', ?, 'Other', '2000-01-01', '001234567890', 'Active')",
                company, user, "test" + UUID.randomUUID(), phone);
    }

    private int insert(String sql, Object... values) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            for (int index = 0; index < values.length; index++) {
                statement.setObject(index + 1, values[index]);
            }
            statement.executeUpdate();
            try (ResultSet resultSet = statement.getGeneratedKeys()) {
                assertTrue(resultSet.next());
                return resultSet.getInt(1);
            }
        }
    }

    private String phone(int id) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("SELECT Phone FROM Employees WHERE EmployeeId = ?")) {
            statement.setInt(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                assertTrue(resultSet.next());
                return resultSet.getString(1);
            }
        }
    }

    private String protectedFields(int id) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT FullName, Gender, DateOfBirth, IdentityNumber, EmployeeCode, Status FROM Employees WHERE EmployeeId = ?")) {
            statement.setInt(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                assertTrue(resultSet.next());
                return resultSet.getString(1) + resultSet.getString(2) + resultSet.getDate(3)
                        + resultSet.getString(4) + resultSet.getString(5) + resultSet.getString(6);
            }
        }
    }
}

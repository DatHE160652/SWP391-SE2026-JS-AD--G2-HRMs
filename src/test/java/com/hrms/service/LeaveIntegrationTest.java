package com.hrms.service;

import com.hrms.dao.LeaveBalanceDAO;
import com.hrms.dao.LeaveReportDTO;
import com.hrms.dao.LeaveRequestDAO;
import com.hrms.dao.LeaveTypeDAO;
import com.hrms.model.LeaveBalance;
import com.hrms.model.LeaveRequest;
import com.hrms.model.LeaveType;
import com.hrms.util.DBConnection;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Deep end-to-end database integration test for the Leave Management workflow:
 * - Policy retrieval & creation
 * - Quota balance allocation
 * - Request submission & validation
 * - Transactional approval with balance deduction & attendance sync
 * - Rejection & cancellation workflows
 * - Calendar querying & utilization reporting
 */
public class LeaveIntegrationTest {

    private LeaveService leaveService;
    private LeaveTypeDAO leaveTypeDAO;
    private LeaveBalanceDAO leaveBalanceDAO;
    private LeaveRequestDAO leaveRequestDAO;
    private boolean dbAvailable = false;

    @Before
    public void checkDatabase() {
        leaveTypeDAO = new LeaveTypeDAO();
        leaveBalanceDAO = new LeaveBalanceDAO();
        leaveRequestDAO = new LeaveRequestDAO();
        leaveService = new LeaveService(leaveTypeDAO, leaveBalanceDAO, leaveRequestDAO);

        try (Connection conn = DBConnection.getConnection()) {
            dbAvailable = (conn != null && !conn.isClosed());
            if (dbAvailable) {
                try (java.sql.Statement s = conn.createStatement()) {
                    java.sql.ResultSet rs = s.executeQuery(
                        "SELECT " +
                        "(SELECT COUNT(*) FROM Companies) AS C_Comp, " +
                        "(SELECT COUNT(*) FROM Users) AS C_Users, " +
                        "(SELECT COUNT(*) FROM Departments) AS C_Depts, " +
                        "(SELECT COUNT(*) FROM Positions) AS C_Pos, " +
                        "(SELECT COUNT(*) FROM Employees) AS C_Emp, " +
                        "(SELECT COUNT(*) FROM LeaveTypes) AS C_LT"
                    );
                    if (rs.next()) {
                        System.out.println(String.format(
                            "[DB STATS] Companies: %d, Users: %d, Departments: %d, Positions: %d, Employees: %d, LeaveTypes: %d",
                            rs.getInt("C_Comp"), rs.getInt("C_Users"), rs.getInt("C_Depts"),
                            rs.getInt("C_Pos"), rs.getInt("C_Emp"), rs.getInt("C_LT")
                        ));
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("[INFO] Live SQL Server is not reachable in current test environment: " + e.getMessage());
            dbAvailable = false;
        }
    }

    @Test
    public void testCompleteLeaveWorkflowEndToEnd() throws SQLException {
        Assume.assumeTrue("Skipping integration test because SQL Server is not running or credentials failed", dbAvailable);

        int companyId;
        int employeeId;
        int approverUserId;
        int year = LocalDate.now().getYear();

        // Dynamically find or seed a valid existing employee, company, and approver user in HRMsDB
        try (Connection conn = DBConnection.getConnection();
             java.sql.Statement stmt = conn.createStatement()) {
            
            // Get Company
            try (java.sql.ResultSet rs = stmt.executeQuery("SELECT TOP 1 CompanyId FROM Companies ORDER BY CompanyId ASC")) {
                if (rs.next()) {
                    companyId = rs.getInt("CompanyId");
                } else {
                    return;
                }
            }

            // Get User
            try (java.sql.ResultSet rs = stmt.executeQuery("SELECT TOP 1 UserId FROM Users WHERE CompanyId = " + companyId)) {
                if (rs.next()) {
                    approverUserId = rs.getInt("UserId");
                } else {
                    return;
                }
            }

            // Check if employee exists, if not, create Department, Position, and Employee
            try (java.sql.ResultSet rs = stmt.executeQuery("SELECT TOP 1 EmployeeId FROM Employees WHERE CompanyId = " + companyId)) {
                if (rs.next()) {
                    employeeId = rs.getInt("EmployeeId");
                } else {
                    // Seed department
                    int deptId;
                    try (java.sql.ResultSet rsD = stmt.executeQuery("SELECT TOP 1 DepartmentId FROM Departments WHERE CompanyId = " + companyId)) {
                        if (rsD.next()) {
                            deptId = rsD.getInt("DepartmentId");
                        } else {
                            try (java.sql.PreparedStatement psD = conn.prepareStatement(
                                    "INSERT INTO Departments (CompanyId, DepartmentCode, DepartmentName) VALUES (?, 'TECH', N'Phòng Kỹ Thuật')",
                                    java.sql.Statement.RETURN_GENERATED_KEYS)) {
                                psD.setInt(1, companyId);
                                psD.executeUpdate();
                                java.sql.ResultSet gk = psD.getGeneratedKeys();
                                gk.next();
                                deptId = gk.getInt(1);
                            }
                        }
                    }

                    // Seed position
                    int posId;
                    try (java.sql.ResultSet rsP = stmt.executeQuery("SELECT TOP 1 PositionId FROM Positions WHERE CompanyId = " + companyId)) {
                        if (rsP.next()) {
                            posId = rsP.getInt("PositionId");
                        } else {
                            try (java.sql.PreparedStatement psP = conn.prepareStatement(
                                    "INSERT INTO Positions (CompanyId, PositionTitle) VALUES (?, N'Kỹ sư phần mềm')",
                                    java.sql.Statement.RETURN_GENERATED_KEYS)) {
                                psP.setInt(1, companyId);
                                psP.executeUpdate();
                                java.sql.ResultSet gk = psP.getGeneratedKeys();
                                gk.next();
                                posId = gk.getInt(1);
                            }
                        }
                    }

                    // Seed Employee
                    try (java.sql.PreparedStatement psE = conn.prepareStatement(
                            "INSERT INTO Employees (CompanyId, UserId, DepartmentId, PositionId, EmployeeCode, FullName, IdentityNumber, Status) " +
                            "VALUES (?, ?, ?, ?, 'EMP-TEST-001', N'Nguyễn Văn Kiểm Thử', '001234567890', 'Active')",
                            java.sql.Statement.RETURN_GENERATED_KEYS)) {
                        psE.setInt(1, companyId);
                        psE.setInt(2, approverUserId);
                        psE.setInt(3, deptId);
                        psE.setInt(4, posId);
                        psE.executeUpdate();
                        java.sql.ResultSet gk = psE.getGeneratedKeys();
                        gk.next();
                        employeeId = gk.getInt(1);
                    }
                }
            }
        }

        // 1. Ensure at least one LeaveType exists
        List<LeaveType> types = leaveTypeDAO.getLeaveTypesByCompany(companyId);
        LeaveType testType;
        if (types.isEmpty()) {
            LeaveType newType = new LeaveType();
            newType.setCompanyId(companyId);
            newType.setTypeCode("TEST_ANNUAL");
            newType.setTypeName("Nghỉ phép thường niên (Test)");
            newType.setDefaultDaysPerYear(12);
            newType.setPaid(true);
            int id = leaveTypeDAO.createLeaveType(newType);
            newType.setLeaveTypeId(id);
            testType = newType;
        } else {
            testType = types.get(0);
        }
        assertNotNull(testType);
        assertTrue(testType.getLeaveTypeId() > 0);

        // 2. Ensure LeaveBalance exists
        LeaveBalance balance = leaveBalanceDAO.getBalance(employeeId, testType.getLeaveTypeId(), year);
        if (balance == null) {
            try (Connection conn = DBConnection.getConnection()) {
                leaveBalanceDAO.insertBalance(conn, employeeId, testType.getLeaveTypeId(), year, BigDecimal.valueOf(15.0));
            }
            balance = leaveBalanceDAO.getBalance(employeeId, testType.getLeaveTypeId(), year);
        }
        assertNotNull(balance);

        BigDecimal initialRemaining = balance.getRemainingDays();
        BigDecimal initialUsed = balance.getUsedDays();

        // 3. Submit Leave Request for a known weekday (Next Monday)
        LocalDate nextMonday = LocalDate.now().plusDays(1);
        while (nextMonday.getDayOfWeek() != java.time.DayOfWeek.MONDAY) {
            nextMonday = nextMonday.plusDays(1);
        }
        LocalDate nextTuesday = nextMonday.plusDays(1);

        LeaveRequest submitted = leaveService.submitLeaveRequest(
                companyId, employeeId, testType.getLeaveTypeId(),
                nextMonday, nextTuesday, "Nghỉ phép kiểm thử hệ thống tự động"
        );

        assertNotNull(submitted);
        assertTrue(submitted.getLeaveRequestId() > 0);
        assertEquals("Pending", submitted.getStatus());
        assertEquals(BigDecimal.valueOf(2), submitted.getTotalDays());

        // 4. Test Cancellation on a separate request
        LocalDate nextWed = nextTuesday.plusDays(1);
        LeaveRequest reqToCancel = leaveService.submitLeaveRequest(
                companyId, employeeId, testType.getLeaveTypeId(),
                nextWed, nextWed, "Đơn để kiểm thử tính năng hủy"
        );
        assertTrue(reqToCancel.getLeaveRequestId() > 0);

        boolean cancelResult = leaveService.cancelPendingRequest(reqToCancel.getLeaveRequestId(), employeeId);
        assertTrue(cancelResult);
        LeaveRequest cancelledCheck = leaveRequestDAO.getRequestById(reqToCancel.getLeaveRequestId());
        assertEquals("Cancelled", cancelledCheck.getStatus());

        // 5. Approve the first request (Atomic transaction)
        leaveService.approveLeaveRequest(submitted.getLeaveRequestId(), approverUserId, "Đã duyệt kiểm thử");

        LeaveRequest approvedCheck = leaveRequestDAO.getRequestById(submitted.getLeaveRequestId());
        assertEquals("Approved", approvedCheck.getStatus());
        assertEquals("Đã duyệt kiểm thử", approvedCheck.getApproverComment());
        assertNotNull(approvedCheck.getReviewedAt());

        // 6. Verify Quota deduction in LeaveBalances
        LeaveBalance updatedBalance = leaveBalanceDAO.getBalance(employeeId, testType.getLeaveTypeId(), year);
        assertNotNull(updatedBalance);
        assertEquals(initialUsed.add(BigDecimal.valueOf(2)), updatedBalance.getUsedDays());
        assertEquals(initialRemaining.subtract(BigDecimal.valueOf(2)), updatedBalance.getRemainingDays());

        // 7. Test Reject flow on a third request
        LocalDate nextThurs = nextWed.plusDays(1);
        LeaveRequest reqToReject = leaveService.submitLeaveRequest(
                companyId, employeeId, testType.getLeaveTypeId(),
                nextThurs, nextThurs, "Đơn kiểm thử từ chối"
        );

        leaveService.rejectLeaveRequest(reqToReject.getLeaveRequestId(), approverUserId, "Không đủ nhân sự trực");
        LeaveRequest rejectedCheck = leaveRequestDAO.getRequestById(reqToReject.getLeaveRequestId());
        assertEquals("Rejected", rejectedCheck.getStatus());
        assertEquals("Không đủ nhân sự trực", rejectedCheck.getApproverComment());

        // 8. Test Calendar queries
        List<LeaveRequest> calendarEvents = leaveRequestDAO.getApprovedRequestsForCalendar(
                companyId, employeeId, null, nextMonday.minusDays(1), nextTuesday.plusDays(1)
        );
        assertFalse(calendarEvents.isEmpty());
        assertTrue(calendarEvents.stream().anyMatch(e -> e.getLeaveRequestId().equals(submitted.getLeaveRequestId())));

        // 9. Test Leave Utilization Report
        List<LeaveReportDTO> reports = leaveRequestDAO.getLeaveUtilizationReport(companyId, year, null);
        assertFalse(reports.isEmpty());
    }
}

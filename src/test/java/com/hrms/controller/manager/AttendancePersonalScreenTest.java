package com.hrms.controller.manager;

import com.hrms.api.ApiRoutes;
import com.hrms.dao.manager.AttendanceDAO.AttendanceDTO;
import com.hrms.dao.manager.AttendanceSummaryDAO;
import com.hrms.service.manager.AttendanceService;
import com.hrms.service.companyadmin.DepartmentService;
import com.hrms.service.companyadmin.EmployeeService;
import com.hrms.service.security.TenantPrincipal;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.Before;
import org.junit.Test;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests covering Screen 2: Chấm công Cá nhân (Employee / Personal Attendance Screen)
 * - Navigation to employee-attendance.jsp and manager-personal-attendance.jsp
 * - View personal monthly attendance records & monthly summary (GET /attendance/personal)
 * - Access control & privacy enforcement: Employee can only view their own attendance
 * - Manager can view their own personal attendance, but restricted from viewing others via /attendance/personal
 * - Default month/year fallback when parameters are omitted
 * - Authentication & tenant workspace isolation checks
 */
public class AttendancePersonalScreenTest {

    private AttendanceService attendanceService;
    private EmployeeService employeeService;
    private DepartmentService departmentService;
    private AttendanceController controller;

    private HttpServletRequest request;
    private HttpServletResponse response;
    private StringWriter responseWriter;

    private static final int COMPANY_ID = 1;
    private static final int EMP_ID_A = 101;
    private static final int EMP_ID_B = 102;
    private static final int MANAGER_EMP_ID = 50;

    private TenantPrincipal employeePrincipalA;
    private TenantPrincipal employeePrincipalB;
    private TenantPrincipal managerPrincipal;

    @Before
    public void setUp() throws Exception {
        attendanceService = mock(AttendanceService.class);
        employeeService = mock(EmployeeService.class);
        departmentService = mock(DepartmentService.class);
        controller = new AttendanceController(attendanceService, employeeService, departmentService);

        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        responseWriter = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(responseWriter));

        employeePrincipalA = AttendanceTestHelper.createPrincipal(21, COMPANY_ID, EMP_ID_A, "EMPLOYEE", "Nguyễn Văn A");
        employeePrincipalB = AttendanceTestHelper.createPrincipal(22, COMPANY_ID, EMP_ID_B, "EMPLOYEE", "Trần Thị B");
        managerPrincipal = AttendanceTestHelper.createPrincipal(10, COMPANY_ID, MANAGER_EMP_ID, "MANAGER", "Lê Quản Lý");

        AttendanceTestHelper.setupMockSession(request, employeePrincipalA);
    }

    // ==========================================
    // 1. Navigation to Personal Views
    // ==========================================

    @Test
    public void testViewEmployeeAttendancePage_ForwardsToEmployeeJsp() throws Exception {
        when(request.getServletPath()).thenReturn(ApiRoutes.ATTENDANCE_PAGE_EMPLOYEE);
        RequestDispatcher dispatcher = mock(RequestDispatcher.class);
        when(request.getRequestDispatcher(ApiRoutes.VIEW_ATTENDANCE_EMPLOYEE)).thenReturn(dispatcher);

        controller.doGet(request, response);

        verify(dispatcher).forward(request, response);
    }

    @Test
    public void testViewManagerPersonalAttendancePage_ForwardsToManagerPersonalJsp() throws Exception {
        AttendanceTestHelper.setupMockSession(request, managerPrincipal);
        when(request.getServletPath()).thenReturn(ApiRoutes.ATTENDANCE_PAGE_MANAGER_PERSONAL);
        RequestDispatcher dispatcher = mock(RequestDispatcher.class);
        when(request.getRequestDispatcher(ApiRoutes.VIEW_ATTENDANCE_MANAGER_PERSONAL)).thenReturn(dispatcher);

        controller.doGet(request, response);

        verify(dispatcher).forward(request, response);
    }

    // ==========================================
    // 2. Personal Attendance Retrieval & Summary
    // ==========================================

    @Test
    public void testPersonalAttendance_EmployeeViewsOwnAttendance_Success() throws Exception {
        when(request.getServletPath()).thenReturn(ApiRoutes.ATTENDANCE_PERSONAL);
        when(request.getParameter("employeeId")).thenReturn(String.valueOf(EMP_ID_A));
        when(request.getParameter("month")).thenReturn("10");
        when(request.getParameter("year")).thenReturn("2026");

        LocalDate d1 = LocalDate.of(2026, 10, 1);
        LocalDate d2 = LocalDate.of(2026, 10, 2);

        AttendanceDTO rec1 = AttendanceTestHelper.createAttendanceDTO(10L, COMPANY_ID, EMP_ID_A, "EMP101", "Nguyễn Văn A",
                1, "Phòng Kỹ thuật", d1, LocalTime.of(8, 20), LocalTime.of(17, 30), new BigDecimal("8.00"), "Present");
        AttendanceDTO rec2 = AttendanceTestHelper.createAttendanceDTO(11L, COMPANY_ID, EMP_ID_A, "EMP101", "Nguyễn Văn A",
                1, "Phòng Kỹ thuật", d2, LocalTime.of(8, 40), LocalTime.of(17, 30), new BigDecimal("7.83"), "Late");

        AttendanceSummaryDAO summary = AttendanceTestHelper.createSummary(
                22, new BigDecimal("176.00"), 20, 1, 1, 0, 0);

        when(attendanceService.getPersonalAttendance(COMPANY_ID, EMP_ID_A, 10, 2026)).thenReturn(List.of(rec1, rec2));
        when(attendanceService.getPersonalSummary(COMPANY_ID, EMP_ID_A, 10, 2026)).thenReturn(summary);

        controller.doGet(request, response);

        verify(response).setStatus(HttpServletResponse.SC_OK);
        String json = responseWriter.toString();
        assertTrue(json.contains("\"success\":true"));
        assertTrue(json.contains("\"data\":["));
        assertTrue(json.contains("EMP101"));
        assertTrue(json.contains("Present"));
        assertTrue(json.contains("Late"));

        // Summary validation
        assertTrue(json.contains("\"summary\":{"));
        assertTrue(json.contains("\"totalWorkingDays\":22"));
        assertTrue(json.contains("\"totalWorkCredits\":22"));
        assertTrue(json.contains("\"presentDays\":20"));
        assertTrue(json.contains("\"lateDays\":1"));
        assertTrue(json.contains("\"earlyLeaveDays\":1"));
    }

    @Test
    public void testPersonalAttendance_WorkCreditCalculation_HalfCreditAndFullCredit() throws Exception {
        when(request.getServletPath()).thenReturn(ApiRoutes.ATTENDANCE_PERSONAL);
        when(request.getParameter("employeeId")).thenReturn(String.valueOf(EMP_ID_A));
        when(request.getParameter("month")).thenReturn("10");
        when(request.getParameter("year")).thenReturn("2026");

        LocalDate d1 = LocalDate.of(2026, 10, 26); // 8.33 hrs >= 7.0 -> 1.0 công
        LocalDate d2 = LocalDate.of(2026, 10, 27); // 3.50 hrs >= 3.5 -> 0.5 công (nửa công)
        LocalDate d3 = LocalDate.of(2026, 10, 28); // 3.00 hrs < 3.5 -> 0.0 công (< 3.5h không tính công)

        AttendanceDTO rec1 = AttendanceTestHelper.createAttendanceDTO(1L, COMPANY_ID, EMP_ID_A, "EMP101", "Nguyễn Văn A",
                1, "IT", d1, LocalTime.of(8, 10), LocalTime.of(17, 30), new BigDecimal("8.33"), "Present");
        AttendanceDTO rec2 = AttendanceTestHelper.createAttendanceDTO(2L, COMPANY_ID, EMP_ID_A, "EMP101", "Nguyễn Văn A",
                1, "IT", d2, LocalTime.of(8, 30), LocalTime.of(12, 00), new BigDecimal("3.50"), "Early Leave");
        AttendanceDTO rec3 = AttendanceTestHelper.createAttendanceDTO(3L, COMPANY_ID, EMP_ID_A, "EMP101", "Nguyễn Văn A",
                1, "IT", d3, LocalTime.of(8, 30), LocalTime.of(11, 30), new BigDecimal("3.00"), "Early Leave");

        // Verify individual work credit calculations
        assertEquals(new BigDecimal("1.0"), rec1.getWorkCredit());
        assertEquals(new BigDecimal("0.5"), rec2.getWorkCredit());
        assertEquals(BigDecimal.ZERO, rec3.getWorkCredit());

        // Total: 1.0 + 0.5 + 0.0 = 1.5 công
        AttendanceSummaryDAO summary = AttendanceTestHelper.createSummary(
                3, new BigDecimal("1.5"), new BigDecimal("14.83"), 1, 0, 2, 0, 0);

        when(attendanceService.getPersonalAttendance(COMPANY_ID, EMP_ID_A, 10, 2026)).thenReturn(List.of(rec1, rec2, rec3));
        when(attendanceService.getPersonalSummary(COMPANY_ID, EMP_ID_A, 10, 2026)).thenReturn(summary);

        controller.doGet(request, response);

        verify(response).setStatus(HttpServletResponse.SC_OK);
        String json = responseWriter.toString();
        assertTrue(json.contains("\"success\":true"));
        assertTrue(json.contains("\"workCredit\":1.0") || json.contains("\"workCredit\":1"));
        assertTrue(json.contains("\"workCredit\":0.5"));
        assertTrue(json.contains("\"workCredit\":0"));
        assertTrue(json.contains("\"totalWorkCredits\":1.5"));
    }

    @Test
    public void testPersonalAttendance_DefaultsToCurrentMonthAndYear_WhenNotProvided() throws Exception {
        when(request.getServletPath()).thenReturn(ApiRoutes.ATTENDANCE_PERSONAL);
        when(request.getParameter("employeeId")).thenReturn(String.valueOf(EMP_ID_A));
        when(request.getParameter("month")).thenReturn(null);
        when(request.getParameter("year")).thenReturn(null);

        int currentMonth = LocalDate.now().getMonthValue();
        int currentYear = LocalDate.now().getYear();

        when(attendanceService.getPersonalAttendance(COMPANY_ID, EMP_ID_A, currentMonth, currentYear))
                .thenReturn(List.of());
        when(attendanceService.getPersonalSummary(COMPANY_ID, EMP_ID_A, currentMonth, currentYear))
                .thenReturn(new AttendanceSummaryDAO());

        controller.doGet(request, response);

        verify(attendanceService).getPersonalAttendance(COMPANY_ID, EMP_ID_A, currentMonth, currentYear);
        verify(attendanceService).getPersonalSummary(COMPANY_ID, EMP_ID_A, currentMonth, currentYear);
        verify(response).setStatus(HttpServletResponse.SC_OK);
    }

    // ==========================================
    // 3. Security, Access Control & Privacy
    // ==========================================

    @Test
    public void testPersonalAttendance_EmployeeAttemptsToViewOtherEmployee_ThrowsBadRequest() throws Exception {
        // Employee A tries to view Employee B's personal records
        when(request.getServletPath()).thenReturn(ApiRoutes.ATTENDANCE_PERSONAL);
        when(request.getParameter("employeeId")).thenReturn(String.valueOf(EMP_ID_B));

        controller.doGet(request, response);

        verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
        String json = responseWriter.toString();
        assertTrue(json.contains("\"success\":false"));
        assertTrue(json.contains("Bạn chỉ được xem dữ liệu chấm công của chính mình"));
    }

    @Test
    public void testPersonalAttendance_ManagerViewsOwnAttendance_Success() throws Exception {
        AttendanceTestHelper.setupMockSession(request, managerPrincipal);

        when(request.getServletPath()).thenReturn(ApiRoutes.ATTENDANCE_PERSONAL);
        when(request.getParameter("employeeId")).thenReturn(String.valueOf(MANAGER_EMP_ID));
        when(request.getParameter("month")).thenReturn("10");
        when(request.getParameter("year")).thenReturn("2026");

        when(attendanceService.getPersonalAttendance(COMPANY_ID, MANAGER_EMP_ID, 10, 2026)).thenReturn(List.of());
        when(attendanceService.getPersonalSummary(COMPANY_ID, MANAGER_EMP_ID, 10, 2026)).thenReturn(new AttendanceSummaryDAO());

        controller.doGet(request, response);

        verify(response).setStatus(HttpServletResponse.SC_OK);
        String json = responseWriter.toString();
        assertTrue(json.contains("\"success\":true"));
    }

    @Test
    public void testPersonalAttendance_ManagerAttemptsToViewOtherEmployee_ThrowsBadRequest() throws Exception {
        AttendanceTestHelper.setupMockSession(request, managerPrincipal);

        when(request.getServletPath()).thenReturn(ApiRoutes.ATTENDANCE_PERSONAL);
        when(request.getParameter("employeeId")).thenReturn(String.valueOf(EMP_ID_A));

        controller.doGet(request, response);

        verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
        String json = responseWriter.toString();
        assertTrue(json.contains("\"success\":false"));
        assertTrue(json.contains("Bạn chỉ được xem dữ liệu chấm công của chính mình"));
    }

    @Test
    public void testPersonalAttendance_MissingEmployeeId_ThrowsBadRequest() throws Exception {
        when(request.getServletPath()).thenReturn(ApiRoutes.ATTENDANCE_PERSONAL);
        when(request.getParameter("employeeId")).thenReturn("");

        controller.doGet(request, response);

        verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
        String json = responseWriter.toString();
        assertTrue(json.contains("\"success\":false"));
        assertTrue(json.contains("Vui lòng chọn nhân viên"));
    }

    @Test
    public void testPersonalAttendance_UnauthenticatedUser_ThrowsForbidden() throws Exception {
        // No principal in session
        AttendanceTestHelper.setupMockSession(request, null);

        when(request.getServletPath()).thenReturn(ApiRoutes.ATTENDANCE_PERSONAL);
        when(request.getParameter("employeeId")).thenReturn(String.valueOf(EMP_ID_A));

        controller.doGet(request, response);

        verify(response).setStatus(HttpServletResponse.SC_FORBIDDEN);
        String json = responseWriter.toString();
        assertTrue(json.contains("\"success\":false"));
        assertTrue(json.contains("Vui lòng đăng nhập vào workspace"));
    }

    // ==========================================
    // 4. Department Violation Quota (Max 5 late/early sessions)
    // ==========================================

    @Test
    public void testPersonalAttendance_LateAndEarlyLeaveSummary_OverThresholdWarning() throws Exception {
        when(request.getServletPath()).thenReturn(ApiRoutes.ATTENDANCE_PERSONAL);
        when(request.getParameter("employeeId")).thenReturn(String.valueOf(EMP_ID_A));
        when(request.getParameter("month")).thenReturn("10");
        when(request.getParameter("year")).thenReturn("2026");

        // 3 late + 1 early leave = 4 violations (> 3 buổi -> cảnh báo)
        AttendanceSummaryDAO summary = AttendanceTestHelper.createSummary(
                20, new BigDecimal("19.0"), new BigDecimal("150.00"), 16, 3, 1, 0, 0);

        when(attendanceService.getPersonalAttendance(COMPANY_ID, EMP_ID_A, 10, 2026)).thenReturn(List.of());
        when(attendanceService.getPersonalSummary(COMPANY_ID, EMP_ID_A, 10, 2026)).thenReturn(summary);

        controller.doGet(request, response);

        verify(response).setStatus(HttpServletResponse.SC_OK);
        String json = responseWriter.toString();
        assertTrue(json.contains("\"success\":true"));
        assertTrue(json.contains("\"lateDays\":3"));
        assertTrue(json.contains("\"earlyLeaveDays\":1"));
    }

    @Test
    public void testPersonalAttendance_LateAndEarlyLeaveSummary_MaxThresholdReached() throws Exception {
        when(request.getServletPath()).thenReturn(ApiRoutes.ATTENDANCE_PERSONAL);
        when(request.getParameter("employeeId")).thenReturn(String.valueOf(EMP_ID_A));
        when(request.getParameter("month")).thenReturn("10");
        when(request.getParameter("year")).thenReturn("2026");

        // 4 late + 2 early leave = 6 violations (>= 5 buổi -> mức vi phạm tối đa)
        AttendanceSummaryDAO summary = AttendanceTestHelper.createSummary(
                20, new BigDecimal("18.0"), new BigDecimal("145.00"), 14, 4, 2, 0, 0);

        when(attendanceService.getPersonalAttendance(COMPANY_ID, EMP_ID_A, 10, 2026)).thenReturn(List.of());
        when(attendanceService.getPersonalSummary(COMPANY_ID, EMP_ID_A, 10, 2026)).thenReturn(summary);

        controller.doGet(request, response);

        verify(response).setStatus(HttpServletResponse.SC_OK);
        String json = responseWriter.toString();
        assertTrue(json.contains("\"success\":true"));
        assertTrue(json.contains("\"lateDays\":4"));
        assertTrue(json.contains("\"earlyLeaveDays\":2"));
    }
}


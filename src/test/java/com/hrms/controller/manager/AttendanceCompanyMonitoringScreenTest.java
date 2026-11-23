package com.hrms.controller.manager;

import com.hrms.api.ApiRoutes;
import com.hrms.dao.manager.AttendanceDAO.AttendanceDTO;
import com.hrms.model.Department;
import com.hrms.model.Employee;
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
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests covering Screen 4: Giám sát Điểm danh Toàn Công ty (Company Attendance Monitoring Screen)
 * - Navigation to company-attendance.jsp (GET /attendance/company-attendance)
 * - Multi-criteria search (department, status, date range from/to, keyword) (GET /attendance/company)
 * - Default parameter handling (null filters)
 * - Metadata endpoint for filter dropdowns (GET /attendance/meta) scoped by role (Admin, Manager, Employee)
 * - Exception handling (Database error, Unauthenticated access)
 */
public class AttendanceCompanyMonitoringScreenTest {

    private AttendanceService attendanceService;
    private EmployeeService employeeService;
    private DepartmentService departmentService;
    private AttendanceController controller;

    private HttpServletRequest request;
    private HttpServletResponse response;
    private StringWriter responseWriter;

    private static final int COMPANY_ID = 1;
    private static final int ADMIN_USER_ID = 1;
    private static final int MANAGER_EMP_ID = 50;
    private static final int EMP_ID = 101;

    private TenantPrincipal adminPrincipal;
    private TenantPrincipal managerPrincipal;
    private TenantPrincipal employeePrincipal;

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

        adminPrincipal = AttendanceTestHelper.createPrincipal(ADMIN_USER_ID, COMPANY_ID, null, "COMPANY_ADMIN", "Admin Công ty");
        managerPrincipal = AttendanceTestHelper.createPrincipal(10, COMPANY_ID, MANAGER_EMP_ID, "MANAGER", "Trưởng Phòng");
        employeePrincipal = AttendanceTestHelper.createPrincipal(20, COMPANY_ID, EMP_ID, "EMPLOYEE", "Nhân Viên");

        AttendanceTestHelper.setupMockSession(request, adminPrincipal);
    }

    // ==========================================
    // 1. Navigation to Company Attendance View
    // ==========================================

    @Test
    public void testViewCompanyAttendancePage_ForwardsToCompanyJsp() throws Exception {
        when(request.getServletPath()).thenReturn(ApiRoutes.ATTENDANCE_PAGE_COMPANY);
        RequestDispatcher dispatcher = mock(RequestDispatcher.class);
        when(request.getRequestDispatcher(ApiRoutes.VIEW_ATTENDANCE_COMPANY)).thenReturn(dispatcher);

        controller.doGet(request, response);

        verify(dispatcher).forward(request, response);
    }

    // ==========================================
    // 2. Multi-criteria Search (GET /attendance/company)
    // ==========================================

    @Test
    public void testSearchCompanyAttendance_WithAllFilters_CallsServiceCorrectly() throws Exception {
        when(request.getServletPath()).thenReturn(ApiRoutes.ATTENDANCE_COMPANY);
        when(request.getParameter("departmentId")).thenReturn("2");
        when(request.getParameter("status")).thenReturn("Late");
        when(request.getParameter("fromDate")).thenReturn("2026-10-01");
        when(request.getParameter("toDate")).thenReturn("2026-10-31");
        when(request.getParameter("keyword")).thenReturn("Nguyen");

        LocalDate from = LocalDate.of(2026, 10, 1);
        LocalDate to = LocalDate.of(2026, 10, 31);

        AttendanceDTO resultItem = AttendanceTestHelper.createAttendanceDTO(
                100L, COMPANY_ID, EMP_ID, "EMP101", "Nguyen Van An",
                2, "Phòng Kinh Doanh", LocalDate.of(2026, 10, 15),
                LocalTime.of(8, 45), LocalTime.of(17, 30), new BigDecimal("7.75"), "Late");

        when(attendanceService.searchCompanyAttendance(COMPANY_ID, 2, "Late", from, to, "Nguyen"))
                .thenReturn(List.of(resultItem));

        controller.doGet(request, response);

        verify(attendanceService).searchCompanyAttendance(COMPANY_ID, 2, "Late", from, to, "Nguyen");
        verify(response).setStatus(HttpServletResponse.SC_OK);

        String json = responseWriter.toString();
        assertTrue(json.contains("\"success\":true"));
        assertTrue(json.contains("\"data\":["));
        assertTrue(json.contains("Nguyen Van An"));
        assertTrue(json.contains("\"status\":\"Late\""));
        assertTrue(json.contains("\"totalHours\":7.75"));
    }

    @Test
    public void testSearchCompanyAttendance_DefaultNoFilters_CallsServiceWithNulls() throws Exception {
        when(request.getServletPath()).thenReturn(ApiRoutes.ATTENDANCE_COMPANY);
        when(request.getParameter("departmentId")).thenReturn(null);
        when(request.getParameter("status")).thenReturn(null);
        when(request.getParameter("fromDate")).thenReturn("2026-11-01");
        when(request.getParameter("toDate")).thenReturn("2026-11-30");
        when(request.getParameter("keyword")).thenReturn(null);

        when(attendanceService.searchCompanyAttendance(COMPANY_ID, null, null, LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30), null))
                .thenReturn(List.of());

        controller.doGet(request, response);

        verify(attendanceService).searchCompanyAttendance(COMPANY_ID, null, null, LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30), null);
        verify(response).setStatus(HttpServletResponse.SC_OK);
        String json = responseWriter.toString();
        assertTrue(json.contains("\"success\":true"));
        assertTrue(json.contains("\"data\":[]"));
    }

    // ==========================================
    // 3. Metadata Dropdowns (GET /attendance/meta)
    // ==========================================

    @Test
    public void testGetMeta_AdminRole_ReturnsAllDepartmentsAndEmployees() throws Exception {
        when(request.getServletPath()).thenReturn(ApiRoutes.ATTENDANCE_META);

        Employee e1 = new Employee();
        e1.setEmployeeId(101);
        e1.setEmployeeCode("EMP101");
        e1.setFullName("Nguyen A");
        e1.setStatus("ACTIVE");

        Employee e2 = new Employee();
        e2.setEmployeeId(102);
        e2.setEmployeeCode("EMP102");
        e2.setFullName("Tran B");
        e2.setStatus("ACTIVE");

        when(employeeService.getAllEmployees(COMPANY_ID)).thenReturn(List.of(e1, e2));

        Department d1 = new Department();
        d1.setDepartmentId(1);
        d1.setDepartmentCode("IT");
        d1.setDepartmentName("Phòng IT");
        when(departmentService.getDepartments(COMPANY_ID)).thenReturn(List.of(d1));

        controller.doGet(request, response);

        verify(response).setStatus(HttpServletResponse.SC_OK);
        String json = responseWriter.toString();
        assertTrue(json.contains("\"success\":true"));
        assertTrue(json.contains("EMP101"));
        assertTrue(json.contains("EMP102"));
        assertTrue(json.contains("Phòng IT"));
    }

    @Test
    public void testGetMeta_ManagerRole_ScopesToManagerTeam() throws Exception {
        AttendanceTestHelper.setupMockSession(request, managerPrincipal);

        when(request.getServletPath()).thenReturn(ApiRoutes.ATTENDANCE_META);

        Employee e1 = new Employee();
        e1.setEmployeeId(101);
        e1.setEmployeeCode("EMP101");
        e1.setFullName("Nguyen A");

        Employee eOther = new Employee();
        eOther.setEmployeeId(999);
        eOther.setEmployeeCode("EMP999");
        eOther.setFullName("Ngoai Team");

        when(employeeService.getAllEmployees(COMPANY_ID)).thenReturn(List.of(e1, eOther));

        Department d1 = new Department();
        d1.setDepartmentId(1);
        d1.setDepartmentCode("IT");
        d1.setDepartmentName("Phòng IT");
        Department d2 = new Department();
        d2.setDepartmentId(2);
        d2.setDepartmentCode("SALE");
        d2.setDepartmentName("Phòng Sales");
        when(departmentService.getDepartments(COMPANY_ID)).thenReturn(List.of(d1, d2));

        // Manager only has employee 101 in department 1
        AttendanceDTO teamMember = AttendanceTestHelper.createAttendanceDTO(0L, COMPANY_ID, 101, "EMP101", "Nguyen A",
                1, "Phòng IT", LocalDate.now(), null, null, BigDecimal.ZERO, null);
        when(attendanceService.getTeamAttendance(eq(COMPANY_ID), eq(MANAGER_EMP_ID), any()))
                .thenReturn(List.of(teamMember));

        controller.doGet(request, response);

        verify(response).setStatus(HttpServletResponse.SC_OK);
        String json = responseWriter.toString();
        assertTrue(json.contains("EMP101"));
        assertFalse(json.contains("EMP999")); // Out of team
        assertTrue(json.contains("Phòng IT"));
        assertFalse(json.contains("Phòng Sales")); // Out of team department
    }

    // ==========================================
    // 4. Error Handling
    // ==========================================

    @Test
    public void testSearchCompanyAttendance_DatabaseError_ReturnsInternalServerError() throws Exception {
        when(request.getServletPath()).thenReturn(ApiRoutes.ATTENDANCE_COMPANY);
        when(request.getParameter("fromDate")).thenReturn("2026-11-01");
        when(request.getParameter("toDate")).thenReturn("2026-11-30");
        when(attendanceService.searchCompanyAttendance(anyInt(), any(), any(), any(), any(), any()))
                .thenThrow(new SQLException("Connection timed out"));

        controller.doGet(request, response);

        verify(response).setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        String json = responseWriter.toString();
        assertTrue(json.contains("\"success\":false"));
        assertTrue(json.contains("Database error"));
    }

    // ==========================================
    // 5. Company Admin Scope Restrictions
    // ==========================================

    @Test
    public void testCompanyAdmin_AccessManagerDailyPage_RedirectsToCompanyAttendance() throws Exception {
        when(request.getContextPath()).thenReturn("/HRMs");
        when(request.getServletPath()).thenReturn(ApiRoutes.ATTENDANCE_PAGE_MANAGER);

        controller.doGet(request, response);

        verify(response).sendRedirect("/HRMs" + ApiRoutes.ATTENDANCE_PAGE_COMPANY);
    }

    @Test
    public void testCompanyAdmin_PerformCheckIn_ReturnsForbidden() throws Exception {
        when(request.getServletPath()).thenReturn(ApiRoutes.ATTENDANCE_CHECK_IN);
        when(request.getParameter("csrfToken")).thenReturn(AttendanceTestHelper.DEFAULT_CSRF_TOKEN);

        controller.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_FORBIDDEN);
        String json = responseWriter.toString();
        assertTrue(json.contains("\"success\":false"));
        assertTrue(json.contains("Tài khoản Quản trị Công ty chỉ có quyền giám sát"));
    }
}


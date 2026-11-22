package com.hrms.controller.manager;

import com.hrms.api.ApiRoutes;
import com.hrms.dao.manager.AttendanceDAO.AttendanceDTO;
import com.hrms.dao.manager.AttendanceReportDAO;
import com.hrms.model.Department;
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
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests covering Screen 3: Bảng ma trận chấm công tháng (Monthly Timesheet Matrix Screen)
 * - Navigation / Forwarding to attendance-list.jsp (?view=list or ?view=timesheet)
 * - Timesheet matrix generation for 28/29/30/31-day months (GET /attendance/report?type=timesheet)
 * - Daily records mapping per employee (Day 1..N check-in, check-out, total hours, status)
 * - Department filter (filtering employees by departmentId)
 * - Keyword search (filtering employees by fullName or employeeCode)
 * - Status aggregation counts (present, late, early, onLeave, absent, totalDays)
 * - Invalid month/year fallback to current date
 */
public class AttendanceTimesheetMatrixScreenTest {

    private AttendanceService attendanceService;
    private EmployeeService employeeService;
    private DepartmentService departmentService;
    private AttendanceController controller;

    private HttpServletRequest request;
    private HttpServletResponse response;
    private StringWriter responseWriter;

    private static final int COMPANY_ID = 1;
    private static final int MANAGER_EMP_ID = 50;

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

        managerPrincipal = AttendanceTestHelper.createPrincipal(10, COMPANY_ID, MANAGER_EMP_ID, "MANAGER", "Nguyễn Quản Lý");
        AttendanceTestHelper.setupMockSession(request, managerPrincipal);
    }

    // ==========================================
    // 1. Navigation to Timesheet List View
    // ==========================================

    @Test
    public void testViewTimesheetPage_ViewList_ForwardsToAttendanceListJsp() throws Exception {
        when(request.getServletPath()).thenReturn(ApiRoutes.ATTENDANCE_PAGE_MANAGER);
        when(request.getParameter("view")).thenReturn("list");

        RequestDispatcher dispatcher = mock(RequestDispatcher.class);
        when(request.getRequestDispatcher("/WEB-INF/views/manager/attendance-list.jsp")).thenReturn(dispatcher);

        controller.doGet(request, response);

        verify(dispatcher).forward(request, response);
    }

    @Test
    public void testViewTimesheetPage_ViewTimesheet_ForwardsToAttendanceListJsp() throws Exception {
        when(request.getServletPath()).thenReturn(ApiRoutes.ATTENDANCE_PAGE_MANAGER);
        when(request.getParameter("view")).thenReturn("timesheet");

        RequestDispatcher dispatcher = mock(RequestDispatcher.class);
        when(request.getRequestDispatcher("/WEB-INF/views/manager/attendance-list.jsp")).thenReturn(dispatcher);

        controller.doGet(request, response);

        verify(dispatcher).forward(request, response);
    }

    // ==========================================
    // 2. Timesheet Matrix Data Generation (GET /attendance/report?type=timesheet)
    // ==========================================

    @Test
    public void testTimesheetMatrix_OctoberMonth_Returns31DaysAndDailyRecords() throws Exception {
        when(request.getServletPath()).thenReturn(ApiRoutes.ATTENDANCE_REPORT);
        when(request.getParameter("type")).thenReturn("timesheet");
        when(request.getParameter("month")).thenReturn("10");
        when(request.getParameter("year")).thenReturn("2026");

        // Scoped employees
        AttendanceDTO emp1 = AttendanceTestHelper.createAttendanceDTO(0L, COMPANY_ID, 101, "EMP101", "Nguyễn Văn A",
                1, "Phòng IT", LocalDate.now(), null, null, BigDecimal.ZERO, null);
        when(attendanceService.getTeamAttendance(eq(COMPANY_ID), eq(MANAGER_EMP_ID), any())).thenReturn(List.of(emp1));

        // Month daily records
        LocalDate day1 = LocalDate.of(2026, 10, 1);
        LocalDate day2 = LocalDate.of(2026, 10, 2);
        AttendanceDTO rec1 = AttendanceTestHelper.createAttendanceDTO(101L, COMPANY_ID, 101, "EMP101", "Nguyễn Văn A",
                1, "Phòng IT", day1, LocalTime.of(8, 25), LocalTime.of(17, 30), new BigDecimal("8.00"), "Present");
        AttendanceDTO rec2 = AttendanceTestHelper.createAttendanceDTO(102L, COMPANY_ID, 101, "EMP101", "Nguyễn Văn A",
                1, "Phòng IT", day2, LocalTime.of(8, 45), LocalTime.of(17, 30), new BigDecimal("7.75"), "Late");

        when(attendanceService.searchCompanyAttendance(eq(COMPANY_ID), any(), isNull(), eq(LocalDate.of(2026, 10, 1)), eq(LocalDate.of(2026, 10, 31)), any()))
                .thenReturn(List.of(rec1, rec2));

        // Departments & stats
        Department dept1 = new Department();
        dept1.setDepartmentId(1);
        dept1.setDepartmentCode("IT");
        dept1.setDepartmentName("Phòng IT");
        when(departmentService.getDepartments(COMPANY_ID)).thenReturn(List.of(dept1));

        AttendanceReportDAO deptStat = AttendanceTestHelper.createReport(101, "EMP101", "Nguyễn Văn A", "Phòng IT",
                2, new BigDecimal("15.75"), 1, 1, 0, 0, 0, 50.0);
        when(attendanceService.getManagerAttendanceReport(eq(COMPANY_ID), eq(MANAGER_EMP_ID), any(), eq(10), eq(2026)))
                .thenReturn(List.of(deptStat));

        controller.doGet(request, response);

        verify(response).setStatus(HttpServletResponse.SC_OK);
        String json = responseWriter.toString();
        assertTrue(json.contains("\"success\":true"));
        assertTrue(json.contains("\"month\":10"));
        assertTrue(json.contains("\"year\":2026"));
        assertTrue(json.contains("\"daysInMonth\":31"));
        assertTrue(json.contains("\"totalEmployees\":1"));

        // Daily records matrix
        assertTrue(json.contains("\"records\":{"));
        assertTrue(json.contains("\"1\":{\"status\":\"Present\",\"checkIn\":\"08:25\",\"checkOut\":\"17:30\",\"totalHours\":8.00}"));
        assertTrue(json.contains("\"2\":{\"status\":\"Late\",\"checkIn\":\"08:45\",\"checkOut\":\"17:30\",\"totalHours\":7.75}"));

        // Monthly summary counts
        assertTrue(json.contains("\"summary\":{"));
        assertTrue(json.contains("\"present\":1"));
        assertTrue(json.contains("\"late\":1"));
        assertTrue(json.contains("\"totalDays\":2"));
    }

    @Test
    public void testTimesheetMatrix_LeapYearFebruary_Returns29Days() throws Exception {
        when(request.getServletPath()).thenReturn(ApiRoutes.ATTENDANCE_REPORT);
        when(request.getParameter("type")).thenReturn("timesheet");
        when(request.getParameter("month")).thenReturn("2");
        when(request.getParameter("year")).thenReturn("2024"); // 2024 is leap year

        when(attendanceService.getTeamAttendance(eq(COMPANY_ID), eq(MANAGER_EMP_ID), any())).thenReturn(List.of());
        when(departmentService.getDepartments(COMPANY_ID)).thenReturn(List.of());

        controller.doGet(request, response);

        String json = responseWriter.toString();
        assertTrue(json.contains("\"month\":2"));
        assertTrue(json.contains("\"year\":2024"));
        assertTrue(json.contains("\"daysInMonth\":29"));
    }

    // ==========================================
    // 3. Department and Keyword Filtering
    // ==========================================

    @Test
    public void testTimesheetMatrix_FilterByDepartment_OnlyReturnsMatchingEmployees() throws Exception {
        when(request.getServletPath()).thenReturn(ApiRoutes.ATTENDANCE_REPORT);
        when(request.getParameter("type")).thenReturn("timesheet");
        when(request.getParameter("departmentId")).thenReturn("2"); // Only Department 2 (HR)

        AttendanceDTO empIT = AttendanceTestHelper.createAttendanceDTO(0L, COMPANY_ID, 101, "EMP101", "Trần A",
                1, "Phòng IT", LocalDate.now(), null, null, BigDecimal.ZERO, null);
        AttendanceDTO empHR = AttendanceTestHelper.createAttendanceDTO(0L, COMPANY_ID, 102, "EMP102", "Phạm B",
                2, "Phòng HR", LocalDate.now(), null, null, BigDecimal.ZERO, null);

        when(attendanceService.getTeamAttendance(eq(COMPANY_ID), eq(MANAGER_EMP_ID), any()))
                .thenReturn(List.of(empIT, empHR));
        when(departmentService.getDepartments(COMPANY_ID)).thenReturn(List.of());

        controller.doGet(request, response);

        String json = responseWriter.toString();
        assertTrue(json.contains("\"totalEmployees\":1"));
        assertTrue(json.contains("EMP102"));
        assertFalse(json.contains("EMP101"));
    }

    @Test
    public void testTimesheetMatrix_FilterByKeyword_MatchesEmployeeNameOrCode() throws Exception {
        when(request.getServletPath()).thenReturn(ApiRoutes.ATTENDANCE_REPORT);
        when(request.getParameter("type")).thenReturn("timesheet");
        when(request.getParameter("keyword")).thenReturn("Linh");

        AttendanceDTO emp1 = AttendanceTestHelper.createAttendanceDTO(0L, COMPANY_ID, 101, "EMP101", "Hoàng Thùy Linh",
                1, "Phòng IT", LocalDate.now(), null, null, BigDecimal.ZERO, null);
        AttendanceDTO emp2 = AttendanceTestHelper.createAttendanceDTO(0L, COMPANY_ID, 102, "EMP102", "Đặng Văn Lâm",
                1, "Phòng IT", LocalDate.now(), null, null, BigDecimal.ZERO, null);

        when(attendanceService.getTeamAttendance(eq(COMPANY_ID), eq(MANAGER_EMP_ID), any()))
                .thenReturn(List.of(emp1, emp2));
        when(departmentService.getDepartments(COMPANY_ID)).thenReturn(List.of());

        controller.doGet(request, response);

        String json = responseWriter.toString();
        assertTrue(json.contains("\"totalEmployees\":1"));
        assertTrue(json.contains("Hoàng Thùy Linh"));
        assertFalse(json.contains("Đặng Văn Lâm"));
    }

    // ==========================================
    // 4. Invalid Month/Year Fallback
    // ==========================================

    @Test
    public void testTimesheetMatrix_InvalidMonthAndYear_FallbacksToCurrent() throws Exception {
        when(request.getServletPath()).thenReturn(ApiRoutes.ATTENDANCE_REPORT);
        when(request.getParameter("type")).thenReturn("timesheet");
        when(request.getParameter("month")).thenReturn("15"); // Invalid
        when(request.getParameter("year")).thenReturn("1990"); // Invalid

        when(attendanceService.getTeamAttendance(eq(COMPANY_ID), eq(MANAGER_EMP_ID), any())).thenReturn(List.of());
        when(departmentService.getDepartments(COMPANY_ID)).thenReturn(List.of());

        controller.doGet(request, response);

        int currentMonth = LocalDate.now().getMonthValue();
        int currentYear = LocalDate.now().getYear();

        String json = responseWriter.toString();
        assertTrue(json.contains("\"month\":" + currentMonth));
        assertTrue(json.contains("\"year\":" + currentYear));
    }

    // ==========================================
    // 5. Excel Export (.xlsx)
    // ==========================================

    @Test
    public void testExportTimesheet_GeneratesXlsxOutputStream() throws Exception {
        when(request.getServletPath()).thenReturn(ApiRoutes.ATTENDANCE_EXPORT_TIMESHEET);
        when(request.getParameter("month")).thenReturn("10");
        when(request.getParameter("year")).thenReturn("2026");

        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        jakarta.servlet.ServletOutputStream sos = new jakarta.servlet.ServletOutputStream() {
            @Override
            public boolean isReady() { return true; }
            @Override
            public void setWriteListener(jakarta.servlet.WriteListener writeListener) {}
            @Override
            public void write(int b) { out.write(b); }
            @Override
            public void write(byte[] b, int off, int len) { out.write(b, off, len); }
        };
        when(response.getOutputStream()).thenReturn(sos);

        AttendanceDTO emp = AttendanceTestHelper.createAttendanceDTO(1L, COMPANY_ID, 101, "EMP-01", "Hoàng Thùy Linh",
                1, "Phòng Kỹ thuật", LocalDate.of(2026, 10, 1),
                LocalTime.of(8, 25), LocalTime.of(17, 30), new BigDecimal("8.00"), "Present");

        when(attendanceService.getTeamAttendance(eq(COMPANY_ID), eq(MANAGER_EMP_ID), any())).thenReturn(List.of(emp));
        when(attendanceService.searchCompanyAttendance(eq(COMPANY_ID), any(), any(), any(), any(), any()))
                .thenReturn(List.of(emp));

        controller.doGet(request, response);

        verify(response).setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        verify(response).setHeader(eq("Content-Disposition"), contains("Bang_Cham_Cong_Thang_10_2026.xlsx"));
        assertTrue("Excel output stream should contain generated xlsx bytes", out.size() > 0);
    }
}


package com.hrms.controller.manager;

import com.hrms.api.ApiRoutes;
import com.hrms.dao.manager.AttendanceDAO.AttendanceDTO;
import com.hrms.model.CompanyShiftSetting;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests covering Screen 1: Điểm danh Nhân sự / Quản lý điểm danh hàng ngày (Manager Daily Attendance Screen)
 * - Navigation / Forwarding to manager-attendance view
 * - View daily attendance for company / scoped team (GET /attendance/all)
 * - Single check-in (On-time, Late, Duplicate check, Validation, CSRF)
 * - Single check-out (On-time, Early leave, Missing check-in, Negative duration, Lunch break deduction)
 * - Batch complete attendance for multiple employees (POST /attendance/batch-complete)
 * - Get & Save Company Shift Settings (Validation, Role restriction, CSRF)
 */
public class AttendanceManagerDailyScreenTest {

    private AttendanceService attendanceService;
    private EmployeeService employeeService;
    private DepartmentService departmentService;
    private AttendanceController controller;

    private HttpServletRequest request;
    private HttpServletResponse response;
    private StringWriter responseWriter;
    private TenantPrincipal managerPrincipal;
    private TenantPrincipal employeePrincipal;
    private TenantPrincipal adminPrincipal;

    private static final int COMPANY_ID = 1;
    private static final int MANAGER_EMP_ID = 50;
    private static final int EMP_ID_1 = 101;
    private static final int EMP_ID_2 = 102;

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

        managerPrincipal = AttendanceTestHelper.createPrincipal(10, COMPANY_ID, MANAGER_EMP_ID, "MANAGER", "Nguyễn Văn Quản Lý");
        employeePrincipal = AttendanceTestHelper.createPrincipal(20, COMPANY_ID, EMP_ID_1, "EMPLOYEE", "Trần Thị Nhân Viên");
        adminPrincipal = AttendanceTestHelper.createPrincipal(1, COMPANY_ID, 1, "COMPANY_ADMIN", "Admin Công Ty");

        AttendanceTestHelper.setupMockSession(request, managerPrincipal);
        when(request.getParameter("csrfToken")).thenReturn(AttendanceTestHelper.DEFAULT_CSRF_TOKEN);
    }

    // ==========================================
    // 1. Navigation to Manager Attendance View
    // ==========================================

    @Test
    public void testViewManagerAttendancePage_ForwardsToManagerJsp() throws Exception {
        when(request.getServletPath()).thenReturn(ApiRoutes.ATTENDANCE_PAGE_MANAGER);
        RequestDispatcher dispatcher = mock(RequestDispatcher.class);
        when(request.getRequestDispatcher(ApiRoutes.VIEW_ATTENDANCE_MANAGER)).thenReturn(dispatcher);

        controller.doGet(request, response);

        verify(dispatcher).forward(request, response);
    }

    // ==========================================
    // 2. View Daily Attendance List (GET /attendance/all)
    // ==========================================

    @Test
    public void testGetAllAttendance_ReturnsScopedEmployeeList() throws Exception {
        when(request.getServletPath()).thenReturn(ApiRoutes.ATTENDANCE_ALL);
        LocalDate date = LocalDate.of(2026, 10, 30);
        when(request.getParameter("date")).thenReturn("2026-10-30");

        AttendanceDTO a1 = AttendanceTestHelper.createAttendanceDTO(1L, COMPANY_ID, EMP_ID_1, "EMP101", "Trần Thị A",
                1, "IT", date, LocalTime.of(8, 25), LocalTime.of(17, 30), new BigDecimal("8.00"), "Present");
        AttendanceDTO a2 = AttendanceTestHelper.createAttendanceDTO(2L, COMPANY_ID, EMP_ID_2, "EMP102", "Lê Văn B",
                1, "IT", date, null, null, BigDecimal.ZERO, "Chưa vào ca");

        when(attendanceService.getTeamAttendance(COMPANY_ID, MANAGER_EMP_ID, date)).thenReturn(List.of(a1, a2));

        controller.doGet(request, response);

        String json = responseWriter.toString();
        assertTrue(json.contains("\"success\":true"));
        assertTrue(json.contains("EMP101"));
        assertTrue(json.contains("EMP102"));
        assertTrue(json.contains("Present"));
    }

    // ==========================================
    // 3. Check-In Tests (POST /attendance/check-in)
    // ==========================================

    @Test
    public void testCheckIn_OnTime_ReturnsSuccessWithPresentStatus() throws Exception {
        when(request.getServletPath()).thenReturn(ApiRoutes.ATTENDANCE_CHECK_IN);
        when(request.getParameter("employeeId")).thenReturn(String.valueOf(EMP_ID_1));
        when(request.getParameter("date")).thenReturn("2026-10-30");
        when(request.getParameter("time")).thenReturn("08:25");

        LocalDate date = LocalDate.of(2026, 10, 30);
        LocalTime time = LocalTime.of(8, 25);

        // Allow manager to manage EMP_ID_1
        AttendanceDTO stubSubordinate = AttendanceTestHelper.createAttendanceDTO(0L, COMPANY_ID, EMP_ID_1, "EMP101", "Trần A", 1, "IT", date, null, null, BigDecimal.ZERO, null);
        when(attendanceService.getTeamAttendance(COMPANY_ID, MANAGER_EMP_ID, date)).thenReturn(List.of(stubSubordinate));

        AttendanceDTO checkedInDto = AttendanceTestHelper.createAttendanceDTO(1L, COMPANY_ID, EMP_ID_1, "EMP101", "Trần A",
                1, "IT", date, time, null, BigDecimal.ZERO, "Present");
        when(attendanceService.checkIn(COMPANY_ID, EMP_ID_1, date, time)).thenReturn(checkedInDto);

        controller.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_OK);
        String json = responseWriter.toString();
        assertTrue(json.contains("\"success\":true"));
        assertTrue(json.contains("Điểm danh vào ca thành công"));
        assertTrue(json.contains("\"status\":\"Present\""));
    }

    @Test
    public void testCheckIn_Late_ReturnsSuccessWithLateStatus() throws Exception {
        when(request.getServletPath()).thenReturn(ApiRoutes.ATTENDANCE_CHECK_IN);
        when(request.getParameter("employeeId")).thenReturn(String.valueOf(EMP_ID_1));
        when(request.getParameter("date")).thenReturn("2026-10-30");
        when(request.getParameter("time")).thenReturn("09:15");

        LocalDate date = LocalDate.of(2026, 10, 30);
        LocalTime time = LocalTime.of(9, 15);

        AttendanceDTO stubSubordinate = AttendanceTestHelper.createAttendanceDTO(0L, COMPANY_ID, EMP_ID_1, "EMP101", "Trần A", 1, "IT", date, null, null, BigDecimal.ZERO, null);
        when(attendanceService.getTeamAttendance(COMPANY_ID, MANAGER_EMP_ID, date)).thenReturn(List.of(stubSubordinate));

        AttendanceDTO checkedInDto = AttendanceTestHelper.createAttendanceDTO(1L, COMPANY_ID, EMP_ID_1, "EMP101", "Trần A",
                1, "IT", date, time, null, BigDecimal.ZERO, "Late");
        when(attendanceService.checkIn(COMPANY_ID, EMP_ID_1, date, time)).thenReturn(checkedInDto);

        controller.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_OK);
        String json = responseWriter.toString();
        assertTrue(json.contains("\"success\":true"));
        assertTrue(json.contains("\"status\":\"Late\""));
    }

    @Test
    public void testCheckIn_DuplicateCheckIn_ReturnsBadRequest() throws Exception {
        when(request.getServletPath()).thenReturn(ApiRoutes.ATTENDANCE_CHECK_IN);
        when(request.getParameter("employeeId")).thenReturn(String.valueOf(EMP_ID_1));
        when(request.getParameter("date")).thenReturn("2026-10-30");
        when(request.getParameter("time")).thenReturn("08:35");

        LocalDate date = LocalDate.of(2026, 10, 30);
        AttendanceDTO stubSubordinate = AttendanceTestHelper.createAttendanceDTO(0L, COMPANY_ID, EMP_ID_1, "EMP101", "Trần A", 1, "IT", date, null, null, BigDecimal.ZERO, null);
        when(attendanceService.getTeamAttendance(COMPANY_ID, MANAGER_EMP_ID, date)).thenReturn(List.of(stubSubordinate));

        when(attendanceService.checkIn(eq(COMPANY_ID), eq(EMP_ID_1), any(), any()))
                .thenThrow(new IllegalArgumentException("Nhân viên này đã được điểm danh vào ca hôm nay lúc 08:20"));

        controller.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
        String json = responseWriter.toString();
        assertTrue(json.contains("\"success\":false"));
        assertTrue(json.contains("đã được điểm danh vào ca"));
    }

    @Test
    public void testCheckIn_MissingEmployeeId_ReturnsBadRequest() throws Exception {
        when(request.getServletPath()).thenReturn(ApiRoutes.ATTENDANCE_CHECK_IN);
        when(request.getParameter("employeeId")).thenReturn("");

        controller.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
        String json = responseWriter.toString();
        assertTrue(json.contains("\"success\":false"));
        assertTrue(json.contains("Vui lòng chọn nhân viên"));
    }

    @Test
    public void testCheckIn_InvalidCsrfToken_ReturnsForbidden() throws Exception {
        when(request.getServletPath()).thenReturn(ApiRoutes.ATTENDANCE_CHECK_IN);
        when(request.getParameter("csrfToken")).thenReturn("invalid-or-expired-token");

        controller.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_FORBIDDEN);
        String json = responseWriter.toString();
        assertTrue(json.contains("\"success\":false"));
        assertTrue(json.contains("Phiên làm việc đã thay đổi"));
    }

    // ==========================================
    // 4. Check-Out Tests (POST /attendance/check-out)
    // ==========================================

    @Test
    public void testCheckOut_OnTime_ReturnsSuccess() throws Exception {
        when(request.getServletPath()).thenReturn(ApiRoutes.ATTENDANCE_CHECK_OUT);
        when(request.getParameter("employeeId")).thenReturn(String.valueOf(EMP_ID_1));
        when(request.getParameter("date")).thenReturn("2026-10-30");
        when(request.getParameter("time")).thenReturn("17:35");

        LocalDate date = LocalDate.of(2026, 10, 30);
        LocalTime time = LocalTime.of(17, 35);

        AttendanceDTO stubSubordinate = AttendanceTestHelper.createAttendanceDTO(0L, COMPANY_ID, EMP_ID_1, "EMP101", "Trần A", 1, "IT", date, null, null, BigDecimal.ZERO, null);
        when(attendanceService.getTeamAttendance(COMPANY_ID, MANAGER_EMP_ID, date)).thenReturn(List.of(stubSubordinate));

        AttendanceDTO checkedOutDto = AttendanceTestHelper.createAttendanceDTO(1L, COMPANY_ID, EMP_ID_1, "EMP101", "Trần A",
                1, "IT", date, LocalTime.of(8, 30), time, new BigDecimal("8.00"), "Present");
        when(attendanceService.checkOut(COMPANY_ID, EMP_ID_1, date, time)).thenReturn(checkedOutDto);

        controller.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_OK);
        String json = responseWriter.toString();
        assertTrue(json.contains("\"success\":true"));
        assertTrue(json.contains("Điểm danh tan ca thành công"));
        assertTrue(json.contains("\"status\":\"Present\""));
    }

    @Test
    public void testCheckOut_EarlyLeave_ReturnsSuccessWithEarlyLeaveStatus() throws Exception {
        when(request.getServletPath()).thenReturn(ApiRoutes.ATTENDANCE_CHECK_OUT);
        when(request.getParameter("employeeId")).thenReturn(String.valueOf(EMP_ID_1));
        when(request.getParameter("date")).thenReturn("2026-10-30");
        when(request.getParameter("time")).thenReturn("16:45");

        LocalDate date = LocalDate.of(2026, 10, 30);
        LocalTime time = LocalTime.of(16, 45);

        AttendanceDTO stubSubordinate = AttendanceTestHelper.createAttendanceDTO(0L, COMPANY_ID, EMP_ID_1, "EMP101", "Trần A", 1, "IT", date, null, null, BigDecimal.ZERO, null);
        when(attendanceService.getTeamAttendance(COMPANY_ID, MANAGER_EMP_ID, date)).thenReturn(List.of(stubSubordinate));

        AttendanceDTO checkedOutDto = AttendanceTestHelper.createAttendanceDTO(1L, COMPANY_ID, EMP_ID_1, "EMP101", "Trần A",
                1, "IT", date, LocalTime.of(8, 30), time, new BigDecimal("7.25"), "Early Leave");
        when(attendanceService.checkOut(COMPANY_ID, EMP_ID_1, date, time)).thenReturn(checkedOutDto);

        controller.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_OK);
        String json = responseWriter.toString();
        assertTrue(json.contains("\"success\":true"));
        assertTrue(json.contains("\"status\":\"Early Leave\""));
    }

    @Test
    public void testCheckOut_WithoutCheckInRecord_ReturnsBadRequest() throws Exception {
        when(request.getServletPath()).thenReturn(ApiRoutes.ATTENDANCE_CHECK_OUT);
        when(request.getParameter("employeeId")).thenReturn(String.valueOf(EMP_ID_1));
        when(request.getParameter("date")).thenReturn("2026-10-30");
        when(request.getParameter("time")).thenReturn("17:30");

        LocalDate date = LocalDate.of(2026, 10, 30);
        AttendanceDTO stubSubordinate = AttendanceTestHelper.createAttendanceDTO(0L, COMPANY_ID, EMP_ID_1, "EMP101", "Trần A", 1, "IT", date, null, null, BigDecimal.ZERO, null);
        when(attendanceService.getTeamAttendance(COMPANY_ID, MANAGER_EMP_ID, date)).thenReturn(List.of(stubSubordinate));

        when(attendanceService.checkOut(eq(COMPANY_ID), eq(EMP_ID_1), any(), any()))
                .thenThrow(new IllegalArgumentException("Nhân viên chưa có bản ghi vào ca (Check-in). Hãy thực hiện điểm danh vào ca trước."));

        controller.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
        String json = responseWriter.toString();
        assertTrue(json.contains("\"success\":false"));
        assertTrue(json.contains("chưa có bản ghi vào ca"));
    }

    // ==========================================
    // 5. Batch Complete Tests (POST /attendance/batch-complete)
    // ==========================================

    @Test
    public void testBatchComplete_AutoMode_PerformsCheckInAndCheckOut() throws Exception {
        when(request.getServletPath()).thenReturn(ApiRoutes.ATTENDANCE_BATCH_COMPLETE);
        when(request.getParameter("employeeIds")).thenReturn("101,102");
        when(request.getParameter("action")).thenReturn("auto");
        when(request.getParameter("date")).thenReturn("2026-10-30");
        when(request.getParameter("time")).thenReturn("12:00");

        LocalDate date = LocalDate.of(2026, 10, 30);

        // Subordinate check
        AttendanceDTO initial1 = AttendanceTestHelper.createAttendanceDTO(1L, COMPANY_ID, EMP_ID_1, "EMP101", "Trần A",
                1, "IT", date, null, null, BigDecimal.ZERO, null); // Needs check-in
        AttendanceDTO initial2 = AttendanceTestHelper.createAttendanceDTO(2L, COMPANY_ID, EMP_ID_2, "EMP102", "Lê B",
                1, "IT", date, LocalTime.of(8, 30), null, BigDecimal.ZERO, "Present"); // Needs check-out

        when(attendanceService.getTeamAttendance(COMPANY_ID, MANAGER_EMP_ID, date))
                .thenReturn(List.of(initial1, initial2));

        controller.doPost(request, response);

        verify(attendanceService).checkIn(eq(COMPANY_ID), eq(EMP_ID_1), eq(date), any(LocalTime.class));
        verify(attendanceService).checkOut(eq(COMPANY_ID), eq(EMP_ID_2), eq(date), any(LocalTime.class));
        verify(response).setStatus(HttpServletResponse.SC_OK);

        String json = responseWriter.toString();
        assertTrue(json.contains("\"success\":true"));
        assertTrue(json.contains("\"updatedCount\":2"));
    }

    @Test
    public void testBatchCheckIn_PreservesAlreadyCheckedInEmployee() throws Exception {
        when(request.getServletPath()).thenReturn(ApiRoutes.ATTENDANCE_BATCH_COMPLETE);
        when(request.getParameter("employeeIds")).thenReturn("101,102");
        when(request.getParameter("action")).thenReturn("check-in");
        when(request.getParameter("date")).thenReturn("2026-10-30");
        when(request.getParameter("time")).thenReturn("08:45");

        LocalDate date = LocalDate.of(2026, 10, 30);

        // EMP 101 hasn't checked in yet; EMP 102 checked in earlier at 08:00
        AttendanceDTO initial1 = AttendanceTestHelper.createAttendanceDTO(1L, COMPANY_ID, EMP_ID_1, "EMP101", "Trần A",
                1, "IT", date, null, null, BigDecimal.ZERO, null);
        AttendanceDTO initial2 = AttendanceTestHelper.createAttendanceDTO(2L, COMPANY_ID, EMP_ID_2, "EMP102", "Lê B",
                1, "IT", date, LocalTime.of(8, 0), null, BigDecimal.ZERO, "Present");

        when(attendanceService.getTeamAttendance(COMPANY_ID, MANAGER_EMP_ID, date))
                .thenReturn(List.of(initial1, initial2));

        controller.doPost(request, response);

        // Verify only EMP 101 was checked in; EMP 102 was NOT checked out or touched
        verify(attendanceService).checkIn(eq(COMPANY_ID), eq(EMP_ID_1), eq(date), any(LocalTime.class));
        verify(attendanceService, never()).checkIn(eq(COMPANY_ID), eq(EMP_ID_2), any(), any());
        verify(attendanceService, never()).checkOut(eq(COMPANY_ID), eq(EMP_ID_2), any(), any());
        verify(response).setStatus(HttpServletResponse.SC_OK);

        String json = responseWriter.toString();
        assertTrue(json.contains("\"success\":true"));
        assertTrue(json.contains("\"updatedCount\":1"));
        assertTrue(json.contains("được giữ nguyên"));
    }

    @Test
    public void testBatchCheckOut_PreservesNotCheckedInEmployee() throws Exception {
        when(request.getServletPath()).thenReturn(ApiRoutes.ATTENDANCE_BATCH_COMPLETE);
        when(request.getParameter("employeeIds")).thenReturn("101,102");
        when(request.getParameter("action")).thenReturn("check-out");
        when(request.getParameter("date")).thenReturn("2026-10-30");
        when(request.getParameter("time")).thenReturn("17:30");

        LocalDate date = LocalDate.of(2026, 10, 30);

        // EMP 101 has not checked in; EMP 102 has checked in and is currently working
        AttendanceDTO initial1 = AttendanceTestHelper.createAttendanceDTO(1L, COMPANY_ID, EMP_ID_1, "EMP101", "Trần A",
                1, "IT", date, null, null, BigDecimal.ZERO, null);
        AttendanceDTO initial2 = AttendanceTestHelper.createAttendanceDTO(2L, COMPANY_ID, EMP_ID_2, "EMP102", "Lê B",
                1, "IT", date, LocalTime.of(8, 30), null, BigDecimal.ZERO, "Present");

        when(attendanceService.getTeamAttendance(COMPANY_ID, MANAGER_EMP_ID, date))
                .thenReturn(List.of(initial1, initial2));

        controller.doPost(request, response);

        // Verify only EMP 102 was checked out; EMP 101 was skipped without error
        verify(attendanceService, never()).checkOut(eq(COMPANY_ID), eq(EMP_ID_1), any(), any());
        verify(attendanceService).checkOut(eq(COMPANY_ID), eq(EMP_ID_2), eq(date), any(LocalTime.class));
        verify(response).setStatus(HttpServletResponse.SC_OK);

        String json = responseWriter.toString();
        assertTrue(json.contains("\"success\":true"));
        assertTrue(json.contains("\"updatedCount\":1"));
        assertTrue(json.contains("bỏ qua"));
    }

    @Test
    public void testBatchComplete_EmptyEmployeeIds_ReturnsBadRequest() throws Exception {
        when(request.getServletPath()).thenReturn(ApiRoutes.ATTENDANCE_BATCH_COMPLETE);
        when(request.getParameter("employeeIds")).thenReturn("");

        controller.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
        String json = responseWriter.toString();
        assertTrue(json.contains("\"success\":false"));
        assertTrue(json.contains("Vui lòng tích chọn"));
    }

    // ==========================================
    // 6. Company Shift Settings Tests
    // ==========================================

    @Test
    public void testGetShiftSetting_ReturnsSuccessWithConfigData() throws Exception {
        when(request.getServletPath()).thenReturn(ApiRoutes.ATTENDANCE_SHIFT_SETTING);

        CompanyShiftSetting setting = AttendanceTestHelper.createShiftSetting(
                COMPANY_ID, LocalTime.of(9, 0), LocalTime.of(18, 0), 45, 10);
        when(attendanceService.getCompanyShiftSetting(COMPANY_ID)).thenReturn(setting);
        when(attendanceService.getShiftSettingStatus(COMPANY_ID)).thenReturn(
                new AttendanceService.ShiftSettingStatus(true, false, "OK", setting));

        controller.doGet(request, response);

        verify(response).setStatus(HttpServletResponse.SC_OK);
        String json = responseWriter.toString();
        assertTrue(json.contains("\"success\":true"));
        assertTrue(json.contains("\"shiftStartTime\":\"09:00:00\""));
        assertTrue(json.contains("\"shiftEndTime\":\"18:00:00\""));
        assertTrue(json.contains("\"lunchBreakMinutes\":45"));
        assertTrue(json.contains("\"gracePeriodMinutes\":10"));
    }

    @Test
    public void testSaveShiftSetting_ManagerRole_ReturnsForbidden() throws Exception {
        when(request.getServletPath()).thenReturn(ApiRoutes.ATTENDANCE_SHIFT_SETTING);
        when(request.getParameter("shiftStartTime")).thenReturn("09:00");
        when(request.getParameter("shiftEndTime")).thenReturn("18:00");

        controller.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_FORBIDDEN);
        String json = responseWriter.toString();
        assertTrue(json.contains("\"success\":false"));
        assertTrue(json.contains("Chỉ Quản trị viên công ty"));
    }

    @Test
    public void testSaveShiftSetting_AdminSuccess() throws Exception {
        AttendanceTestHelper.setupMockSession(request, adminPrincipal);

        when(request.getServletPath()).thenReturn(ApiRoutes.ATTENDANCE_SHIFT_SETTING);
        when(request.getParameter("shiftStartTime")).thenReturn("09:00");
        when(request.getParameter("shiftEndTime")).thenReturn("18:00");
        when(request.getParameter("lunchBreakMinutes")).thenReturn("60");
        when(request.getParameter("gracePeriodMinutes")).thenReturn("15");
        when(request.getParameter("timesheetCutoffDay")).thenReturn("5");

        CompanyShiftSetting updated = AttendanceTestHelper.createShiftSetting(
                COMPANY_ID, LocalTime.of(9, 0), LocalTime.of(18, 0), 60, 15);
        updated.setTimesheetCutoffDay(5);
        when(attendanceService.getCompanyShiftSetting(COMPANY_ID)).thenReturn(updated);
        when(attendanceService.saveCompanyShiftSetting(any(CompanyShiftSetting.class), eq(COMPANY_ID), anyInt()))
                .thenReturn("Cập nhật cấu hình giờ giấc và ngày chốt công thành công");
        when(attendanceService.getShiftSettingStatus(COMPANY_ID)).thenReturn(
                new AttendanceService.ShiftSettingStatus(false, false, "OK", updated));

        controller.doPost(request, response);

        verify(attendanceService).saveCompanyShiftSetting(any(CompanyShiftSetting.class), eq(COMPANY_ID), anyInt());
        verify(response).setStatus(HttpServletResponse.SC_OK);
        String json = responseWriter.toString();
        assertTrue(json.contains("\"success\":true"));
        assertTrue(json.contains("Cập nhật cấu hình giờ giấc và ngày chốt công thành công"));
    }

    @Test
    public void testSaveShiftSetting_EmployeeRole_ReturnsForbidden() throws Exception {
        // Change session user to EMPLOYEE
        AttendanceTestHelper.setupMockSession(request, employeePrincipal);

        when(request.getServletPath()).thenReturn(ApiRoutes.ATTENDANCE_SHIFT_SETTING);
        when(request.getParameter("shiftStartTime")).thenReturn("09:00");
        when(request.getParameter("shiftEndTime")).thenReturn("18:00");

        controller.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_FORBIDDEN);
        String json = responseWriter.toString();
        assertTrue(json.contains("\"success\":false"));
        assertTrue(json.contains("Chỉ Quản trị viên công ty"));
    }

    @Test
    public void testSaveShiftSetting_EndTimeBeforeStartTime_ReturnsBadRequest() throws Exception {
        AttendanceTestHelper.setupMockSession(request, adminPrincipal);

        when(request.getServletPath()).thenReturn(ApiRoutes.ATTENDANCE_SHIFT_SETTING);
        when(request.getParameter("shiftStartTime")).thenReturn("18:00");
        when(request.getParameter("shiftEndTime")).thenReturn("09:00");

        controller.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
        String json = responseWriter.toString();
        assertTrue(json.contains("\"success\":false"));
        assertTrue(json.contains("sau giờ bắt đầu ca"));
    }

    @Test
    public void testSaveShiftSetting_NegativeLunchBreak_ReturnsBadRequest() throws Exception {
        AttendanceTestHelper.setupMockSession(request, adminPrincipal);

        when(request.getServletPath()).thenReturn(ApiRoutes.ATTENDANCE_SHIFT_SETTING);
        when(request.getParameter("shiftStartTime")).thenReturn("08:30");
        when(request.getParameter("shiftEndTime")).thenReturn("17:30");
        when(request.getParameter("lunchBreakMinutes")).thenReturn("-20");

        controller.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
        String json = responseWriter.toString();
        assertTrue(json.contains("\"success\":false"));
        assertTrue(json.contains("từ 0 đến 240 phút"));
    }

    @Test
    public void testSaveShiftSetting_InvalidCutoffDay_ReturnsBadRequest() throws Exception {
        AttendanceTestHelper.setupMockSession(request, adminPrincipal);

        when(request.getServletPath()).thenReturn(ApiRoutes.ATTENDANCE_SHIFT_SETTING);
        when(request.getParameter("shiftStartTime")).thenReturn("08:30");
        when(request.getParameter("shiftEndTime")).thenReturn("17:30");
        when(request.getParameter("timesheetCutoffDay")).thenReturn("35");

        controller.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
        String json = responseWriter.toString();
        assertTrue(json.contains("\"success\":false"));
        assertTrue(json.contains("từ ngày 1 đến ngày 31"));
    }

    // ==========================================
    // 7. Manual Attendance Adjustment (POST /attendance/manual-adjust)
    // ==========================================

    @Test
    public void testManualAdjust_Success_Today_ReturnsAdjustedRecord() throws Exception {
        LocalDate today = LocalDate.now();
        when(request.getServletPath()).thenReturn(ApiRoutes.ATTENDANCE_MANUAL_ADJUST);
        when(request.getParameter("employeeId")).thenReturn(String.valueOf(EMP_ID_1));
        when(request.getParameter("date")).thenReturn(today.toString());
        when(request.getParameter("checkInTime")).thenReturn("08:15");
        when(request.getParameter("checkOutTime")).thenReturn("17:30");
        when(request.getParameter("note")).thenReturn("Quên quẹt thẻ sáng");

        AttendanceDTO empDto = AttendanceTestHelper.createAttendanceDTO(0L, COMPANY_ID, EMP_ID_1, "NV-101", "Lê Văn A", 1, "IT", today, null, null, BigDecimal.ZERO, "Absent");
        when(attendanceService.getTeamAttendance(COMPANY_ID, MANAGER_EMP_ID, today)).thenReturn(List.of(empDto));

        AttendanceDTO resultDto = AttendanceTestHelper.createAttendanceDTO(1L, COMPANY_ID, EMP_ID_1, "NV-101", "Lê Văn A", 1, "IT", today, LocalTime.of(8, 15), LocalTime.of(17, 30), BigDecimal.valueOf(8.25), "Present");
        resultDto.setWorkCredit(BigDecimal.ONE.setScale(1));
        resultDto.setManualAdjusted(true);
        resultDto.setNote("Quên quẹt thẻ sáng");

        when(attendanceService.manualAdjustAttendance(eq(COMPANY_ID), eq(EMP_ID_1), eq(today),
                eq(LocalTime.of(8, 15)), eq(LocalTime.of(17, 30)), eq("Quên quẹt thẻ sáng"), eq(10)))
                .thenReturn(resultDto);

        controller.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_OK);
        String json = responseWriter.toString();
        assertTrue(json.contains("\"success\":true"));
        assertTrue(json.contains("Điểm danh bổ sung thành công"));
        assertTrue(json.contains("\"manualAdjusted\":true"));
        assertTrue(json.contains("\"workCredit\":1.0"));
    }

    @Test
    public void testManualAdjust_ManagerPastDate_ReturnsBadRequest() throws Exception {
        LocalDate pastDate = LocalDate.now().minusDays(1);
        when(request.getServletPath()).thenReturn(ApiRoutes.ATTENDANCE_MANUAL_ADJUST);
        when(request.getParameter("employeeId")).thenReturn(String.valueOf(EMP_ID_1));
        when(request.getParameter("date")).thenReturn(pastDate.toString());
        when(request.getParameter("checkInTime")).thenReturn("08:15");
        when(request.getParameter("checkOutTime")).thenReturn("17:30");

        AttendanceDTO empDto = AttendanceTestHelper.createAttendanceDTO(0L, COMPANY_ID, EMP_ID_1, "NV-101", "Lê Văn A", 1, "IT", pastDate, null, null, BigDecimal.ZERO, "Absent");
        when(attendanceService.getTeamAttendance(COMPANY_ID, MANAGER_EMP_ID, pastDate)).thenReturn(List.of(empDto));

        controller.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
        String json = responseWriter.toString();
        assertTrue(json.contains("\"success\":false"));
        assertTrue(json.contains("Chức năng điểm danh bổ sung chỉ áp dụng cho ngày hôm nay. Đối với các ngày trước đó, vui lòng duyệt Đơn giải trình chấm công của nhân viên."));
    }

    @Test
    public void testManualAdjust_CheckOutBeforeCheckIn_ReturnsBadRequest() throws Exception {
        LocalDate date = LocalDate.now();
        when(request.getServletPath()).thenReturn(ApiRoutes.ATTENDANCE_MANUAL_ADJUST);
        when(request.getParameter("employeeId")).thenReturn(String.valueOf(EMP_ID_1));
        when(request.getParameter("date")).thenReturn(date.toString());
        when(request.getParameter("checkInTime")).thenReturn("17:30");
        when(request.getParameter("checkOutTime")).thenReturn("08:30");

        AttendanceDTO empDto = AttendanceTestHelper.createAttendanceDTO(0L, COMPANY_ID, EMP_ID_1, "NV-101", "Lê Văn A", 1, "IT", date, null, null, BigDecimal.ZERO, "Absent");
        when(attendanceService.getTeamAttendance(COMPANY_ID, MANAGER_EMP_ID, date)).thenReturn(List.of(empDto));

        when(attendanceService.manualAdjustAttendance(eq(COMPANY_ID), eq(EMP_ID_1), eq(date),
                eq(LocalTime.of(17, 30)), eq(LocalTime.of(8, 30)), any(), any()))
                .thenThrow(new IllegalArgumentException("Thời gian tan ca (Check-out) không thể sớm hơn thời gian vào ca (Check-in)."));

        controller.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
        String json = responseWriter.toString();
        assertTrue(json.contains("\"success\":false"));
        assertTrue(json.contains("không thể sớm hơn"));
    }

    @Test
    public void testManualAdjust_MissingDate_ReturnsBadRequest() throws Exception {
        when(request.getServletPath()).thenReturn(ApiRoutes.ATTENDANCE_MANUAL_ADJUST);
        when(request.getParameter("employeeId")).thenReturn(String.valueOf(EMP_ID_1));
        when(request.getParameter("date")).thenReturn("");

        controller.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
        String json = responseWriter.toString();
        assertTrue(json.contains("\"success\":false"));
        assertTrue(json.contains("Vui lòng chọn ngày"));
    }

    @Test
    public void testManualAdjust_MissingEmployeeId_ReturnsBadRequest() throws Exception {
        when(request.getServletPath()).thenReturn(ApiRoutes.ATTENDANCE_MANUAL_ADJUST);
        when(request.getParameter("employeeId")).thenReturn("");
        when(request.getParameter("date")).thenReturn("2026-10-25");

        controller.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
        String json = responseWriter.toString();
        assertTrue(json.contains("\"success\":false"));
        assertTrue(json.contains("Vui lòng chọn nhân viên"));
    }

    @Test
    public void testManualAdjust_EmployeeRole_ReturnsForbidden() throws Exception {
        AttendanceTestHelper.setupMockSession(request, employeePrincipal);

        when(request.getServletPath()).thenReturn(ApiRoutes.ATTENDANCE_MANUAL_ADJUST);
        when(request.getParameter("employeeId")).thenReturn(String.valueOf(EMP_ID_1));
        when(request.getParameter("date")).thenReturn("2026-10-25");

        controller.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_FORBIDDEN);
        String json = responseWriter.toString();
        assertTrue(json.contains("\"success\":false"));
        assertTrue(json.contains("Bạn không có quyền thực hiện điểm danh bổ sung"));
    }

    @Test
    public void testManualAdjust_ManagerNonSubordinate_ReturnsBadRequest() throws Exception {
        LocalDate date = LocalDate.now();
        when(request.getServletPath()).thenReturn(ApiRoutes.ATTENDANCE_MANUAL_ADJUST);
        when(request.getParameter("employeeId")).thenReturn(String.valueOf(999)); // Not in manager team
        when(request.getParameter("date")).thenReturn(date.toString());

        when(attendanceService.getTeamAttendance(COMPANY_ID, MANAGER_EMP_ID, date)).thenReturn(List.of());

        controller.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
        String json = responseWriter.toString();
        assertTrue(json.contains("\"success\":false"));
        assertTrue(json.contains("Bạn chỉ được quản lý chấm công của cấp dưới trực tiếp"));
    }
}


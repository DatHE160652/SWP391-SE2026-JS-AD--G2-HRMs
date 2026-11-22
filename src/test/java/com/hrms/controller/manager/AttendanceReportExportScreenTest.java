package com.hrms.controller.manager;

import com.hrms.api.ApiRoutes;
import com.hrms.dao.manager.AttendanceReportDAO;
import com.hrms.service.manager.AttendanceService;
import com.hrms.service.companyadmin.DepartmentService;
import com.hrms.service.companyadmin.EmployeeService;
import com.hrms.service.security.TenantPrincipal;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests covering Screen 5: Báo cáo & Xuất file Chấm công (Attendance Report & Export Screen)
 * - Navigation to attendance-report.jsp (GET /attendance/attendance-report)
 * - Generate attendance report summary by department & company (GET /attendance/report)
 * - Role-based report scoping (Admin vs Manager)
 * - Punctuality rate calculation & status counts (Present, Late, Early leave, Absent, On leave)
 * - Export report to CSV (GET /attendance/export-report)
 * - Verification of UTF-8 BOM, HTTP headers (Content-Type, Content-Disposition), and CSV structure
 */
public class AttendanceReportExportScreenTest {

    private AttendanceService attendanceService;
    private EmployeeService employeeService;
    private DepartmentService departmentService;
    private AttendanceController controller;

    private HttpServletRequest request;
    private HttpServletResponse response;
    private StringWriter responseWriter;
    private ByteArrayOutputStream responseOutputStream;

    private static final int COMPANY_ID = 1;
    private static final int ADMIN_USER_ID = 1;
    private static final int MANAGER_EMP_ID = 50;

    private TenantPrincipal adminPrincipal;
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

        responseOutputStream = new ByteArrayOutputStream();
        ServletOutputStream sos = new ServletOutputStream() {
            @Override
            public boolean isReady() { return true; }
            @Override
            public void setWriteListener(WriteListener writeListener) {}
            @Override
            public void write(int b) { responseOutputStream.write(b); }
        };
        when(response.getOutputStream()).thenReturn(sos);

        adminPrincipal = AttendanceTestHelper.createPrincipal(ADMIN_USER_ID, COMPANY_ID, null, "COMPANY_ADMIN", "Admin Công ty");
        managerPrincipal = AttendanceTestHelper.createPrincipal(10, COMPANY_ID, MANAGER_EMP_ID, "MANAGER", "Nguyễn Quản Lý");

        AttendanceTestHelper.setupMockSession(request, adminPrincipal);
    }

    // ==========================================
    // 1. Navigation to Report View
    // ==========================================

    @Test
    public void testViewReportPage_ForwardsToReportJsp() throws Exception {
        when(request.getServletPath()).thenReturn(ApiRoutes.ATTENDANCE_PAGE_REPORT);
        RequestDispatcher dispatcher = mock(RequestDispatcher.class);
        when(request.getRequestDispatcher(ApiRoutes.VIEW_ATTENDANCE_REPORT)).thenReturn(dispatcher);

        controller.doGet(request, response);

        verify(dispatcher).forward(request, response);
    }

    // ==========================================
    // 2. Attendance Report Data Generation (GET /attendance/report)
    // ==========================================

    @Test
    public void testGetAttendanceReport_AdminRole_ReturnsCompanyWideReport() throws Exception {
        when(request.getServletPath()).thenReturn(ApiRoutes.ATTENDANCE_REPORT);
        when(request.getParameter("month")).thenReturn("10");
        when(request.getParameter("year")).thenReturn("2026");
        when(request.getParameter("departmentId")).thenReturn("1");

        AttendanceReportDAO r1 = AttendanceTestHelper.createReport(
                101, "EMP101", "Nguyễn Văn A", "Phòng IT",
                20, new BigDecimal("160.00"), 18, 1, 1, 0, 0, 90.0);
        AttendanceReportDAO r2 = AttendanceTestHelper.createReport(
                102, "EMP102", "Trần Thị B", "Phòng IT",
                22, new BigDecimal("176.00"), 22, 0, 0, 0, 0, 100.0);

        when(attendanceService.getAttendanceReport(COMPANY_ID, 1, 10, 2026)).thenReturn(List.of(r1, r2));

        controller.doGet(request, response);

        verify(attendanceService).getAttendanceReport(COMPANY_ID, 1, 10, 2026);
        verify(response).setStatus(HttpServletResponse.SC_OK);

        String json = responseWriter.toString();
        assertTrue(json.contains("\"success\":true"));
        assertTrue(json.contains("\"data\":["));
        assertTrue(json.contains("EMP101"));
        assertTrue(json.contains("EMP102"));
        assertTrue(json.contains("\"punctualityRate\":90.0"));
        assertTrue(json.contains("\"punctualityRate\":100.0"));
        assertTrue(json.contains("\"totalHours\":160.00"));
    }

    @Test
    public void testGetAttendanceReport_ManagerRole_CallsManagerSpecificReport() throws Exception {
        AttendanceTestHelper.setupMockSession(request, managerPrincipal);

        when(request.getServletPath()).thenReturn(ApiRoutes.ATTENDANCE_REPORT);
        when(request.getParameter("month")).thenReturn("10");
        when(request.getParameter("year")).thenReturn("2026");

        AttendanceReportDAO subReport = AttendanceTestHelper.createReport(
                101, "EMP101", "Cấp Dưới", "Phòng Kỹ Thuật",
                21, new BigDecimal("168.00"), 19, 1, 1, 0, 0, 90.5);

        when(attendanceService.getManagerAttendanceReport(COMPANY_ID, MANAGER_EMP_ID, null, 10, 2026))
                .thenReturn(List.of(subReport));

        controller.doGet(request, response);

        verify(attendanceService).getManagerAttendanceReport(COMPANY_ID, MANAGER_EMP_ID, null, 10, 2026);
        verify(response).setStatus(HttpServletResponse.SC_OK);

        String json = responseWriter.toString();
        assertTrue(json.contains("Cấp Dưới"));
        assertTrue(json.contains("\"punctualityRate\":90.5"));
    }

    // ==========================================
    // 3. Export CSV Report (GET /attendance/export-report)
    // ==========================================

    @Test
    public void testExportCsvReport_SetsHeadersAndWritesUtf8Bom() throws Exception {
        when(request.getServletPath()).thenReturn(ApiRoutes.ATTENDANCE_EXPORT_REPORT);
        when(request.getParameter("month")).thenReturn("10");
        when(request.getParameter("year")).thenReturn("2026");

        AttendanceReportDAO r1 = AttendanceTestHelper.createReport(
                101, "EMP101", "Nguyễn \"Văn\" A", "Phòng Kỹ Thuật",
                20, new BigDecimal("160.00"), 18, 1, 1, 0, 0, 90.0);

        when(attendanceService.getAttendanceReport(COMPANY_ID, null, 10, 2026)).thenReturn(List.of(r1));

        // Use the real exportCsvReport service logic to test the actual output format
        AttendanceService realService = new AttendanceService();
        String expectedCsv = realService.exportCsvReport(List.of(r1), 10, 2026);
        when(attendanceService.exportCsvReport(anyList(), eq(10), eq(2026))).thenReturn(expectedCsv);

        controller.doGet(request, response);

        verify(response).setContentType("text/csv; charset=UTF-8");
        verify(response).setHeader("Content-Disposition", "attachment; filename=\"Bao_Cao_Cham_Cong_2026_10.csv\"");

        byte[] outputBytes = responseOutputStream.toByteArray();
        assertTrue(outputBytes.length > 0);

        String csvString = new String(outputBytes, StandardCharsets.UTF_8);

        // Verify UTF-8 BOM present
        assertTrue(csvString.startsWith("\uFEFF"));

        // Verify title & headers
        assertTrue(csvString.contains("BÁO CÁO TỔNG HỢP CHẤM CÔNG THÁNG 10/2026"));
        assertTrue(csvString.contains("Mã Nhân Viên,Họ và Tên,Phòng Ban,Chức Danh,Số Ngày Đi Làm,Tổng Giờ Làm,Đúng Giờ,Đi Muộn,Về Sớm,Vắng Mặt,Nghỉ Phép,Tỉ Lệ Đúng Giờ (%)"));

        // Verify escaped quote in name: "Nguyễn ""Văn"" A"
        assertTrue(csvString.contains("\"Nguyễn \"\"Văn\"\" A\""));

        // Verify metrics
        assertTrue(csvString.contains("\"Phòng Kỹ Thuật\""));
        assertTrue(csvString.contains("90.0%"));
    }

    @Test
    public void testServiceExportCsvReport_EmptyListProducesCleanHeader() {
        AttendanceService service = new AttendanceService();
        String csv = service.exportCsvReport(List.of(), 5, 2026);

        assertTrue(csv.startsWith("\uFEFF"));
        assertTrue(csv.contains("THÁNG 5/2026"));
        assertTrue(csv.contains("Mã Nhân Viên,Họ và Tên,Phòng Ban"));
    }
}


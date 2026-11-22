package com.hrms.controller.export;

import com.hrms.dao.manager.AttendanceDAO.AttendanceDTO;
import com.hrms.dao.manager.LeaveRequestDAO;
import com.hrms.model.Department;
import com.hrms.model.Employee;
import com.hrms.model.LeaveRequest;
import com.hrms.model.Position;
import com.hrms.service.manager.AttendanceService;
import com.hrms.service.companyadmin.DepartmentService;
import com.hrms.service.companyadmin.EmployeeLifecycleService;
import com.hrms.service.companyadmin.EmployeeService;
import com.hrms.service.export.GeneralExportExcelService;
import com.hrms.service.manager.LeaveService;
import com.hrms.service.companyadmin.PositionService;
import com.hrms.service.security.TenantPrincipal;
import com.hrms.util.SecuritySupport;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Date;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class DataExportControllerTest {

    private EmployeeService mockEmployeeService;
    private LeaveService mockLeaveService;
    private LeaveRequestDAO mockLeaveDAO;
    private EmployeeLifecycleService mockLifecycleService;
    private DepartmentService mockDepartmentService;
    private PositionService mockPositionService;
    private AttendanceService mockAttendanceService;
    private GeneralExportExcelService exportExcelService;
    private DataExportController controller;

    private HttpServletRequest request;
    private HttpServletResponse response;
    private HttpSession session;
    private ByteArrayOutputStream outStream;

    private static final int COMPANY_ID = 10;

    @Before
    public void setUp() throws Exception {
        mockEmployeeService = mock(EmployeeService.class);
        mockLeaveService = mock(LeaveService.class);
        mockLeaveDAO = mock(LeaveRequestDAO.class);
        when(mockLeaveService.getLeaveRequestDAO()).thenReturn(mockLeaveDAO);
        mockLifecycleService = mock(EmployeeLifecycleService.class);
        mockDepartmentService = mock(DepartmentService.class);
        mockPositionService = mock(PositionService.class);
        mockAttendanceService = mock(AttendanceService.class);
        exportExcelService = new GeneralExportExcelService();

        controller = new DataExportController(mockEmployeeService, mockLeaveService, mockLifecycleService,
                mockDepartmentService, mockPositionService, mockAttendanceService, exportExcelService);

        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        session = mock(HttpSession.class);

        when(request.getSession()).thenReturn(session);
        when(request.getSession(anyBoolean())).thenReturn(session);

        outStream = new ByteArrayOutputStream();
        ServletOutputStream servletOut = new ServletOutputStream() {
            @Override
            public boolean isReady() { return true; }
            @Override
            public void setWriteListener(WriteListener writeListener) {}
            @Override
            public void write(int b) { outStream.write(b); }
        };
        when(response.getOutputStream()).thenReturn(servletOut);
    }

    private void mockAdminPrincipal() {
        TenantPrincipal admin = new TenantPrincipal(1, COMPANY_ID, "TECHNOVA", "Công ty Cổ phần TechNova", "Quản trị viên", "admin@technova.vn", "COMPANY_ADMIN", 100, false);
        when(session.getAttribute(SecuritySupport.TENANT_PRINCIPAL_ATTRIBUTE)).thenReturn(admin);
    }

    private void mockEmployeePrincipal() {
        TenantPrincipal emp = new TenantPrincipal(2, COMPANY_ID, "TECHNOVA", "Công ty Cổ phần TechNova", "Nhân viên", "emp@technova.vn", "EMPLOYEE", 200, false);
        when(session.getAttribute(SecuritySupport.TENANT_PRINCIPAL_ATTRIBUTE)).thenReturn(emp);
    }

    @Test
    public void testExportEmployees_Success() throws Exception {
        mockAdminPrincipal();
        when(request.getServletPath()).thenReturn("/employees/export");

        Employee emp1 = new Employee();
        emp1.setEmployeeId(1);
        emp1.setEmployeeCode("NV-001");
        emp1.setFullName("Nguyễn Văn An");
        emp1.setDepartmentName("Phòng Kỹ thuật");
        emp1.setPositionTitle("Lập trình viên");
        emp1.setEmail("an.nguyen@technova.vn");
        emp1.setPhone("0987654321");
        emp1.setGender("Male");
        emp1.setDateOfBirth(Date.valueOf("1995-05-15"));
        emp1.setIdentityNumber("001234567890");
        emp1.setStatus("Active");

        when(mockEmployeeService.getAllEmployees(COMPANY_ID)).thenReturn(List.of(emp1));

        controller.doGet(request, response);

        verify(response).setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        ArgumentCaptor<String> headerCaptor = ArgumentCaptor.forClass(String.class);
        verify(response).setHeader(eq("Content-Disposition"), headerCaptor.capture());
        assertTrue(headerCaptor.getValue().contains("Danh_Sach_Nhan_Su_"));

        byte[] bytes = outStream.toByteArray();
        assertTrue("Output should not be empty", bytes.length > 0);

        try (XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            assertEquals("Danh sach nhan su", wb.getSheetAt(0).getSheetName());
            assertEquals("NV-001", wb.getSheetAt(0).getRow(4).getCell(1).getStringCellValue());
            assertEquals("Nguyễn Văn An", wb.getSheetAt(0).getRow(4).getCell(2).getStringCellValue());
        }
    }

    /*
    @Test
    public void testExportLeaveRequests_Success() throws Exception {
        // Disabled: /leave/export-requests was refactored out in commit ce36d73
    }
    */

    @Test
    public void testExportContracts_Success() throws Exception {
        mockAdminPrincipal();
        when(request.getServletPath()).thenReturn("/lifecycle/contracts/export");

        Map<String, Object> contract = new HashMap<>();
        contract.put("ContractId", 501L);
        contract.put("ContractCode", "HDLD-2026-001");
        contract.put("EmployeeCode", "NV-003");
        contract.put("FullName", "Lê Hoàng Long");
        contract.put("ContractType", "FIXED_TERM");
        contract.put("StartDate", "2026-01-01");
        contract.put("EndDate", "2027-01-01");
        contract.put("GrossSalary", BigDecimal.valueOf(18000000));
        contract.put("TravelAllowance", BigDecimal.valueOf(1000000));
        contract.put("DisplayStatus", "ACTIVE");
        contract.put("SignedDate", "2025-12-25");
        contract.put("Notes", "Hợp đồng 1 năm");

        when(mockLifecycleService.read(any(), eq("contracts"), any())).thenReturn(List.of(contract));

        controller.doGet(request, response);

        verify(response).setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        ArgumentCaptor<String> headerCaptor = ArgumentCaptor.forClass(String.class);
        verify(response).setHeader(eq("Content-Disposition"), headerCaptor.capture());
        assertTrue(headerCaptor.getValue().contains("Danh_Sach_Hop_Dong_Lao_Dong_"));

        byte[] bytes = outStream.toByteArray();
        assertTrue("Output should not be empty", bytes.length > 0);

        try (XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            assertEquals("Danh sach hop dong", wb.getSheetAt(0).getSheetName());
            assertEquals("HDLD-2026-001", wb.getSheetAt(0).getRow(4).getCell(1).getStringCellValue());
            assertEquals("NV-003", wb.getSheetAt(0).getRow(4).getCell(2).getStringCellValue());
        }
    }

    @Test
    public void testExportDepartments_Success() throws Exception {
        mockAdminPrincipal();
        when(request.getServletPath()).thenReturn("/departments/export");

        Department d1 = new Department();
        d1.setDepartmentId(1);
        d1.setDepartmentCode("IT");
        d1.setDepartmentName("Công nghệ thông tin");
        d1.setActive(true);
        d1.setCreatedAt(LocalDateTime.of(2026, 1, 1, 8, 0));

        when(mockDepartmentService.getDepartments(COMPANY_ID)).thenReturn(List.of(d1));

        controller.doGet(request, response);

        verify(response).setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        ArgumentCaptor<String> headerCaptor = ArgumentCaptor.forClass(String.class);
        verify(response).setHeader(eq("Content-Disposition"), headerCaptor.capture());
        assertTrue(headerCaptor.getValue().contains("Danh_Sach_Phong_Ban_"));

        byte[] bytes = outStream.toByteArray();
        assertTrue("Output should not be empty", bytes.length > 0);

        try (XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            assertEquals("Danh sach phong ban", wb.getSheetAt(0).getSheetName());
            assertEquals("IT", wb.getSheetAt(0).getRow(4).getCell(1).getStringCellValue());
            assertEquals("Công nghệ thông tin", wb.getSheetAt(0).getRow(4).getCell(2).getStringCellValue());
        }
    }

    @Test
    public void testExportPositions_Success() throws Exception {
        mockAdminPrincipal();
        when(request.getServletPath()).thenReturn("/positions/export");

        Position p1 = new Position();
        p1.setPositionId(1);
        p1.setPositionTitle("Kỹ sư phần mềm");
        p1.setDepartmentName("Công nghệ thông tin");
        p1.setResponsibilityLevelName("Trưởng nhóm");
        p1.setPositionAllowance(BigDecimal.valueOf(2000000));
        p1.setCanManageEmployees(true);
        p1.setActive(true);
        p1.setDescription("Phát triển các tính năng backend");

        when(mockPositionService.getPositions(eq(COMPANY_ID), any(), anyBoolean())).thenReturn(List.of(p1));

        controller.doGet(request, response);

        verify(response).setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        ArgumentCaptor<String> headerCaptor = ArgumentCaptor.forClass(String.class);
        verify(response).setHeader(eq("Content-Disposition"), headerCaptor.capture());
        assertTrue(headerCaptor.getValue().contains("Danh_Sach_Chuc_Danh_"));

        byte[] bytes = outStream.toByteArray();
        assertTrue("Output should not be empty", bytes.length > 0);

        try (XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            assertEquals("Danh sach chuc danh", wb.getSheetAt(0).getSheetName());
            assertEquals("Kỹ sư phần mềm", wb.getSheetAt(0).getRow(4).getCell(1).getStringCellValue());
            assertEquals("Công nghệ thông tin", wb.getSheetAt(0).getRow(4).getCell(2).getStringCellValue());
        }
    }

    @Test
    public void testExportAttendance_Success() throws Exception {
        mockAdminPrincipal();
        when(request.getServletPath()).thenReturn("/attendance/export");

        AttendanceDTO a1 = new AttendanceDTO();
        a1.setEmployeeCode("NV-001");
        a1.setFullName("Nguyễn Văn An");
        a1.setDepartmentName("Phòng Kỹ thuật");
        a1.setAttendanceDate(LocalDate.of(2026, 11, 10));
        a1.setCheckInTime(java.time.LocalTime.of(8, 25));
        a1.setCheckOutTime(java.time.LocalTime.of(17, 35));
        a1.setTotalHours(BigDecimal.valueOf(8.00));
        a1.setStatus("ON_TIME");
        a1.setNote("Điểm danh đúng giờ");

        when(mockAttendanceService.searchCompanyAttendance(eq(COMPANY_ID), any(), any(), any(), any(), any()))
                .thenReturn(List.of(a1));

        controller.doGet(request, response);

        verify(response).setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        ArgumentCaptor<String> headerCaptor = ArgumentCaptor.forClass(String.class);
        verify(response).setHeader(eq("Content-Disposition"), headerCaptor.capture());
        assertTrue(headerCaptor.getValue().contains("Bang_Diem_Danh_Cham_Cong_"));

        byte[] bytes = outStream.toByteArray();
        assertTrue("Output should not be empty", bytes.length > 0);

        try (XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            assertEquals("Cham cong", wb.getSheetAt(0).getSheetName());
            assertEquals("1", wb.getSheetAt(0).getRow(4).getCell(0).getStringCellValue());
            assertEquals("NV-001", wb.getSheetAt(0).getRow(4).getCell(1).getStringCellValue());
            assertEquals("Nguyễn Văn An", wb.getSheetAt(0).getRow(4).getCell(2).getStringCellValue());
            assertEquals("Phòng Kỹ thuật", wb.getSheetAt(0).getRow(4).getCell(3).getStringCellValue());
            assertEquals("2026-11-10", wb.getSheetAt(0).getRow(4).getCell(4).getStringCellValue());
            assertEquals("08:25", wb.getSheetAt(0).getRow(4).getCell(5).getStringCellValue());
            assertEquals("17:35", wb.getSheetAt(0).getRow(4).getCell(6).getStringCellValue());
        }
    }

    @Test
    public void testExportEmployees_ForbiddenForEmployeeRole() throws Exception {
        mockEmployeePrincipal();
        when(request.getServletPath()).thenReturn("/employees/export");

        controller.doGet(request, response);

        verify(response).sendError(eq(HttpServletResponse.SC_FORBIDDEN), anyString());
    }
}

package com.hrms.controller.export;

import com.hrms.dao.AttendanceDAO.AttendanceDTO;
import com.hrms.model.Department;
import com.hrms.model.Employee;
import com.hrms.model.Position;
import com.hrms.service.AttendanceService;
import com.hrms.service.DepartmentService;
import com.hrms.service.EmployeeLifecycleService;
import com.hrms.service.EmployeeService;
import com.hrms.service.GeneralExportExcelService;
import com.hrms.service.LeaveService;
import com.hrms.service.PositionService;
import com.hrms.service.security.TenantPrincipal;
import com.hrms.util.SecuritySupport;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * Controller chuyên trách xuất file Excel (.xlsx) cho:
 * 1. /employees/export : Danh sách nhân sự
 * 2. /lifecycle/contracts/export : Danh sách hợp đồng lao động
 * 3. /departments/export : Danh sách phòng ban
 * 4. /positions/export : Danh sách chức danh
 * 5. /lifecycle/organization/export : Sơ đồ / Cơ cấu tổ chức
 * 6. /attendance/export : Danh sách điểm danh chấm công
 */
@WebServlet(name = "DataExportController", urlPatterns = {
        "/employees/export",
        "/lifecycle/contracts/export",
        "/departments/export",
        "/positions/export",
        "/lifecycle/organization/export",
        "/attendance/export"
})
public class DataExportController extends HttpServlet {

    private final EmployeeService employeeService;
    private final LeaveService leaveService;
    private final EmployeeLifecycleService lifecycleService;
    private final DepartmentService departmentService;
    private final PositionService positionService;
    private final AttendanceService attendanceService;
    private final GeneralExportExcelService exportExcelService;

    public DataExportController() {
        this(new EmployeeService(), new LeaveService(), new EmployeeLifecycleService(),
                new DepartmentService(), new PositionService(), new AttendanceService(), new GeneralExportExcelService());
    }

    public DataExportController(EmployeeService employeeService, LeaveService leaveService,
                                EmployeeLifecycleService lifecycleService, GeneralExportExcelService exportExcelService) {
        this(employeeService, leaveService, lifecycleService, new DepartmentService(), new PositionService(), new AttendanceService(), exportExcelService);
    }

    public DataExportController(EmployeeService employeeService, LeaveService leaveService,
                                EmployeeLifecycleService lifecycleService, DepartmentService departmentService,
                                PositionService positionService, GeneralExportExcelService exportExcelService) {
        this(employeeService, leaveService, lifecycleService, departmentService, positionService, new AttendanceService(), exportExcelService);
    }

    public DataExportController(EmployeeService employeeService, LeaveService leaveService,
                                EmployeeLifecycleService lifecycleService, DepartmentService departmentService,
                                PositionService positionService, AttendanceService attendanceService,
                                GeneralExportExcelService exportExcelService) {
        this.employeeService = employeeService;
        this.leaveService = leaveService;
        this.lifecycleService = lifecycleService;
        this.departmentService = departmentService;
        this.positionService = positionService;
        this.attendanceService = attendanceService;
        this.exportExcelService = exportExcelService;
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        SecuritySupport.applyNoStore(response::setHeader);
        String path = request.getServletPath();

        try {
            switch (path) {
                case "/employees/export" -> handleExportEmployees(request, response);
                case "/lifecycle/contracts/export" -> handleExportContracts(request, response);
                case "/departments/export" -> handleExportDepartments(request, response);
                case "/positions/export" -> handleExportPositions(request, response);
                case "/lifecycle/organization/export" -> handleExportOrganization(request, response);
                case "/attendance/export" -> handleExportAttendance(request, response);
                default -> response.sendError(HttpServletResponse.SC_NOT_FOUND);
            }
        } catch (SecurityException e) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, e.getMessage());
        } catch (SQLException e) {
            log("Database error during export: " + path, e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Lỗi truy vấn cơ sở dữ liệu: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            log("Server error during export: " + path, e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Lỗi hệ thống: " + e.getMessage());
        }
    }

    /**
     * 1. Xuất danh sách nhân sự
     */
    private void handleExportEmployees(HttpServletRequest request, HttpServletResponse response)
            throws SQLException, IOException {
        int companyId = SecuritySupport.requireCompanyId(request);
        SecuritySupport.requireOrganizationEditor(request);
        TenantPrincipal principal = SecuritySupport.getTenantPrincipal(request);
        String companyName = principal != null && principal.getCompanyName() != null ? principal.getCompanyName() : "HRMs Cloud";

        Integer departmentId = parseNullableInt(request.getParameter("departmentId"));
        String keyword = trimToNull(request.getParameter("keyword"));

        List<Employee> list = employeeService.getAllEmployees(companyId);

        if (departmentId != null && departmentId > 0) {
            list = list.stream().filter(e -> e.getDepartmentId() != null && e.getDepartmentId().equals(departmentId)).toList();
        }
        if (keyword != null && !keyword.isBlank()) {
            String kw = keyword.toLowerCase();
            list = list.stream().filter(e ->
                    (e.getFullName() != null && e.getFullName().toLowerCase().contains(kw))
                            || (e.getEmployeeCode() != null && e.getEmployeeCode().toLowerCase().contains(kw))
                            || (e.getEmail() != null && e.getEmail().toLowerCase().contains(kw))
            ).toList();
        }

        String filename = "Danh_Sach_Nhan_Su_" + LocalDate.now().format(DateTimeFormatter.ofPattern("dd_MM_yyyy")) + ".xlsx";
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");

        exportExcelService.exportEmployees(companyName, list, response.getOutputStream());
    }

/**
     * 3. Xuất danh sách hợp đồng lao động
     */
    private void handleExportContracts(HttpServletRequest request, HttpServletResponse response)
            throws SQLException, IOException {
        TenantPrincipal principal = SecuritySupport.getTenantPrincipal(request);
        lifecycleService.requireEditor(principal);
        String companyName = principal != null && principal.getCompanyName() != null ? principal.getCompanyName() : "HRMs Cloud";

        Integer employeeId = parseNullableInt(request.getParameter("employeeId"));
        String status = trimToNull(request.getParameter("status"));
        String type = trimToNull(request.getParameter("type"));
        String keyword = trimToNull(request.getParameter("keyword"));

        List<Map<String, Object>> contracts = lifecycleService.read(principal, "contracts", employeeId);

        if (status != null && !status.isBlank()) {
            contracts = contracts.stream().filter(c ->
                    status.equalsIgnoreCase(String.valueOf(c.get("DisplayStatus")))
                            || status.equalsIgnoreCase(String.valueOf(c.get("Status")))
            ).toList();
        }
        if (type != null && !type.isBlank()) {
            contracts = contracts.stream().filter(c ->
                    type.equalsIgnoreCase(String.valueOf(c.get("ContractType")))
            ).toList();
        }
        if (keyword != null && !keyword.isBlank()) {
            String kw = keyword.toLowerCase();
            contracts = contracts.stream().filter(c -> {
                String code = String.valueOf(c.get("ContractCode")).toLowerCase();
                String name = String.valueOf(c.get("FullName")).toLowerCase();
                String empCode = String.valueOf(c.get("EmployeeCode")).toLowerCase();
                return code.contains(kw) || name.contains(kw) || empCode.contains(kw);
            }).toList();
        }

        String filename = "Danh_Sach_Hop_Dong_Lao_Dong_" + LocalDate.now().format(DateTimeFormatter.ofPattern("dd_MM_yyyy")) + ".xlsx";
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");

        exportExcelService.exportContracts(companyName, contracts, response.getOutputStream());
    }

    /**
     * 4. Xuất danh sách phòng ban
     */
    private void handleExportDepartments(HttpServletRequest request, HttpServletResponse response)
            throws SQLException, IOException {
        int companyId = SecuritySupport.requireCompanyId(request);
        SecuritySupport.requireOrganizationEditor(request);
        TenantPrincipal principal = SecuritySupport.getTenantPrincipal(request);
        String companyName = principal != null && principal.getCompanyName() != null ? principal.getCompanyName() : "HRMs Cloud";

        String status = trimToNull(request.getParameter("status"));
        List<Department> list = departmentService.getDepartments(companyId);
        if ("Active".equalsIgnoreCase(status)) {
            list = list.stream().filter(Department::isActive).toList();
        } else if ("Inactive".equalsIgnoreCase(status)) {
            list = list.stream().filter(d -> !d.isActive()).toList();
        }

        String filename = "Danh_Sach_Phong_Ban_" + LocalDate.now().format(DateTimeFormatter.ofPattern("dd_MM_yyyy")) + ".xlsx";
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");

        exportExcelService.exportDepartments(companyName, list, response.getOutputStream());
    }

    /**
     * 5. Xuất danh sách chức danh
     */
    private void handleExportPositions(HttpServletRequest request, HttpServletResponse response)
            throws SQLException, IOException {
        int companyId = SecuritySupport.requireCompanyId(request);
        SecuritySupport.requireOrganizationEditor(request);
        TenantPrincipal principal = SecuritySupport.getTenantPrincipal(request);
        String companyName = principal != null && principal.getCompanyName() != null ? principal.getCompanyName() : "HRMs Cloud";

        Integer departmentId = parseNullableInt(request.getParameter("departmentId"));
        String status = trimToNull(request.getParameter("status"));
        boolean activeOnly = "Active".equalsIgnoreCase(status);

        List<Position> list = positionService.getPositions(companyId, departmentId, activeOnly);
        if ("Inactive".equalsIgnoreCase(status)) {
            list = list.stream().filter(p -> !p.isActive()).toList();
        }

        String filename = "Danh_Sach_Chuc_Danh_" + LocalDate.now().format(DateTimeFormatter.ofPattern("dd_MM_yyyy")) + ".xlsx";
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");

        exportExcelService.exportPositions(companyName, list, response.getOutputStream());
    }

    /**
     * 6. Xuất sơ đồ / cơ cấu tổ chức
     */
    private void handleExportOrganization(HttpServletRequest request, HttpServletResponse response)
            throws SQLException, IOException {
        TenantPrincipal principal = SecuritySupport.getTenantPrincipal(request);
        lifecycleService.requireEditor(principal);
        int companyId = principal.getCompanyId();
        String companyName = principal.getCompanyName() != null ? principal.getCompanyName() : "HRMs Cloud";

        Integer departmentId = parseNullableInt(request.getParameter("departmentId"));
        String keyword = trimToNull(request.getParameter("keyword"));

        List<Map<String, Object>> assignments = lifecycleService.read(principal, "organization", null);
        List<Employee> employees = employeeService.getAllEmployees(companyId);

        Map<Integer, Map<String, Object>> assignedByEmpId = new java.util.LinkedHashMap<>();
        for (Map<String, Object> a : assignments) {
            if ("Active".equalsIgnoreCase(String.valueOf(a.get("Status"))) && a.get("EmployeeId") instanceof Number num) {
                assignedByEmpId.put(num.intValue(), a);
            }
        }

        List<Map<String, Object>> combined = new java.util.ArrayList<>();
        for (Employee e : employees) {
            if (!"Active".equalsIgnoreCase(e.getStatus())) continue;
            Map<String, Object> assignment = assignedByEmpId.remove(e.getEmployeeId());
            Map<String, Object> row = new java.util.LinkedHashMap<>();
            row.put("EmployeeId", e.getEmployeeId());
            row.put("EmployeeCode", e.getEmployeeCode());
            row.put("FullName", e.getFullName());
            row.put("Email", e.getEmail());
            row.put("Phone", e.getPhone());
            row.put("DepartmentId", assignment != null ? assignment.get("DepartmentId") : null);
            row.put("DepartmentName", assignment != null ? assignment.get("DepartmentName") : null);
            row.put("PositionTitle", assignment != null ? assignment.get("PositionTitle") : null);
            row.put("LevelName", assignment != null ? assignment.get("LevelName") : null);
            row.put("ManagerName", assignment != null ? assignment.get("ManagerName") : null);
            row.put("HasAssignment", assignment != null);
            combined.add(row);
        }
        for (Map<String, Object> remaining : assignedByEmpId.values()) {
            Map<String, Object> row = new java.util.LinkedHashMap<>(remaining);
            row.put("HasAssignment", true);
            combined.add(row);
        }

        if (departmentId != null && departmentId > 0) {
            combined = combined.stream().filter(m ->
                    m.get("DepartmentId") instanceof Number num && num.intValue() == departmentId
            ).toList();
        }
        if (keyword != null && !keyword.isBlank()) {
            String kw = keyword.toLowerCase();
            combined = combined.stream().filter(m -> {
                String haystack = String.join(" ",
                        String.valueOf(m.getOrDefault("FullName", "")),
                        String.valueOf(m.getOrDefault("EmployeeCode", "")),
                        String.valueOf(m.getOrDefault("DepartmentName", "")),
                        String.valueOf(m.getOrDefault("PositionTitle", "")),
                        String.valueOf(m.getOrDefault("Email", "")),
                        String.valueOf(m.getOrDefault("ManagerName", ""))
                ).toLowerCase();
                return haystack.contains(kw);
            }).toList();
        }

        String filename = "Co_Cau_To_Chuc_" + LocalDate.now().format(DateTimeFormatter.ofPattern("dd_MM_yyyy")) + ".xlsx";
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");

        exportExcelService.exportOrganization(companyName, combined, response.getOutputStream());
    }

    /**
     * 7. Xuất danh sách điểm danh chấm công (phù hợp form với file import)
     */
    private void handleExportAttendance(HttpServletRequest request, HttpServletResponse response)
            throws SQLException, IOException {
        int companyId = SecuritySupport.requireCompanyId(request);
        TenantPrincipal principal = SecuritySupport.getTenantPrincipal(request);
        String companyName = principal != null && principal.getCompanyName() != null ? principal.getCompanyName() : "HRMs Cloud";

        Integer departmentId = parseNullableInt(request.getParameter("departmentId"));
        String status = trimToNull(request.getParameter("status"));
        if ("ALL".equalsIgnoreCase(status)) status = null;

        LocalDate fromDate = parseOptionalDate(request.getParameter("fromDate"));
        LocalDate toDate = parseOptionalDate(request.getParameter("toDate"));
        String keyword = trimToNull(request.getParameter("keyword"));

        List<AttendanceDTO> attendances = attendanceService.searchCompanyAttendance(
                companyId, departmentId, status, fromDate, toDate, keyword);

        String filename = "Bang_Diem_Danh_Cham_Cong_" + LocalDate.now().format(DateTimeFormatter.ofPattern("dd_MM_yyyy")) + ".xlsx";
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");

        exportExcelService.exportAttendances(companyName, attendances, response.getOutputStream());
    }

    private Integer parseNullableInt(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private LocalDate parseOptionalDate(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return LocalDate.parse(value.trim());
        } catch (Exception e) {
            return null;
        }
    }

    private String trimToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}

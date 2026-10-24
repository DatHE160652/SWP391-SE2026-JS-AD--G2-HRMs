package com.hrms.controller.hr;

import com.hrms.api.ApiRoutes;
import com.hrms.dao.AttendanceDAO.AttendanceDTO;
import com.hrms.dao.AttendanceReportDAO;
import com.hrms.dao.AttendanceSummaryDAO;
import com.hrms.model.Department;
import com.hrms.model.Employee;
import com.hrms.service.AttendanceService;
import com.hrms.service.DepartmentService;
import com.hrms.service.EmployeeService;
import com.hrms.service.security.TenantPrincipal;
import com.hrms.util.SecuritySupport;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Handles Attendance operations for Employee, Manager, and HR/Admin:
 * - UC29: Check-in (Manager)
 * - UC30: Check-out (Manager)
 * - UC31: View Personal Attendance History (Employee read-only)
 * - UC32: View Team Attendance (Manager)
 * - UC33: Monitor & Search Company Attendance (HR / Admin)
 * - UC36: Generate Attendance Reports & CSV Export (HR / Manager)
 */
@WebServlet(name = "AttendanceController", urlPatterns = {
        ApiRoutes.ATTENDANCE_PERSONAL,
        ApiRoutes.ATTENDANCE_TEAM,
        ApiRoutes.ATTENDANCE_PAGE_MANAGER,
        ApiRoutes.ATTENDANCE_PAGE_EMPLOYEE,
        ApiRoutes.ATTENDANCE_PAGE_COMPANY,
        ApiRoutes.ATTENDANCE_PAGE_MANAGER_PERSONAL,
        "/attendance/manager-attendance.html",
        "/attendance/employee-attendance.html",
        "/attendance/company-attendance.html",
        "/attendance/manager-personal-attendance.html",
        "/attendance/attendance-report.html",
        ApiRoutes.ATTENDANCE_PAGE_REPORT,
        ApiRoutes.ATTENDANCE_ALL,
        ApiRoutes.ATTENDANCE_CHECK_IN,
        ApiRoutes.ATTENDANCE_CHECK_OUT,
        ApiRoutes.ATTENDANCE_BATCH_COMPLETE,
        ApiRoutes.ATTENDANCE_COMPANY,
        ApiRoutes.ATTENDANCE_REPORT,
        ApiRoutes.ATTENDANCE_EXPORT_REPORT,
        ApiRoutes.ATTENDANCE_META,
        ApiRoutes.MANAGER_TEAM_ATTENDANCE,
        ApiRoutes.MANAGER_EMPLOYEE_ATTENDANCE,
        ApiRoutes.MANAGER_EMPLOYEE_ATTENDANCE_HTML,
        ApiRoutes.MANAGER_COMPANY_ATTENDANCE_HTML,
        ApiRoutes.MANAGER_ATTENDANCE_REPORT_HTML
})
public class AttendanceController extends HttpServlet {
    private final AttendanceService attendanceService = new AttendanceService();
    private final EmployeeService employeeService = new EmployeeService();
    private final DepartmentService departmentService = new DepartmentService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String path = request.getServletPath();

        try {
            SecuritySupport.requireCompanyId(request);
            request.setAttribute("organizationCsrfToken", SecuritySupport.ensurePublicCsrfToken(request.getSession()));
            if (path.endsWith(".html") && path.startsWith(ApiRoutes.ATTENDANCE_PREFIX + "/")) {
                String canonical = path.substring(0, path.length() - ".html".length());
                response.sendRedirect(request.getContextPath() + canonical);
                return;
            }

            switch (path) {
                case ApiRoutes.ATTENDANCE_PERSONAL -> handlePersonalAttendance(request, response);
                case ApiRoutes.ATTENDANCE_TEAM -> handleTeamAttendance(request, response);
                case ApiRoutes.MANAGER_TEAM_ATTENDANCE -> {
                    String accept = request.getHeader("Accept");
                    if (accept != null && accept.contains("application/json") && request.getParameter("managerId") != null) {
                        handleTeamAttendance(request, response);
                    } else {
                        response.sendRedirect(request.getContextPath() + ApiRoutes.ATTENDANCE_PAGE_MANAGER);
                    }
                }
                case ApiRoutes.ATTENDANCE_PAGE_MANAGER -> {
                    String accept = request.getHeader("Accept");
                    if (accept != null && accept.contains("application/json") && request.getParameter("managerId") != null) {
                        handleTeamAttendance(request, response);
                    } else {
                        request.getRequestDispatcher(ApiRoutes.VIEW_ATTENDANCE_MANAGER).forward(request, response);
                    }
                }
                case ApiRoutes.ATTENDANCE_PAGE_EMPLOYEE ->
                        request.getRequestDispatcher(ApiRoutes.VIEW_ATTENDANCE_EMPLOYEE).forward(request, response);
                case ApiRoutes.ATTENDANCE_PAGE_COMPANY ->
                        request.getRequestDispatcher(ApiRoutes.VIEW_ATTENDANCE_COMPANY).forward(request, response);
                case ApiRoutes.ATTENDANCE_PAGE_MANAGER_PERSONAL ->
                        request.getRequestDispatcher(ApiRoutes.VIEW_ATTENDANCE_MANAGER_PERSONAL).forward(request, response);
                case ApiRoutes.ATTENDANCE_PAGE_REPORT ->
                        request.getRequestDispatcher(ApiRoutes.VIEW_ATTENDANCE_REPORT).forward(request, response);
                case ApiRoutes.MANAGER_EMPLOYEE_ATTENDANCE, ApiRoutes.MANAGER_EMPLOYEE_ATTENDANCE_HTML ->
                        response.sendRedirect(request.getContextPath() + ApiRoutes.ATTENDANCE_PAGE_EMPLOYEE);
                case ApiRoutes.MANAGER_COMPANY_ATTENDANCE_HTML ->
                        response.sendRedirect(request.getContextPath() + ApiRoutes.ATTENDANCE_PAGE_COMPANY);
                case ApiRoutes.MANAGER_ATTENDANCE_REPORT_HTML ->
                        response.sendRedirect(request.getContextPath() + ApiRoutes.ATTENDANCE_PAGE_REPORT);
                case ApiRoutes.ATTENDANCE_ALL -> handleAllAttendance(request, response);
                case ApiRoutes.ATTENDANCE_COMPANY -> handleCompanyAttendance(request, response);
                case ApiRoutes.ATTENDANCE_REPORT -> handleAttendanceReport(request, response);
                case ApiRoutes.ATTENDANCE_EXPORT_REPORT -> handleExportReport(request, response);
                case ApiRoutes.ATTENDANCE_META -> handleMeta(request, response);
                default -> writeJson(response, HttpServletResponse.SC_NOT_FOUND, error("Không tìm thấy chức năng được yêu cầu."));
            }
        } catch (SecurityException e) {
            writeJson(response, HttpServletResponse.SC_FORBIDDEN, error(e.getMessage()));
        } catch (SQLException e) {
            writeJson(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, error("Database error: " + e.getMessage()));
        } catch (IllegalArgumentException e) {
            writeJson(response, HttpServletResponse.SC_BAD_REQUEST, error(e.getMessage()));
        } catch (Exception e) {
            writeJson(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, error("Server error: " + e.getMessage()));
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String path = request.getServletPath();

        try {
            SecuritySupport.requireCompanyId(request);
            if (!SecuritySupport.hasValidPublicCsrfToken(request)) throw new SecurityException("Phiên làm việc đã thay đổi. Vui lòng tải lại trang trước khi lưu.");
            switch (path) {
                case ApiRoutes.ATTENDANCE_CHECK_IN -> handleCheckIn(request, response);
                case ApiRoutes.ATTENDANCE_CHECK_OUT -> handleCheckOut(request, response);
                case ApiRoutes.ATTENDANCE_BATCH_COMPLETE -> handleBatchComplete(request, response);
                default -> writeJson(response, HttpServletResponse.SC_NOT_FOUND, error("Không tìm thấy chức năng được yêu cầu."));
            }
        } catch (SecurityException e) {
            writeJson(response, HttpServletResponse.SC_FORBIDDEN, error(e.getMessage()));
        } catch (SQLException e) {
            writeJson(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, error("Database error: " + e.getMessage()));
        } catch (IllegalArgumentException e) {
            writeJson(response, HttpServletResponse.SC_BAD_REQUEST, error(e.getMessage()));
        } catch (Exception e) {
            writeJson(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, error("Server error: " + e.getMessage()));
        }
    }

    /**
     * UC29: Manager performs Check-in for an employee
     */
    private void handleCheckIn(HttpServletRequest request, HttpServletResponse response) throws SQLException, IOException {
        int employeeId = parseRequiredInt(request.getParameter("employeeId"), "Vui lòng chọn nhân viên.");
        LocalDate date = parseOptionalDate(request.getParameter("date"));
        if (date == null) {
            date = LocalDate.now();
        }
        ensureCanManageAttendance(request, employeeId, date);
        LocalTime time = parseOptionalTime(request.getParameter("time"));

        AttendanceDTO result = attendanceService.checkIn(SecuritySupport.requireCompanyId(request), employeeId, date, time);
        writeJson(response, HttpServletResponse.SC_OK,
                "{\"success\":true,\"message\":\"Điểm danh vào ca thành công!\",\"data\":" + toJson(result) + "}");
    }

    /**
     * UC30: Manager performs Check-out for an employee
     */
    private void handleCheckOut(HttpServletRequest request, HttpServletResponse response) throws SQLException, IOException {
        int employeeId = parseRequiredInt(request.getParameter("employeeId"), "Vui lòng chọn nhân viên.");
        LocalDate date = parseOptionalDate(request.getParameter("date"));
        if (date == null) {
            date = LocalDate.now();
        }
        ensureCanManageAttendance(request, employeeId, date);
        LocalTime time = parseOptionalTime(request.getParameter("time"));

        AttendanceDTO result = attendanceService.checkOut(SecuritySupport.requireCompanyId(request), employeeId, date, time);
        writeJson(response, HttpServletResponse.SC_OK,
                "{\"success\":true,\"message\":\"Điểm danh tan ca thành công!\",\"data\":" + toJson(result) + "}");
    }

    /**
     * Batch check-in / check-out: Hoàn tất việc check điểm danh cho danh sách nhân sự đã chọn
     */
    private void handleBatchComplete(HttpServletRequest request, HttpServletResponse response) throws SQLException, IOException {
        String empIdsParam = request.getParameter("employeeIds");
        if (empIdsParam == null || empIdsParam.isBlank()) {
            throw new IllegalArgumentException("Vui lòng tích chọn ít nhất một nhân sự để hoàn tất điểm danh.");
        }

        String action = trimToNull(request.getParameter("action")); // "check-in", "check-out", or "auto"
        LocalDate date = parseOptionalDate(request.getParameter("date"));
        if (date == null) {
            date = LocalDate.now();
        }
        LocalTime time = parseOptionalTime(request.getParameter("time"));
        if (time == null) {
            time = LocalTime.now().withNano(0);
        }

        String[] ids = empIdsParam.split(",");
        int successCount = 0;
        List<String> notices = new java.util.ArrayList<>();

        for (String idStr : ids) {
            if (idStr == null || idStr.isBlank()) continue;
            try {
                int empId = Integer.parseInt(idStr.trim());
                ensureCanManageAttendance(request, empId, date);
                List<AttendanceDTO> currentAll = scopedAttendance(request, date);
                AttendanceDTO existing = currentAll.stream()
                        .filter(a -> a.getEmployeeId() == empId)
                        .findFirst().orElse(null);

                String act = action;
                if (act == null || "auto".equalsIgnoreCase(act)) {
                    if (existing == null || existing.getCheckInTime() == null) {
                        act = "check-in";
                    } else if (existing.getCheckOutTime() == null) {
                        act = "check-out";
                    } else {
                        continue; // Đã hoàn thành cả vào và tan ca
                    }
                }

                if ("check-in".equalsIgnoreCase(act)) {
                    if (existing == null || existing.getCheckInTime() == null) {
                        attendanceService.checkIn(SecuritySupport.requireCompanyId(request), empId, date, time);
                        successCount++;
                    }
                } else if ("check-out".equalsIgnoreCase(act)) {
                    if (existing != null && existing.getCheckInTime() != null && existing.getCheckOutTime() == null) {
                        attendanceService.checkOut(SecuritySupport.requireCompanyId(request), empId, date, time);
                        successCount++;
                    }
                }
            } catch (Exception ex) {
                notices.add(ex.getMessage());
            }
        }

        List<AttendanceDTO> updatedList = scopedAttendance(request, date);
        String msg = "Đã hoàn tất điểm danh thành công cho " + successCount + " nhân sự.";
        if (!notices.isEmpty()) {
            msg += " Lưu ý: " + String.join(" | ", notices);
        }
        writeJson(response, HttpServletResponse.SC_OK,
                "{\"success\":true,\"message\":" + quote(msg) + ",\"updatedCount\":" + successCount + ",\"data\":" + toJsonArray(updatedList) + "}");
    }

    /**
     * UC31: View personal attendance history (Employee read-only)
     */
    private void handlePersonalAttendance(HttpServletRequest request, HttpServletResponse response) throws SQLException, IOException {
        int employeeId = parseRequiredInt(request.getParameter("employeeId"), "Vui lòng chọn nhân viên.");
        TenantPrincipal principal = SecuritySupport.getTenantPrincipal(request);
        if ((isRole(principal, "EMPLOYEE") || isRole(principal, "MANAGER")) && !matchesPrincipalEmployee(principal, employeeId)) {
            throw new IllegalArgumentException("Bạn chỉ được xem dữ liệu chấm công của chính mình.");
        }
        int month = parseOptionalInt(request.getParameter("month"), LocalDate.now().getMonthValue());
        int year = parseOptionalInt(request.getParameter("year"), LocalDate.now().getYear());

        List<AttendanceDTO> list = attendanceService.getPersonalAttendance(SecuritySupport.requireCompanyId(request), employeeId, month, year);
        AttendanceSummaryDAO summary = attendanceService.getPersonalSummary(SecuritySupport.requireCompanyId(request), employeeId, month, year);

        String json = "{\"success\":true,\"data\":" + toJsonArray(list) + ",\"summary\":" + toJson(summary) + "}";
        writeJson(response, HttpServletResponse.SC_OK, json);
    }

    /**
     * UC32: View team attendance (Manager)
     */
    private void handleTeamAttendance(HttpServletRequest request, HttpServletResponse response) throws SQLException, IOException {
        int managerId = parseRequiredInt(request.getParameter("managerId"), "Vui lòng chọn quản lý.");
        LocalDate date = parseOptionalDate(request.getParameter("date"));
        if (date == null) {
            date = LocalDate.now();
        }

        TenantPrincipal principal = SecuritySupport.getTenantPrincipal(request);
        if (isRole(principal, "MANAGER") && !matchesPrincipalEmployee(principal, managerId)) throw new SecurityException("Bạn chỉ được xem nhóm do mình quản lý.");
        List<AttendanceDTO> team = attendanceService.getTeamAttendance(SecuritySupport.requireCompanyId(request), managerId, date);
        writeJson(response, HttpServletResponse.SC_OK, "{\"success\":true,\"data\":" + toJsonArray(team) + "}");
    }

    /**
     * View all employees attendance by date
     */
    private void handleAllAttendance(HttpServletRequest request, HttpServletResponse response) throws SQLException, IOException {
        LocalDate date = parseOptionalDate(request.getParameter("date"));
        if (date == null) {
            date = LocalDate.now();
        }
        List<AttendanceDTO> list = scopedAttendance(request, date);
        writeJson(response, HttpServletResponse.SC_OK, "{\"success\":true,\"data\":" + toJsonArray(list) + "}");
    }

    /**
     * UC33: Monitor & Search company-wide attendance (HR / Admin)
     */
    private void handleCompanyAttendance(HttpServletRequest request, HttpServletResponse response) throws SQLException, IOException {
        int companyId = SecuritySupport.requireCompanyId(request);
        Integer departmentId = parseNullableInt(request.getParameter("departmentId"));
        String status = trimToNull(request.getParameter("status"));
        LocalDate fromDate = parseOptionalDate(request.getParameter("fromDate"));
        LocalDate toDate = parseOptionalDate(request.getParameter("toDate"));
        String keyword = trimToNull(request.getParameter("keyword"));

        List<AttendanceDTO> list = attendanceService.searchCompanyAttendance(companyId, departmentId, status, fromDate, toDate, keyword);
        writeJson(response, HttpServletResponse.SC_OK, "{\"success\":true,\"data\":" + toJsonArray(list) + "}");
    }

    /**
     * UC36: Generate attendance reports by department
     */
    private void handleAttendanceReport(HttpServletRequest request, HttpServletResponse response) throws SQLException, IOException {
        int companyId = SecuritySupport.requireCompanyId(request);
        Integer departmentId = parseNullableInt(request.getParameter("departmentId"));
        int month = parseOptionalInt(request.getParameter("month"), LocalDate.now().getMonthValue());
        int year = parseOptionalInt(request.getParameter("year"), LocalDate.now().getYear());

        List<AttendanceReportDAO> reports = getReportRowsForPrincipal(request, companyId, departmentId, month, year);
        writeJson(response, HttpServletResponse.SC_OK, "{\"success\":true,\"data\":" + toReportJsonArray(reports) + "}");
    }

    /**
     * UC36: Export report to CSV
     */
    private void handleExportReport(HttpServletRequest request, HttpServletResponse response) throws SQLException, IOException {
        int companyId = SecuritySupport.requireCompanyId(request);
        Integer departmentId = parseNullableInt(request.getParameter("departmentId"));
        int month = parseOptionalInt(request.getParameter("month"), LocalDate.now().getMonthValue());
        int year = parseOptionalInt(request.getParameter("year"), LocalDate.now().getYear());

        List<AttendanceReportDAO> reports = getReportRowsForPrincipal(request, companyId, departmentId, month, year);
        String csv = attendanceService.exportCsvReport(reports, month, year);

        byte[] bytes = csv.getBytes(StandardCharsets.UTF_8);
        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=\"Bao_Cao_Cham_Cong_" + year + "_" + month + ".csv\"");
        response.setContentLength(bytes.length);

        try (OutputStream os = response.getOutputStream()) {
            os.write(bytes);
            os.flush();
        }
    }

    /**
     * Helper endpoint to fetch metadata (departments & employees) for dropdowns
     */
    private void handleMeta(HttpServletRequest request, HttpServletResponse response) throws SQLException, IOException {
        List<Employee> employees = employeeService.getAllEmployees(SecuritySupport.requireCompanyId(request));
        List<Department> departments = departmentService.getDepartments(SecuritySupport.requireCompanyId(request));
        TenantPrincipal principal = SecuritySupport.getTenantPrincipal(request);

        if ("self".equalsIgnoreCase(request.getParameter("scope")) || isRole(principal, "EMPLOYEE")) {
            Integer employeeId = principal.getEmployeeId();
            employees = employeeId == null
                    ? List.of()
                    : employees.stream().filter(employee -> employee.getEmployeeId() == employeeId).toList();
        } else if (isRole(principal, "MANAGER")) {
            Integer managerEmployeeId = principal.getEmployeeId();
            List<AttendanceDTO> team = managerEmployeeId == null
                    ? List.of()
                    : attendanceService.getTeamAttendance(SecuritySupport.requireCompanyId(request), managerEmployeeId, LocalDate.now());
            Set<Integer> teamEmployeeIds = team.stream()
                    .map(AttendanceDTO::getEmployeeId)
                    .collect(Collectors.toSet());
            Set<Integer> teamDepartmentIds = team.stream()
                    .map(AttendanceDTO::getDepartmentId)
                    .collect(Collectors.toSet());
            employees = employees.stream()
                    .filter(employee -> teamEmployeeIds.contains(employee.getEmployeeId()))
                    .toList();
            departments = departments.stream()
                    .filter(department -> teamDepartmentIds.contains(department.getDepartmentId()))
                    .toList();
        }

        StringBuilder json = new StringBuilder("{\"success\":true,\"employees\":[");
        for (int i = 0; i < employees.size(); i++) {
            if (i > 0) json.append(",");
            Employee e = employees.get(i);
            json.append("{\"employeeId\":").append(e.getEmployeeId())
                    .append(",\"employeeCode\":").append(quote(e.getEmployeeCode()))
                    .append(",\"fullName\":").append(quote(e.getFullName()))
                    .append(",\"status\":").append(quote(e.getStatus()))
                    .append("}");
        }
        json.append("],\"departments\":[");
        for (int i = 0; i < departments.size(); i++) {
            if (i > 0) json.append(",");
            Department d = departments.get(i);
            json.append("{\"departmentId\":").append(d.getDepartmentId())
                    .append(",\"departmentCode\":").append(quote(d.getDepartmentCode()))
                    .append(",\"departmentName\":").append(quote(d.getDepartmentName()))
                    .append("}");
        }
        json.append("]}");

        writeJson(response, HttpServletResponse.SC_OK, json.toString());
    }

    private void ensureCanManageAttendance(HttpServletRequest request, int employeeId, LocalDate date)
            throws SQLException {
        TenantPrincipal principal = SecuritySupport.getTenantPrincipal(request);
        attendanceService.requireEmployee(SecuritySupport.requireCompanyId(request), employeeId);
        if (isRole(principal, "MANAGER")) {
            Integer managerEmployeeId = principal.getEmployeeId();
            if (managerEmployeeId == null || attendanceService.getTeamAttendance(SecuritySupport.requireCompanyId(request), managerEmployeeId, date)
                    .stream().noneMatch(item -> item.getEmployeeId() == employeeId)) {
                throw new IllegalArgumentException("Bạn chỉ được quản lý chấm công của cấp dưới trực tiếp.");
            }
        }
    }

    private List<AttendanceReportDAO> getReportRowsForPrincipal(HttpServletRequest request, Integer companyId,
                                                                 Integer departmentId, int month, int year)
            throws SQLException {
        TenantPrincipal principal = SecuritySupport.getTenantPrincipal(request);
        if (isRole(principal, "MANAGER")) {
            Integer managerEmployeeId = principal.getEmployeeId();
            if (managerEmployeeId == null) {
                throw new IllegalArgumentException("Tài khoản quản lý chưa liên kết với hồ sơ nhân viên.");
            }
            return attendanceService.getManagerAttendanceReport(SecuritySupport.requireCompanyId(request), managerEmployeeId, departmentId, month, year);
        }
        return attendanceService.getAttendanceReport(companyId, departmentId, month, year);
    }

    private List<AttendanceDTO> scopedAttendance(HttpServletRequest request, LocalDate date) throws SQLException {
        TenantPrincipal principal = SecuritySupport.getTenantPrincipal(request);
        int companyId = SecuritySupport.requireCompanyId(request);
        if (isRole(principal, "MANAGER")) {
            if (principal.getEmployeeId() == null) throw new SecurityException("Tài khoản quản lý chưa liên kết với hồ sơ nhân viên.");
            return attendanceService.getTeamAttendance(companyId, principal.getEmployeeId(), date);
        }
        return attendanceService.getAllAttendanceByDate(companyId, date);
    }

    private boolean matchesPrincipalEmployee(TenantPrincipal principal, int employeeId) {
        return principal != null && principal.getEmployeeId() != null && principal.getEmployeeId() == employeeId;
    }

    private boolean isRole(TenantPrincipal principal, String roleCode) {
        return principal != null && principal.getRoleCode() != null
                && principal.getRoleCode().equalsIgnoreCase(roleCode);
    }

    private void writeJson(HttpServletResponse response, int status, String json) throws IOException {
        response.setStatus(status);
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json; charset=UTF-8");
        response.getWriter().write(json);
    }

    private String toJson(AttendanceDTO a) {
        if (a == null) return "null";
        return "{"
                + "\"attendanceId\":" + a.getAttendanceId() + ","
                + "\"companyId\":" + a.getCompanyId() + ","
                + "\"employeeId\":" + a.getEmployeeId() + ","
                + "\"employeeCode\":" + quote(a.getEmployeeCode()) + ","
                + "\"fullName\":" + quote(a.getFullName()) + ","
                + "\"departmentId\":" + a.getDepartmentId() + ","
                + "\"departmentName\":" + quote(a.getDepartmentName()) + ","
                + "\"positionTitle\":" + quote(a.getPositionTitle()) + ","
                + "\"managerId\":" + a.getManagerId() + ","
                + "\"managerName\":" + quote(a.getManagerName()) + ","
                + "\"attendanceDate\":" + (a.getAttendanceDate() != null ? quote(a.getAttendanceDate().toString()) : "null") + ","
                + "\"checkInTime\":" + (a.getCheckInTime() != null ? quote(a.getCheckInTime().format(DateTimeFormatter.ofPattern("HH:mm:ss"))) : "null") + ","
                + "\"checkOutTime\":" + (a.getCheckOutTime() != null ? quote(a.getCheckOutTime().format(DateTimeFormatter.ofPattern("HH:mm:ss"))) : "null") + ","
                + "\"totalHours\":" + (a.getTotalHours() != null ? a.getTotalHours() : "0.00") + ","
                + "\"status\":" + quote(a.getStatus()) + ","
                + "\"note\":" + quote(a.getNote()) + ","
                + "\"manualAdjusted\":" + (a.getManualAdjusted() != null && a.getManualAdjusted()) + ","
                + "\"roleCode\":" + quote(a.getRoleCode()) + ","
                + "\"roleName\":" + quote(a.getRoleName()) + ","
                + "\"employeeStatus\":" + quote(a.getEmployeeStatus())
                + "}";
    }

    private String toJson(AttendanceSummaryDAO s) {
        if (s == null) return "{}";
        return "{"
                + "\"totalWorkingDays\":" + s.getTotalWorkingDays() + ","
                + "\"totalHours\":" + (s.getTotalHours() != null ? s.getTotalHours() : "0.00") + ","
                + "\"presentDays\":" + s.getPresentDays() + ","
                + "\"lateDays\":" + s.getLateDays() + ","
                + "\"earlyLeaveDays\":" + s.getEarlyLeaveDays() + ","
                + "\"absentDays\":" + s.getAbsentDays() + ","
                + "\"onLeaveDays\":" + s.getOnLeaveDays()
                + "}";
    }

    private String toJsonArray(List<AttendanceDTO> list) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(toJson(list.get(i)));
        }
        return sb.append("]").toString();
    }

    private String toReportJsonArray(List<AttendanceReportDAO> list) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) sb.append(",");
            AttendanceReportDAO r = list.get(i);
            sb.append("{")
                    .append("\"employeeId\":").append(r.getEmployeeId()).append(",")
                    .append("\"employeeCode\":").append(quote(r.getEmployeeCode())).append(",")
                    .append("\"fullName\":").append(quote(r.getFullName())).append(",")
                    .append("\"departmentName\":").append(quote(r.getDepartmentName())).append(",")
                    .append("\"positionTitle\":").append(quote(r.getPositionTitle())).append(",")
                    .append("\"totalWorkDays\":").append(r.getTotalWorkDays()).append(",")
                    .append("\"totalHours\":").append(r.getTotalHours()).append(",")
                    .append("\"presentCount\":").append(r.getPresentCount()).append(",")
                    .append("\"lateCount\":").append(r.getLateCount()).append(",")
                    .append("\"earlyLeaveCount\":").append(r.getEarlyLeaveCount()).append(",")
                    .append("\"absentCount\":").append(r.getAbsentCount()).append(",")
                    .append("\"onLeaveCount\":").append(r.getOnLeaveCount()).append(",")
                    .append("\"punctualityRate\":").append(r.getPunctualityRate())
                    .append("}");
        }
        return sb.append("]").toString();
    }

    private String quote(String value) {
        if (value == null) return "null";
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r") + "\"";
    }

    private String error(String message) {
        return "{\"success\":false,\"message\":" + quote(message) + "}";
    }

    private int parseRequiredInt(String value, String errorMsg) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(errorMsg);
        }
        return Integer.parseInt(value.trim());
    }

    private int parseOptionalInt(String value, int defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private Integer parseNullableInt(String value) {
        if (value == null || value.isBlank() || "0".equals(value.trim())) {
            return null;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private LocalDate parseOptionalDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(value.trim());
        } catch (Exception e) {
            return null;
        }
    }

    private LocalTime parseOptionalTime(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            if (value.length() == 5) {
                return LocalTime.parse(value + ":00");
            }
            return LocalTime.parse(value.trim());
        } catch (Exception e) {
            return null;
        }
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}

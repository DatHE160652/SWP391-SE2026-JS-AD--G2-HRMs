package com.hrms.controller.manager;

import com.hrms.api.ApiRoutes;
import com.hrms.dao.AttendanceDAO.AttendanceDTO;
import com.hrms.dao.AttendanceReportDAO;
import com.hrms.dao.AttendanceSummaryDAO;
import com.hrms.model.AttendanceExplanation;
import com.hrms.model.CompanyShiftSetting;
import com.hrms.model.TimesheetLock;
import com.hrms.model.Department;
import com.hrms.model.Employee;
import com.hrms.service.AttendanceService;
import com.hrms.service.DepartmentService;
import com.hrms.service.EmployeeService;
import com.hrms.service.TimesheetExcelService;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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
        ApiRoutes.ATTENDANCE_EXPORT_TIMESHEET,
        ApiRoutes.ATTENDANCE_META,
        ApiRoutes.ATTENDANCE_SHIFT_SETTING,
        ApiRoutes.ATTENDANCE_MANUAL_ADJUST,
        ApiRoutes.MANAGER_TEAM_ATTENDANCE,
        ApiRoutes.MANAGER_EMPLOYEE_ATTENDANCE,
        ApiRoutes.MANAGER_EMPLOYEE_ATTENDANCE_HTML,
        ApiRoutes.MANAGER_COMPANY_ATTENDANCE_HTML,
        ApiRoutes.MANAGER_ATTENDANCE_REPORT_HTML
})
public class AttendanceController extends HttpServlet {
    private final AttendanceService attendanceService;
    private final EmployeeService employeeService;
    private final DepartmentService departmentService;
    private final TimesheetExcelService timesheetExcelService;

    public AttendanceController() {
        this(new AttendanceService(), new EmployeeService(), new DepartmentService(), new TimesheetExcelService());
    }

    public AttendanceController(AttendanceService attendanceService, EmployeeService employeeService, DepartmentService departmentService) {
        this(attendanceService, employeeService, departmentService, new TimesheetExcelService());
    }

    public AttendanceController(AttendanceService attendanceService, EmployeeService employeeService, DepartmentService departmentService, TimesheetExcelService timesheetExcelService) {
        this.attendanceService = attendanceService;
        this.employeeService = employeeService;
        this.departmentService = departmentService;
        this.timesheetExcelService = timesheetExcelService != null ? timesheetExcelService : new TimesheetExcelService();
    }

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String path = request.getServletPath();

        try {
            SecuritySupport.requireCompanyId(request);
            SecuritySupport.applyNoStore(response::setHeader);
            request.setAttribute("organizationCsrfToken", SecuritySupport.ensurePublicCsrfToken(request.getSession(true)));
            if (path.endsWith(".html") && path.startsWith(ApiRoutes.ATTENDANCE_PREFIX + "/")) {
                String canonical = path.substring(0, path.length() - ".html".length());
                response.sendRedirect(request.getContextPath() + canonical);
                return;
            }

            TenantPrincipal principal = SecuritySupport.getTenantPrincipal(request);
            boolean isCompanyAdmin = principal != null && principal.isCompanyAdministrator();
            if (isCompanyAdmin && (
                    path.equals(ApiRoutes.ATTENDANCE_PAGE_MANAGER)
                    || path.equals(ApiRoutes.ATTENDANCE_PAGE_MANAGER_PERSONAL)
                    || path.equals(ApiRoutes.ATTENDANCE_PAGE_EMPLOYEE)
                    || path.equals(ApiRoutes.MANAGER_TEAM_ATTENDANCE))) {
                response.sendRedirect(request.getContextPath() + ApiRoutes.ATTENDANCE_PAGE_COMPANY);
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
                    String view = request.getParameter("view");
                    if ("list".equalsIgnoreCase(view) || "timesheet".equalsIgnoreCase(view)) {
                        request.getRequestDispatcher("/WEB-INF/views/manager/attendance-list.jsp").forward(request, response);
                        return;
                    }
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
                case ApiRoutes.ATTENDANCE_REPORT -> {
                    String type = request.getParameter("type");
                    if ("timesheet".equalsIgnoreCase(type)) {
                        handleTimesheetData(request, response);
                    } else {
                        handleAttendanceReport(request, response);
                    }
                }
                case ApiRoutes.ATTENDANCE_EXPORT_REPORT -> handleExportReport(request, response);
                case ApiRoutes.ATTENDANCE_EXPORT_TIMESHEET -> handleExportTimesheet(request, response);
                case ApiRoutes.ATTENDANCE_META -> handleMeta(request, response);
                case ApiRoutes.ATTENDANCE_SHIFT_SETTING -> handleGetShiftSetting(request, response);
                case ApiRoutes.ATTENDANCE_EXPLANATION_MY -> handleMyExplanations(request, response);
                case ApiRoutes.ATTENDANCE_EXPLANATION_PENDING -> handlePendingExplanations(request, response);
                case ApiRoutes.ATTENDANCE_TIMESHEET_LOCK -> handleGetTimesheetLock(request, response);
                default -> writeJson(response, HttpServletResponse.SC_NOT_FOUND, error("Endpoint not found."));
            }
        } catch (SecurityException e) {
            writeJson(response, HttpServletResponse.SC_FORBIDDEN, error(e.getMessage()));
        } catch (SQLException e) {
            writeJson(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, error("Database error: " + e.getMessage()));
        } catch (IllegalStateException e) {
            writeJson(response, HttpServletResponse.SC_BAD_REQUEST, error(e.getMessage()));
        } catch (IllegalArgumentException e) {
            writeJson(response, HttpServletResponse.SC_BAD_REQUEST, error(e.getMessage()));
        } catch (Exception e) {
            writeJson(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, error("Server error: " + e.getMessage()));
        }
    }

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String path = request.getServletPath();

        try {
            SecuritySupport.requireCompanyId(request);
            SecuritySupport.applyNoStore(response::setHeader);
            if (!SecuritySupport.hasValidPublicCsrfToken(request)) throw new SecurityException("Phiên làm việc đã thay đổi. Vui lòng tải lại trang trước khi lưu.");

            TenantPrincipal principal = SecuritySupport.getTenantPrincipal(request);
            if (principal != null && principal.isCompanyAdministrator()) {
                switch (path) {
                    case ApiRoutes.ATTENDANCE_CHECK_IN,
                         ApiRoutes.ATTENDANCE_CHECK_OUT,
                         ApiRoutes.ATTENDANCE_BATCH_COMPLETE,
                         ApiRoutes.ATTENDANCE_MANUAL_ADJUST -> {
                        writeJson(response, HttpServletResponse.SC_FORBIDDEN,
                                error("Tài khoản Quản trị Công ty chỉ có quyền giám sát và xem báo cáo tổng hợp chấm công. Thao tác điểm danh chỉ dành cho Quản lý phòng ban."));
                        return;
                    }
                }
            }
            switch (path) {
                case ApiRoutes.ATTENDANCE_CHECK_IN -> handleCheckIn(request, response);
                case ApiRoutes.ATTENDANCE_CHECK_OUT -> handleCheckOut(request, response);
                case ApiRoutes.ATTENDANCE_BATCH_COMPLETE -> handleBatchComplete(request, response);
                case ApiRoutes.ATTENDANCE_SHIFT_SETTING -> handleSaveShiftSetting(request, response);
                case ApiRoutes.ATTENDANCE_MANUAL_ADJUST -> handleManualAdjust(request, response);
                case ApiRoutes.ATTENDANCE_EXPLANATION_SUBMIT -> handleSubmitExplanation(request, response);
                case ApiRoutes.ATTENDANCE_EXPLANATION_CANCEL -> handleCancelExplanation(request, response);
                case ApiRoutes.ATTENDANCE_EXPLANATION_REVIEW -> handleReviewExplanation(request, response);
                case ApiRoutes.ATTENDANCE_TIMESHEET_LOCK -> handleSetTimesheetLock(request, response);
                default -> writeJson(response, HttpServletResponse.SC_NOT_FOUND, error("Endpoint not found."));
            }
        } catch (SecurityException e) {
            writeJson(response, HttpServletResponse.SC_FORBIDDEN, error(e.getMessage()));
        } catch (SQLException e) {
            writeJson(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, error("Database error: " + e.getMessage()));
        } catch (IllegalStateException e) {
            writeJson(response, HttpServletResponse.SC_BAD_REQUEST, error(e.getMessage()));
        } catch (IllegalArgumentException e) {
            writeJson(response, HttpServletResponse.SC_BAD_REQUEST, error(e.getMessage()));
        } catch (Exception e) {
            writeJson(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, error("Server error: " + e.getMessage()));
        }
    }

    /**
     * Checkin: Controller
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
     * Checkout
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
     * Điểm danh hàng loạt cho nhân viên
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
     * Adjust Attendance Record: bố sung điểm danh
     */
    private void handleManualAdjust(HttpServletRequest request, HttpServletResponse response) throws SQLException, IOException {
        TenantPrincipal principal = SecuritySupport.getTenantPrincipal(request);
        if (isRole(principal, "EMPLOYEE")) {
            throw new SecurityException("Bạn không có quyền thực hiện điểm danh bổ sung.");
        }

        int companyId = SecuritySupport.requireCompanyId(request);
        int employeeId = parseRequiredInt(request.getParameter("employeeId"), "Vui lòng chọn nhân viên cần điểm danh bổ sung.");
        LocalDate date = parseOptionalDate(request.getParameter("date"));
        if (date == null) {
            throw new IllegalArgumentException("Vui lòng chọn ngày cần điểm danh bổ sung.");
        }

        ensureCanManageAttendance(request, employeeId, date);

        LocalTime checkInTime = parseOptionalTime(request.getParameter("checkInTime"));
        LocalTime checkOutTime = parseOptionalTime(request.getParameter("checkOutTime"));
        String note = trimToNull(request.getParameter("note"));
        Integer adjustedByUserId = principal != null ? principal.getUserId() : null;

        AttendanceDTO result = attendanceService.manualAdjustAttendance(
                companyId, employeeId, date, checkInTime, checkOutTime, note, adjustedByUserId
        );

        writeJson(response, HttpServletResponse.SC_OK,
                "{\"success\":true,\"message\":\"Điểm danh bổ sung thành công!\",\"data\":" + toJson(result) + "}");
    }

    /**
     * View personal attendance history (Employee read-only)
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
     * View team attendance(Manager)
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
     * View all employees attendance by date: Manager quản lý toàn bộ nhân viên công ty
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
     * Monitor & Search company-wide attendance (HR / Admin / Manager)
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
     * xuất file báo cáo điểm danh
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
     * Bảng chấm công theo tháng (Timesheet Matrix Data)
     */
    private void handleTimesheetData(HttpServletRequest request, HttpServletResponse response) throws SQLException, IOException {
        int month = parseOptionalInt(request.getParameter("month"), LocalDate.now().getMonthValue());
        int year = parseOptionalInt(request.getParameter("year"), LocalDate.now().getYear());
        if (month < 1 || month > 12) month = LocalDate.now().getMonthValue();
        if (year < 2000 || year > 2100) year = LocalDate.now().getYear();

        Integer departmentId = parseNullableInt(request.getParameter("departmentId"));
        String keyword = trimToNull(request.getParameter("keyword"));

        LocalDate firstDay = LocalDate.of(year, month, 1);
        LocalDate lastDay = firstDay.withDayOfMonth(firstDay.lengthOfMonth());
        int daysInMonth = lastDay.getDayOfMonth();

        int companyId = SecuritySupport.requireCompanyId(request);

        // Danh sách toàn bộ nhân sự công ty
        List<AttendanceDTO> allEmpAttendance = scopedAttendance(request, firstDay);
        List<AttendanceDTO> filteredEmps = allEmpAttendance;
        if (departmentId != null && departmentId > 0) {
            filteredEmps = filteredEmps.stream()
                    .filter(e -> e.getDepartmentId() != null && e.getDepartmentId().equals(departmentId))
                    .toList();
        }
        if (keyword != null && !keyword.isBlank()) {
            String kw = keyword.toLowerCase();
            filteredEmps = filteredEmps.stream()
                    .filter(e -> (e.getFullName() != null && e.getFullName().toLowerCase().contains(kw))
                            || (e.getEmployeeCode() != null && e.getEmployeeCode().toLowerCase().contains(kw)))
                    .toList();
        }

        // Lấy tất cả bản ghi chấm công trong tháng của toàn bộ nhân sự công ty
        Set<Integer> scopedEmployeeIds = filteredEmps.stream().map(AttendanceDTO::getEmployeeId).collect(Collectors.toSet());
        List<AttendanceDTO> monthRecords = attendanceService.searchCompanyAttendance(companyId, departmentId, null, firstDay, lastDay, keyword)
                .stream().filter(record -> scopedEmployeeIds.contains(record.getEmployeeId())).toList();

        // Map employeeId -> dayOfMonth -> AttendanceDTO
        java.util.Map<Integer, java.util.Map<Integer, AttendanceDTO>> empRecordsMap = new java.util.HashMap<>();
        for (AttendanceDTO rec : monthRecords) {
            if (rec.getAttendanceDate() != null) {
                empRecordsMap.computeIfAbsent(rec.getEmployeeId(), k -> new java.util.HashMap<>())
                        .put(rec.getAttendanceDate().getDayOfMonth(), rec);
            }
        }

        List<Department> departments = departmentService.getDepartments(SecuritySupport.requireCompanyId(request));
        List<AttendanceReportDAO> deptStats = getReportRowsForPrincipal(request, companyId, departmentId, month, year);

        StringBuilder sb = new StringBuilder("{");
        sb.append("\"success\":true,");
        sb.append("\"month\":").append(month).append(",");
        sb.append("\"year\":").append(year).append(",");
        sb.append("\"daysInMonth\":").append(daysInMonth).append(",");
        sb.append("\"totalEmployees\":").append(filteredEmps.size()).append(",");

        // departments
        sb.append("\"departments\":[");
        for (int i = 0; i < departments.size(); i++) {
            if (i > 0) sb.append(",");
            Department d = departments.get(i);
            sb.append("{\"departmentId\":").append(d.getDepartmentId())
                    .append(",\"departmentCode\":").append(quote(d.getDepartmentCode()))
                    .append(",\"departmentName\":").append(quote(d.getDepartmentName()))
                    .append("}");
        }
        sb.append("],");

        // departmentStats
        sb.append("\"departmentStats\":").append(toReportJsonArray(deptStats)).append(",");

        // employees
        sb.append("\"employees\":[");
        for (int i = 0; i < filteredEmps.size(); i++) {
            if (i > 0) sb.append(",");
            AttendanceDTO emp = filteredEmps.get(i);
            java.util.Map<Integer, AttendanceDTO> dayMap = empRecordsMap.getOrDefault(emp.getEmployeeId(), java.util.Collections.emptyMap());

            int presentCount = 0;
            int lateCount = 0;
            int earlyCount = 0;
            int onLeaveCount = 0;
            int absentCount = 0;

            for (AttendanceDTO dRec : dayMap.values()) {
                String st = dRec.getStatus() != null ? dRec.getStatus().toLowerCase() : "";
                if (st.contains("present") || st.contains("đúng")) presentCount++;
                else if (st.contains("late") || st.contains("muộn") || st.contains("trễ")) lateCount++;
                else if (st.contains("early") || st.contains("sớm")) earlyCount++;
                else if (st.contains("leave") || st.contains("phép")) onLeaveCount++;
                else if (st.contains("absent") || st.contains("vắng")) absentCount++;
            }

            sb.append("{")
                    .append("\"employeeId\":").append(emp.getEmployeeId()).append(",")
                    .append("\"employeeCode\":").append(quote(emp.getEmployeeCode())).append(",")
                    .append("\"fullName\":").append(quote(emp.getFullName())).append(",")
                    .append("\"departmentId\":").append(emp.getDepartmentId()).append(",")
                    .append("\"departmentName\":").append(quote(emp.getDepartmentName())).append(",")
                    .append("\"positionTitle\":").append(quote(emp.getPositionTitle())).append(",")
                    .append("\"summary\":{")
                    .append("\"present\":").append(presentCount).append(",")
                    .append("\"late\":").append(lateCount).append(",")
                    .append("\"early\":").append(earlyCount).append(",")
                    .append("\"onLeave\":").append(onLeaveCount).append(",")
                    .append("\"absent\":").append(absentCount).append(",")
                    .append("\"totalDays\":").append(presentCount + lateCount + earlyCount)
                    .append("},")
                    .append("\"records\":{");

            boolean firstDayEntry = true;
            for (int d = 1; d <= daysInMonth; d++) {
                AttendanceDTO dayRec = dayMap.get(d);
                if (dayRec != null) {
                    if (!firstDayEntry) sb.append(",");
                    firstDayEntry = false;
                    sb.append("\"").append(d).append("\":{")
                            .append("\"status\":").append(quote(dayRec.getStatus())).append(",")
                            .append("\"checkIn\":").append(dayRec.getCheckInTime() != null ? quote(dayRec.getCheckInTime().format(DateTimeFormatter.ofPattern("HH:mm"))) : "null").append(",")
                            .append("\"checkOut\":").append(dayRec.getCheckOutTime() != null ? quote(dayRec.getCheckOutTime().format(DateTimeFormatter.ofPattern("HH:mm"))) : "null").append(",")
                            .append("\"totalHours\":").append(dayRec.getTotalHours() != null ? dayRec.getTotalHours() : "0.00")
                            .append("}");
                }
            }
            sb.append("}}");
        }
        sb.append("]}");

        writeJson(response, HttpServletResponse.SC_OK, sb.toString());
    }

    /**
     * xuất báo cáo tổng kết chấm công
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
     *  Xuất bảng ma trận chấm công 1 tháng
     */
    private void handleExportTimesheet(HttpServletRequest request, HttpServletResponse response) throws SQLException, IOException {
        int month = parseOptionalInt(request.getParameter("month"), LocalDate.now().getMonthValue());
        int year = parseOptionalInt(request.getParameter("year"), LocalDate.now().getYear());
        if (month < 1 || month > 12) month = LocalDate.now().getMonthValue();
        if (year < 2000 || year > 2100) year = LocalDate.now().getYear();

        Integer departmentId = parseNullableInt(request.getParameter("departmentId"));
        String keyword = trimToNull(request.getParameter("keyword"));

        LocalDate firstDay = LocalDate.of(year, month, 1);
        LocalDate lastDay = firstDay.withDayOfMonth(firstDay.lengthOfMonth());
        int daysInMonth = lastDay.getDayOfMonth();

        int companyId = SecuritySupport.requireCompanyId(request);
        TenantPrincipal principal = SecuritySupport.getTenantPrincipal(request);
        String companyName = principal != null && principal.getCompanyName() != null ? principal.getCompanyName() : "HRMs Cloud";

        // Scoped employees
        List<AttendanceDTO> allEmpAttendance = scopedAttendance(request, firstDay);
        List<AttendanceDTO> filteredEmps = allEmpAttendance;
        if (departmentId != null && departmentId > 0) {
            filteredEmps = filteredEmps.stream()
                    .filter(e -> e.getDepartmentId() != null && e.getDepartmentId().equals(departmentId))
                    .toList();
        }
        if (keyword != null && !keyword.isBlank()) {
            String kw = keyword.toLowerCase();
            filteredEmps = filteredEmps.stream()
                    .filter(e -> (e.getFullName() != null && e.getFullName().toLowerCase().contains(kw))
                            || (e.getEmployeeCode() != null && e.getEmployeeCode().toLowerCase().contains(kw)))
                    .toList();
        }

        Set<Integer> scopedEmployeeIds = filteredEmps.stream().map(AttendanceDTO::getEmployeeId).collect(Collectors.toSet());
        List<AttendanceDTO> monthRecords = attendanceService.searchCompanyAttendance(companyId, departmentId, null, firstDay, lastDay, keyword)
                .stream().filter(record -> scopedEmployeeIds.contains(record.getEmployeeId())).toList();

        Map<Integer, Map<Integer, AttendanceDTO>> empRecordsMap = new HashMap<>();
        for (AttendanceDTO rec : monthRecords) {
            if (rec.getAttendanceDate() != null) {
                empRecordsMap.computeIfAbsent(rec.getEmployeeId(), k -> new HashMap<>())
                        .put(rec.getAttendanceDate().getDayOfMonth(), rec);
            }
        }

        List<TimesheetExcelService.TimesheetEmpData> empDataList = new ArrayList<>();
        for (AttendanceDTO emp : filteredEmps) {
            Map<Integer, AttendanceDTO> records = empRecordsMap.getOrDefault(emp.getEmployeeId(), Map.of());
            empDataList.add(new TimesheetExcelService.TimesheetEmpData(
                    emp.getEmployeeId(),
                    emp.getEmployeeCode(),
                    emp.getFullName(),
                    emp.getDepartmentName(),
                    emp.getPositionTitle(),
                    records
            ));
        }

        String filename = String.format("Bang_Cham_Cong_Thang_%02d_%d.xlsx", month, year);
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");

        timesheetExcelService.exportTimesheet(companyName, month, year, daysInMonth, empDataList, response.getOutputStream());
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
        json.append("],\"currentEmployeeId\":").append(principal.getEmployeeId() == null ? "null" : principal.getEmployeeId()).append(",\"departments\":[");
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

    /**
     * Kiểm tra quyền quản lý điểm danh:
     * Manager được toàn quyền quản lý điểm danh cho toàn bộ nhân sự công ty.
     * Employee chỉ được xem lịch sử cá nhân.
     */
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
                + "\"workCredit\":" + (a.getWorkCredit() != null ? a.getWorkCredit() : "0.0") + ","
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
                + "\"totalWorkCredits\":" + (s.getTotalWorkCredits() != null ? s.getTotalWorkCredits() : "0.0") + ","
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

    // Configure Company Shift & Working Hours: setup time
    private void handleGetShiftSetting(HttpServletRequest request, HttpServletResponse response) throws SQLException, IOException {
        int companyId = SecuritySupport.requireCompanyId(request);
        CompanyShiftSetting setting = attendanceService.getCompanyShiftSetting(companyId);
        writeJson(response, HttpServletResponse.SC_OK,
                "{\"success\":true,\"data\":" + toJson(setting) + "}");
    }

    // lưu time được setup với từng công ty
    private void handleSaveShiftSetting(HttpServletRequest request, HttpServletResponse response) throws SQLException, IOException {
        TenantPrincipal principal = SecuritySupport.getTenantPrincipal(request);
        if (isRole(principal, "EMPLOYEE")) {
            throw new SecurityException("Bạn không có quyền thay đổi cài đặt ca làm việc của công ty.");
        }

        int companyId = SecuritySupport.requireCompanyId(request);
        LocalTime startTime = parseOptionalTime(request.getParameter("shiftStartTime"));
        if (startTime == null) {
            throw new IllegalArgumentException("Vui lòng chọn giờ bắt đầu ca làm việc.");
        }
        LocalTime endTime = parseOptionalTime(request.getParameter("shiftEndTime"));
        if (endTime == null) {
            throw new IllegalArgumentException("Vui lòng chọn giờ kết thúc ca làm việc.");
        }
        if (!endTime.isAfter(startTime)) {
            throw new IllegalArgumentException("Giờ kết thúc ca làm việc phải sau giờ bắt đầu ca.");
        }

        int lunchBreakMinutes = parseOptionalInt(request.getParameter("lunchBreakMinutes"), CompanyShiftSetting.DEFAULT_LUNCH_BREAK_MINUTES);
        if (lunchBreakMinutes < 0 || lunchBreakMinutes > 240) {
            throw new IllegalArgumentException("Thời gian nghỉ trưa phải từ 0 đến 240 phút.");
        }

        int gracePeriodMinutes = parseOptionalInt(request.getParameter("gracePeriodMinutes"), CompanyShiftSetting.DEFAULT_GRACE_PERIOD_MINUTES);
        if (gracePeriodMinutes < 0 || gracePeriodMinutes > 120) {
            throw new IllegalArgumentException("Thời gian ân hạn phải từ 0 đến 120 phút.");
        }

        CompanyShiftSetting setting = new CompanyShiftSetting();
        setting.setCompanyId(companyId);
        setting.setShiftStartTime(startTime);
        setting.setShiftEndTime(endTime);
        setting.setLunchBreakMinutes(lunchBreakMinutes);
        setting.setGracePeriodMinutes(gracePeriodMinutes);
        setting.setUpdatedByUserId(principal.getUserId());

        attendanceService.updateCompanyShiftSetting(setting);

        CompanyShiftSetting updated = attendanceService.getCompanyShiftSetting(companyId);
        writeJson(response, HttpServletResponse.SC_OK,
                "{\"success\":true,\"message\":\"Cập nhật giờ giấc làm việc thành công!\",\"data\":" + toJson(updated) + "}");
    }

    private String toJson(CompanyShiftSetting s) {
        if (s == null) return "null";
        DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("HH:mm:ss");
        return "{"
                + "\"companyId\":" + s.getCompanyId() + ","
                + "\"shiftStartTime\":" + (s.getShiftStartTime() != null ? quote(s.getShiftStartTime().format(timeFmt)) : "null") + ","
                + "\"shiftEndTime\":" + (s.getShiftEndTime() != null ? quote(s.getShiftEndTime().format(timeFmt)) : "null") + ","
                + "\"lunchBreakMinutes\":" + s.getLunchBreakMinutes() + ","
                + "\"gracePeriodMinutes\":" + s.getGracePeriodMinutes() + ","
                + "\"updatedAt\":" + (s.getUpdatedAt() != null ? quote(s.getUpdatedAt().toString()) : "null") + ","
                + "\"updatedByUserId\":" + s.getUpdatedByUserId() + ","
                + "\"updatedByName\":" + quote(s.getUpdatedByName())
                + "}";
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

    // xem đơn cá nhân
     private void handleMyExplanations(HttpServletRequest request, HttpServletResponse response) throws SQLException, IOException {
        TenantPrincipal principal = SecuritySupport.getTenantPrincipal(request);
        if (principal == null || principal.getEmployeeId() == null) {
            writeJson(response, HttpServletResponse.SC_FORBIDDEN, error("Tài khoản chưa liên kết với hồ sơ nhân viên."));
            return;
        }
        int companyId = SecuritySupport.requireCompanyId(request);
        Integer month = parseNullableInt(request.getParameter("month"));
        Integer year = parseNullableInt(request.getParameter("year"));

        List<AttendanceExplanation> list = attendanceService.getMyExplanations(companyId, principal.getEmployeeId(), month, year);
        writeJson(response, HttpServletResponse.SC_OK, "{\"success\":true,\"data\":" + toJsonExplanations(list) + "}");
    }

    // Manager lấy đơn pending
    private void handlePendingExplanations(HttpServletRequest request, HttpServletResponse response) throws SQLException, IOException {
        TenantPrincipal principal = SecuritySupport.getTenantPrincipal(request);
        if (principal == null) {
            writeJson(response, HttpServletResponse.SC_FORBIDDEN, error("Vui lòng đăng nhập."));
            return;
        }
        int companyId = SecuritySupport.requireCompanyId(request);
        Integer managerEmployeeId = null;
        if (!principal.isCompanyAdministrator() && !"HR".equals(principal.getRoleCode())) {
            managerEmployeeId = principal.getEmployeeId();
        }

        List<AttendanceExplanation> list = attendanceService.getPendingExplanations(companyId, managerEmployeeId);
        writeJson(response, HttpServletResponse.SC_OK, "{\"success\":true,\"data\":" + toJsonExplanations(list) + "}");
    }

    // Submit Attendance Explanation / Punch Request
    // gửi đơn
    private void handleSubmitExplanation(HttpServletRequest request, HttpServletResponse response) throws SQLException, IOException {
        TenantPrincipal principal = SecuritySupport.getTenantPrincipal(request);
        if (principal == null || principal.getEmployeeId() == null) {
            writeJson(response, HttpServletResponse.SC_FORBIDDEN, error("Tài khoản chưa liên kết với hồ sơ nhân viên."));
            return;
        }
        int companyId = SecuritySupport.requireCompanyId(request);
        LocalDate date = parseOptionalDate(request.getParameter("attendanceDate"));
        if (date == null) {
            throw new IllegalArgumentException("Vui lòng chọn ngày cần giải trình.");
        }
        LocalTime inTime = parseOptionalTime(request.getParameter("proposedCheckIn"));
        LocalTime outTime = parseOptionalTime(request.getParameter("proposedCheckOut"));
        String reasonType = trimToNull(request.getParameter("reasonType"));
        String reasonDetail = trimToNull(request.getParameter("reasonDetail"));

        AttendanceExplanation exp = new AttendanceExplanation(companyId, principal.getEmployeeId(), date, inTime, outTime,
                reasonType != null ? reasonType : "OTHER", reasonDetail);
        attendanceService.submitExplanation(exp);
        writeJson(response, HttpServletResponse.SC_OK, "{\"success\":true,\"message\":\"Gửi đơn giải trình chấm công thành công! Vui lòng chờ quản lý phê duyệt.\"}");
    }

    // hủy đơn giải trình
    private void handleCancelExplanation(HttpServletRequest request, HttpServletResponse response) throws SQLException, IOException {
        TenantPrincipal principal = SecuritySupport.getTenantPrincipal(request);
        if (principal == null || principal.getEmployeeId() == null) {
            writeJson(response, HttpServletResponse.SC_FORBIDDEN, error("Không có quyền thực hiện."));
            return;
        }
        int explanationId = parseRequiredInt(request.getParameter("explanationId"), "Mã đơn không hợp lệ.");
        boolean cancelled = attendanceService.cancelExplanation(explanationId, principal.getEmployeeId());
        if (cancelled) {
            writeJson(response, HttpServletResponse.SC_OK, "{\"success\":true,\"message\":\"Hủy đơn giải trình thành công.\"}");
        } else {
            writeJson(response, HttpServletResponse.SC_BAD_REQUEST, error("Không thể hủy đơn (đơn đã được duyệt hoặc không tồn tại)."));
        }
    }

    // Manager phê duyệt/từ chối đơn
    private void handleReviewExplanation(HttpServletRequest request, HttpServletResponse response) throws SQLException, IOException {
        TenantPrincipal principal = SecuritySupport.getTenantPrincipal(request);
        if (principal == null) {
            writeJson(response, HttpServletResponse.SC_FORBIDDEN, error("Vui lòng đăng nhập."));
            return;
        }
        int companyId = SecuritySupport.requireCompanyId(request);
        int explanationId = parseRequiredInt(request.getParameter("explanationId"), "Mã đơn không hợp lệ.");
        String decision = request.getParameter("decision");
        String comment = request.getParameter("comment");

        boolean approve = "APPROVE".equalsIgnoreCase(decision);
        attendanceService.reviewExplanation(explanationId, principal.getUserId(), principal.getEmployeeId(), approve, comment, companyId);
        writeJson(response, HttpServletResponse.SC_OK, "{\"success\":true,\"message\":" + (approve ? "\"Đã duyệt giải trình và tự động cập nhật ngày công!\"" : "\"Đã từ chối đơn giải trình.\"") + "}");
    }

    // lock bảng điểm danh và bảng lương
    private void handleGetTimesheetLock(HttpServletRequest request, HttpServletResponse response) throws SQLException, IOException {
        int companyId = SecuritySupport.requireCompanyId(request);
        int month = parseRequiredInt(request.getParameter("month"), "Vui lòng chọn tháng.");
        int year = parseRequiredInt(request.getParameter("year"), "Vui lòng chọn năm.");

        TimesheetLock lock = attendanceService.getTimesheetLock(companyId, month, year);
        boolean isLocked = lock != null && lock.isLocked();
        writeJson(response, HttpServletResponse.SC_OK, "{\"success\":true,\"isLocked\":" + isLocked + ",\"data\":" + toJson(lock) + "}");
    }

    private void handleSetTimesheetLock(HttpServletRequest request, HttpServletResponse response) throws SQLException, IOException {
        TenantPrincipal principal = SecuritySupport.getTenantPrincipal(request);
        if (principal == null) {
            writeJson(response, HttpServletResponse.SC_FORBIDDEN, error("Vui lòng đăng nhập."));
            return;
        }
        int companyId = SecuritySupport.requireCompanyId(request);
        int month = parseRequiredInt(request.getParameter("month"), "Vui lòng chọn tháng.");
        int year = parseRequiredInt(request.getParameter("year"), "Vui lòng chọn năm.");
        String action = request.getParameter("action");
        String notes = request.getParameter("notes");
        boolean lock = "LOCK".equalsIgnoreCase(action);

        attendanceService.setTimesheetLock(companyId, month, year, lock, principal.getUserId(), notes);
        writeJson(response, HttpServletResponse.SC_OK, "{\"success\":true,\"isLocked\":" + lock + ",\"message\":" + (lock ? "\"Đã khóa bảng chấm công tháng " + month + "/" + year + "!\"" : "\"Đã mở khóa bảng chấm công tháng " + month + "/" + year + "!\"") + "}");
    }

    private String toJson(TimesheetLock l) {
        if (l == null) return "null";
        return "{"
                + "\"lockId\":" + l.getLockId() + ","
                + "\"companyId\":" + l.getCompanyId() + ","
                + "\"month\":" + l.getMonth() + ","
                + "\"year\":" + l.getYear() + ","
                + "\"isLocked\":" + l.isLocked() + ","
                + "\"lockedByUserId\":" + l.getLockedByUserId() + ","
                + "\"lockedByUserName\":" + quote(l.getLockedByUserName()) + ","
                + "\"lockedAt\":" + (l.getLockedAt() != null ? quote(l.getLockedAt().toString()) : "null") + ","
                + "\"notes\":" + quote(l.getNotes())
                + "}";
    }

    private String toJson(AttendanceExplanation e) {
        if (e == null) return "null";
        DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("HH:mm:ss");
        return "{"
                + "\"explanationId\":" + e.getExplanationId() + ","
                + "\"companyId\":" + e.getCompanyId() + ","
                + "\"employeeId\":" + e.getEmployeeId() + ","
                + "\"employeeCode\":" + quote(e.getEmployeeCode()) + ","
                + "\"employeeFullName\":" + quote(e.getEmployeeFullName()) + ","
                + "\"departmentName\":" + quote(e.getDepartmentName()) + ","
                + "\"attendanceDate\":" + (e.getAttendanceDate() != null ? quote(e.getAttendanceDate().toString()) : "null") + ","
                + "\"proposedCheckIn\":" + (e.getProposedCheckIn() != null ? quote(e.getProposedCheckIn().format(timeFmt)) : "null") + ","
                + "\"proposedCheckOut\":" + (e.getProposedCheckOut() != null ? quote(e.getProposedCheckOut().format(timeFmt)) : "null") + ","
                + "\"reasonType\":" + quote(e.getReasonType()) + ","
                + "\"reasonTypeDisplay\":" + quote(e.getReasonTypeDisplay()) + ","
                + "\"reasonDetail\":" + quote(e.getReasonDetail()) + ","
                + "\"status\":" + quote(e.getStatus()) + ","
                + "\"reviewerId\":" + (e.getReviewerId() != null ? e.getReviewerId() : "null") + ","
                + "\"reviewerComment\":" + quote(e.getReviewerComment()) + ","
                + "\"reviewerDisplayName\":" + quote(e.getReviewerDisplayName()) + ","
                + "\"reviewedAt\":" + (e.getReviewedAt() != null ? quote(e.getReviewedAt().toString()) : "null") + ","
                + "\"createdAt\":" + (e.getCreatedAt() != null ? quote(e.getCreatedAt().toString()) : "null")
                + "}";
    }

    private String toJsonExplanations(List<AttendanceExplanation> list) {
        if (list == null) return "[]";
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(toJson(list.get(i)));
        }
        sb.append("]");
        return sb.toString();
    }
}

package com.hrms.controller.hr;

import com.hrms.util.ValidationSupport;

import com.hrms.service.FieldValidationException;

import com.hrms.api.ApiRoutes;
import com.hrms.dao.LeaveReportDTO;
import com.hrms.model.LeaveBalance;
import com.hrms.model.LeaveRequest;
import com.hrms.model.LeaveType;
import com.hrms.service.LeaveService;
import com.hrms.service.security.TenantPrincipal;
import com.hrms.util.SecuritySupport;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Controller coordinating leave policies, balances, submissions, approval workflows,
 * calendars, and analytics reports.
 */
@WebServlet(name = "LeaveController", urlPatterns = {
        ApiRoutes.LEAVE_PAGE_TYPES,
        ApiRoutes.LEAVE_PAGE_MY_BALANCES,
        ApiRoutes.LEAVE_PAGE_MY_REQUESTS,
        ApiRoutes.LEAVE_PAGE_SUBMIT,
        ApiRoutes.LEAVE_PAGE_PENDING_APPROVALS,
        ApiRoutes.LEAVE_PAGE_ALL_REQUESTS,
        ApiRoutes.LEAVE_PAGE_CALENDAR,
        ApiRoutes.LEAVE_PAGE_REPORT,
        ApiRoutes.LEAVE_API_TYPES,
        ApiRoutes.LEAVE_API_MY_BALANCES,
        ApiRoutes.LEAVE_API_MY_REQUESTS,
        ApiRoutes.LEAVE_API_SUBMIT,
        ApiRoutes.LEAVE_API_CANCEL,
        ApiRoutes.LEAVE_API_PENDING_APPROVALS,
        ApiRoutes.LEAVE_API_APPROVE,
        ApiRoutes.LEAVE_API_REJECT,
        ApiRoutes.LEAVE_API_ALL_REQUESTS,
        ApiRoutes.LEAVE_API_CALENDAR_EVENTS,
        ApiRoutes.LEAVE_API_REPORT,
        ApiRoutes.LEAVE_EXPORT_EXCEL,
        ApiRoutes.LEAVE_API_CALCULATE_DAYS
})
public class LeaveController extends HttpServlet {
    private final LeaveService leaveService = new LeaveService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String path = request.getServletPath();

        try {
            switch (path) {
                // View Pages
                case ApiRoutes.LEAVE_PAGE_TYPES -> forwardView(request, response, ApiRoutes.VIEW_LEAVE_TYPE_LIST, true);
                case ApiRoutes.LEAVE_PAGE_MY_BALANCES -> forwardView(request, response, ApiRoutes.VIEW_LEAVE_MY_BALANCES, false);
                case ApiRoutes.LEAVE_PAGE_MY_REQUESTS -> forwardView(request, response, ApiRoutes.VIEW_LEAVE_MY_REQUESTS, false);
                case ApiRoutes.LEAVE_PAGE_SUBMIT -> forwardView(request, response, ApiRoutes.VIEW_LEAVE_SUBMIT, false);
                case ApiRoutes.LEAVE_PAGE_PENDING_APPROVALS -> forwardView(request, response, ApiRoutes.VIEW_LEAVE_PENDING_APPROVALS, false);
                case ApiRoutes.LEAVE_PAGE_ALL_REQUESTS -> forwardView(request, response, ApiRoutes.VIEW_LEAVE_ALL_REQUESTS, true);
                case ApiRoutes.LEAVE_PAGE_CALENDAR -> handleCalendarPage(request, response);
                case ApiRoutes.LEAVE_PAGE_REPORT -> forwardView(request, response, ApiRoutes.VIEW_LEAVE_REPORT, true);

                // API Endpoints
                case ApiRoutes.LEAVE_API_TYPES -> handleGetLeaveTypes(request, response);
                case ApiRoutes.LEAVE_API_MY_BALANCES -> handleGetMyBalances(request, response);
                case ApiRoutes.LEAVE_API_MY_REQUESTS -> handleGetMyRequests(request, response);
                case ApiRoutes.LEAVE_API_PENDING_APPROVALS -> handleGetPendingApprovals(request, response);
                case ApiRoutes.LEAVE_API_ALL_REQUESTS -> handleGetAllRequests(request, response);
                case ApiRoutes.LEAVE_API_CALENDAR_EVENTS -> handleGetCalendarEvents(request, response);
                case ApiRoutes.LEAVE_API_REPORT -> handleGetReport(request, response);
                case ApiRoutes.LEAVE_EXPORT_EXCEL -> handleExportExcel(request, response);
                case ApiRoutes.LEAVE_API_CALCULATE_DAYS -> handleCalculateWorkingDays(request, response);

                default -> writeJson(response, HttpServletResponse.SC_NOT_FOUND, error("Endpoint not found."));
            }
        } catch (SQLException e) {
            writeJson(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, error("Database error: " + e.getMessage()));
        } catch (IllegalArgumentException e) {
            writeJson(response, HttpServletResponse.SC_BAD_REQUEST, ValidationSupport.errorJson(e));
        } catch (Exception e) {
            writeJson(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, error("Server error: " + e.getMessage()));
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String path = request.getServletPath();

        try {
            switch (path) {
                case ApiRoutes.LEAVE_API_SUBMIT -> handleSubmitRequest(request, response);
                case ApiRoutes.LEAVE_API_CANCEL -> handleCancelRequest(request, response);
                case ApiRoutes.LEAVE_API_APPROVE -> handleApproveRequest(request, response);
                case ApiRoutes.LEAVE_API_REJECT -> handleRejectRequest(request, response);
                case ApiRoutes.LEAVE_API_TYPES -> handleSaveLeaveType(request, response);
                default -> writeJson(response, HttpServletResponse.SC_NOT_FOUND, error("Endpoint not found."));
            }
        } catch (SQLException e) {
            writeJson(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, error("Database error: " + e.getMessage()));
        } catch (IllegalArgumentException e) {
            writeJson(response, HttpServletResponse.SC_BAD_REQUEST, ValidationSupport.errorJson(e));
        } catch (Exception e) {
            writeJson(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, error("Server error: " + e.getMessage()));
        }
    }

    private void forwardView(HttpServletRequest request, HttpServletResponse response, String jspPath, boolean adminOnly)
            throws ServletException, IOException {
        TenantPrincipal principal = SecuritySupport.getTenantPrincipal(request);
        if (principal == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }

        if (adminOnly && !isAdminOrHr(principal)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Bạn không có quyền truy cập trang này.");
            return;
        }

        request.setAttribute("currentUser", principal);
        request.getRequestDispatcher(jspPath).forward(request, response);
    }

    private void handleCalendarPage(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        TenantPrincipal principal = SecuritySupport.getTenantPrincipal(request);
        if (principal == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }

        request.setAttribute("currentUser", principal);
        // Employees view personal calendar; Managers and Admins view team/company calendar
        if (isRole(principal, "EMPLOYEE")) {
            request.getRequestDispatcher(ApiRoutes.VIEW_LEAVE_CALENDAR_EMPLOYEE).forward(request, response);
        } else {
            request.getRequestDispatcher(ApiRoutes.VIEW_LEAVE_CALENDAR_COMPANY).forward(request, response);
        }
    }

    private void handleGetLeaveTypes(HttpServletRequest request, HttpServletResponse response)
            throws SQLException, IOException {
        TenantPrincipal principal = getRequiredPrincipal(request);
        List<LeaveType> list = leaveService.getLeaveTypeDAO().getLeaveTypesByCompany(principal.getCompanyId());
        StringBuilder sb = new StringBuilder("{\"success\":true,\"data\":[");
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) sb.append(",");
            LeaveType t = list.get(i);
            sb.append("{")
                    .append("\"leaveTypeId\":").append(t.getLeaveTypeId()).append(",")
                    .append("\"typeCode\":").append(quote(t.getTypeCode())).append(",")
                    .append("\"typeName\":").append(quote(t.getTypeName())).append(",")
                    .append("\"defaultDaysPerYear\":").append(t.getDefaultDaysPerYear()).append(",")
                    .append("\"isPaid\":").append(Boolean.TRUE.equals(t.getPaid()))
                    .append("}");
        }
        sb.append("]}");
        writeJson(response, HttpServletResponse.SC_OK, sb.toString());
    }

    private void handleSaveLeaveType(HttpServletRequest request, HttpServletResponse response)
            throws SQLException, IOException {
        TenantPrincipal principal = getRequiredPrincipal(request);
        if (!isAdminOrHr(principal)) {
            writeJson(response, HttpServletResponse.SC_FORBIDDEN, error("Chỉ quản trị viên mới có quyền cập nhật danh mục nghỉ phép."));
            return;
        }

        String action = request.getParameter("action");
        if ("delete".equalsIgnoreCase(action)) {
            int leaveTypeId = parseRequiredInt(request.getParameter("leaveTypeId"), "Thiếu ID loại nghỉ phép.");
            boolean deleted = leaveService.getLeaveTypeDAO().deleteLeaveType(leaveTypeId, principal.getCompanyId());
            if (deleted) {
                writeJson(response, HttpServletResponse.SC_OK, "{\"success\":true,\"message\":\"Đã xóa loại nghỉ phép.\"}");
            } else {
                writeJson(response, HttpServletResponse.SC_BAD_REQUEST, error("Không thể xóa loại nghỉ phép (có thể đang được sử dụng)."));
            }
            return;
        }

        String typeCode = request.getParameter("typeCode");
        String typeName = request.getParameter("typeName");
        int defaultDays = parseOptionalInt(request.getParameter("defaultDaysPerYear"), 12);
        boolean isPaid = "true".equalsIgnoreCase(request.getParameter("isPaid")) || "1".equals(request.getParameter("isPaid"));
        Integer leaveTypeId = parseNullableInt(request.getParameter("leaveTypeId"));

        if (typeName == null || typeName.isBlank()) {
            throw new FieldValidationException("typeName", "Vui lòng nhập tên loại nghỉ phép.");
        }

        if (leaveTypeId != null && leaveTypeId > 0) {
            LeaveType lt = new LeaveType();
            lt.setLeaveTypeId(leaveTypeId);
            lt.setCompanyId(principal.getCompanyId());
            lt.setTypeName(typeName.trim());
            lt.setDefaultDaysPerYear(defaultDays);
            lt.setPaid(isPaid);
            leaveService.getLeaveTypeDAO().updateLeaveType(lt);
            writeJson(response, HttpServletResponse.SC_OK, "{\"success\":true,\"message\":\"Cập nhật chính sách thành công!\"}");
        } else {
            if (typeCode == null || typeCode.isBlank()) {
                throw new FieldValidationException("typeCode", "Vui lòng nhập mã loại nghỉ phép (VD: ANNUAL, SICK).");
            }
            LeaveType existing = leaveService.getLeaveTypeDAO().getLeaveTypeByCode(principal.getCompanyId(), typeCode.trim().toUpperCase());
            if (existing != null) {
                throw new FieldValidationException("typeCode", "Mã loại nghỉ phép '" + typeCode.trim().toUpperCase() + "' đã tồn tại.");
            }
            LeaveType lt = new LeaveType();
            lt.setCompanyId(principal.getCompanyId());
            lt.setTypeCode(typeCode.trim().toUpperCase());
            lt.setTypeName(typeName.trim());
            lt.setDefaultDaysPerYear(defaultDays);
            lt.setPaid(isPaid);
            int newId = leaveService.getLeaveTypeDAO().createLeaveType(lt);
            writeJson(response, HttpServletResponse.SC_OK, "{\"success\":true,\"message\":\"Thêm loại nghỉ phép thành công!\",\"id\":" + newId + "}");
        }
    }

    private void handleGetMyBalances(HttpServletRequest request, HttpServletResponse response)
            throws SQLException, IOException {
        TenantPrincipal principal = getRequiredPrincipal(request);
        int employeeId = getRequiredEmployeeId(principal);
        int year = parseOptionalInt(request.getParameter("year"), LocalDate.now().getYear());

        List<LeaveBalance> balances = leaveService.getEmployeeBalances(employeeId, principal.getCompanyId(), year);
        StringBuilder sb = new StringBuilder("{\"success\":true,\"year\":").append(year).append(",\"data\":[");
        for (int i = 0; i < balances.size(); i++) {
            if (i > 0) sb.append(",");
            LeaveBalance b = balances.get(i);
            sb.append("{")
                    .append("\"balanceId\":").append(b.getBalanceId()).append(",")
                    .append("\"leaveTypeId\":").append(b.getLeaveTypeId()).append(",")
                    .append("\"leaveTypeCode\":").append(quote(b.getLeaveTypeCode())).append(",")
                    .append("\"leaveTypeName\":").append(quote(b.getLeaveTypeName())).append(",")
                    .append("\"isPaid\":").append(Boolean.TRUE.equals(b.getIsPaid())).append(",")
                    .append("\"defaultDaysPerYear\":").append(b.getDefaultDaysPerYear() != null ? b.getDefaultDaysPerYear() : 0).append(",")
                    .append("\"entitledDays\":").append(b.getEntitledDays() != null ? b.getEntitledDays() : 0).append(",")
                    .append("\"usedDays\":").append(b.getUsedDays() != null ? b.getUsedDays() : 0).append(",")
                    .append("\"remainingDays\":").append(b.getRemainingDays() != null ? b.getRemainingDays() : 0)
                    .append("}");
        }
        sb.append("]}");
        writeJson(response, HttpServletResponse.SC_OK, sb.toString());
    }

    private void handleGetMyRequests(HttpServletRequest request, HttpServletResponse response)
            throws SQLException, IOException {
        TenantPrincipal principal = getRequiredPrincipal(request);
        int employeeId = getRequiredEmployeeId(principal);
        String status = trimToNull(request.getParameter("status"));
        Integer year = parseNullableInt(request.getParameter("year"));
        int page = parseOptionalInt(request.getParameter("page"), 1);
        int pageSize = 20;
        int offset = (page - 1) * pageSize;

        int totalCount = leaveService.getLeaveRequestDAO().countRequestsByEmployee(employeeId, status, year);
        List<LeaveRequest> list = leaveService.getLeaveRequestDAO().getRequestsByEmployee(employeeId, status, year, offset, pageSize);

        writePagedRequestsJson(response, list, totalCount, page, pageSize);
    }

    private void handleSubmitRequest(HttpServletRequest request, HttpServletResponse response)
            throws SQLException, IOException {
        TenantPrincipal principal = getRequiredPrincipal(request);
        int employeeId = getRequiredEmployeeId(principal);
        int leaveTypeId = parseRequiredInt(request.getParameter("leaveTypeId"), "Vui lòng chọn loại nghỉ phép.");
        LocalDate startDate = parseRequiredDate(request.getParameter("startDate"), "Vui lòng chọn ngày bắt đầu.");
        LocalDate endDate = parseRequiredDate(request.getParameter("endDate"), "Vui lòng chọn ngày kết thúc.");
        String reason = request.getParameter("reason");

        LeaveRequest submitted = leaveService.submitLeaveRequest(
                principal.getCompanyId(), employeeId, leaveTypeId, startDate, endDate, reason
        );

        writeJson(response, HttpServletResponse.SC_OK,
                "{\"success\":true,\"message\":\"Nộp đơn xin nghỉ phép thành công! Đơn đang chờ duyệt.\",\"leaveRequestId\":"
                        + submitted.getLeaveRequestId() + "}");
    }

    private void handleCancelRequest(HttpServletRequest request, HttpServletResponse response)
            throws SQLException, IOException {
        TenantPrincipal principal = getRequiredPrincipal(request);
        int employeeId = getRequiredEmployeeId(principal);
        int leaveRequestId = parseRequiredInt(request.getParameter("leaveRequestId"), "Thiếu ID đơn nghỉ phép.");

        boolean cancelled = leaveService.cancelPendingRequest(leaveRequestId, employeeId);
        if (cancelled) {
            writeJson(response, HttpServletResponse.SC_OK, "{\"success\":true,\"message\":\"Hủy đơn xin nghỉ phép thành công.\"}");
        } else {
            writeJson(response, HttpServletResponse.SC_BAD_REQUEST, error("Không thể hủy đơn (có thể đơn không còn ở trạng thái Chờ duyệt)."));
        }
    }

    private void handleGetPendingApprovals(HttpServletRequest request, HttpServletResponse response)
            throws SQLException, IOException {
        TenantPrincipal principal = getRequiredPrincipal(request);
        int page = parseOptionalInt(request.getParameter("page"), 1);
        int pageSize = 20;
        int offset = (page - 1) * pageSize;

        int totalCount;
        List<LeaveRequest> list;

        if (isAdminOrHr(principal)) {
            totalCount = leaveService.getLeaveRequestDAO().countAllRequestsByCompany(
                    principal.getCompanyId(), "Pending", null, null, null, null, null
            );
            list = leaveService.getLeaveRequestDAO().getAllRequestsByCompany(
                    principal.getCompanyId(), "Pending", null, null, null, null, null, offset, pageSize
            );
        } else if (isRole(principal, "MANAGER") && principal.getEmployeeId() != null) {
            totalCount = leaveService.getLeaveRequestDAO().countPendingRequestsByManager(principal.getEmployeeId());
            list = leaveService.getLeaveRequestDAO().getPendingRequestsByManager(principal.getEmployeeId(), offset, pageSize);
        } else {
            writeJson(response, HttpServletResponse.SC_FORBIDDEN, error("Bạn không có quyền duyệt đơn nghỉ phép."));
            return;
        }

        writePagedRequestsJson(response, list, totalCount, page, pageSize);
    }

    private void handleApproveRequest(HttpServletRequest request, HttpServletResponse response)
            throws SQLException, IOException {
        TenantPrincipal principal = getRequiredPrincipal(request);
        ensureCanReview(principal);

        int leaveRequestId = parseRequiredInt(request.getParameter("leaveRequestId"), "Thiếu ID đơn nghỉ phép.");
        String comment = request.getParameter("comment");

        leaveService.approveLeaveRequest(leaveRequestId, principal.getUserId(), comment);
        writeJson(response, HttpServletResponse.SC_OK,
                "{\"success\":true,\"message\":\"Phê duyệt đơn thành công! Số dư đã được khấu trừ và đồng bộ sang chấm công.\"}");
    }

    private void handleRejectRequest(HttpServletRequest request, HttpServletResponse response)
            throws SQLException, IOException {
        TenantPrincipal principal = getRequiredPrincipal(request);
        ensureCanReview(principal);

        int leaveRequestId = parseRequiredInt(request.getParameter("leaveRequestId"), "Thiếu ID đơn nghỉ phép.");
        String comment = request.getParameter("comment");
        if (comment == null || comment.trim().isBlank()) {
            throw new FieldValidationException("rejectComment", "Vui lòng nhập lý do từ chối đơn nghỉ phép.");
        }

        leaveService.rejectLeaveRequest(leaveRequestId, principal.getUserId(), comment.trim());
        writeJson(response, HttpServletResponse.SC_OK, "{\"success\":true,\"message\":\"Đã từ chối đơn xin nghỉ phép.\"}");
    }

    private void handleGetAllRequests(HttpServletRequest request, HttpServletResponse response)
            throws SQLException, IOException {
        TenantPrincipal principal = getRequiredPrincipal(request);
        if (!isAdminOrHr(principal) && !isRole(principal, "MANAGER")) {
            writeJson(response, HttpServletResponse.SC_FORBIDDEN, error("Không có quyền truy cập danh sách đơn toàn công ty."));
            return;
        }

        String status = trimToNull(request.getParameter("status"));
        Integer departmentId = parseNullableInt(request.getParameter("departmentId"));
        Integer leaveTypeId = parseNullableInt(request.getParameter("leaveTypeId"));
        LocalDate fromDate = parseOptionalDate(request.getParameter("fromDate"));
        LocalDate toDate = parseOptionalDate(request.getParameter("toDate"));
        String keyword = trimToNull(request.getParameter("keyword"));
        int page = parseOptionalInt(request.getParameter("page"), 1);
        int pageSize = 20;
        int offset = (page - 1) * pageSize;

        int totalCount = leaveService.getLeaveRequestDAO().countAllRequestsByCompany(
                principal.getCompanyId(), status, departmentId, leaveTypeId, fromDate, toDate, keyword
        );
        List<LeaveRequest> list = leaveService.getLeaveRequestDAO().getAllRequestsByCompany(
                principal.getCompanyId(), status, departmentId, leaveTypeId, fromDate, toDate, keyword, offset, pageSize
        );

        writePagedRequestsJson(response, list, totalCount, page, pageSize);
    }

    private void handleGetCalendarEvents(HttpServletRequest request, HttpServletResponse response)
            throws SQLException, IOException {
        TenantPrincipal principal = getRequiredPrincipal(request);
        int month = parseOptionalInt(request.getParameter("month"), LocalDate.now().getMonthValue());
        int year = parseOptionalInt(request.getParameter("year"), LocalDate.now().getYear());
        if (month < 1 || month > 12) {
            month = LocalDate.now().getMonthValue();
        }
        if (year < 2000 || year > 2100) {
            year = LocalDate.now().getYear();
        }
        Integer departmentId = parseNullableInt(request.getParameter("departmentId"));

        LocalDate firstDay = LocalDate.of(year, month, 1);
        LocalDate lastDay = firstDay.plusMonths(1).minusDays(1);

        Integer filterEmployeeId = null;
        if (isRole(principal, "EMPLOYEE")) {
            filterEmployeeId = principal.getEmployeeId();
        }

        List<LeaveRequest> events = leaveService.getLeaveRequestDAO().getApprovedRequestsForCalendar(
                principal.getCompanyId(), filterEmployeeId, departmentId, firstDay, lastDay
        );

        StringBuilder sb = new StringBuilder("{\"success\":true,\"data\":[");
        for (int i = 0; i < events.size(); i++) {
            if (i > 0) sb.append(",");
            LeaveRequest r = events.get(i);
            sb.append("{")
                    .append("\"leaveRequestId\":").append(r.getLeaveRequestId()).append(",")
                    .append("\"employeeId\":").append(r.getEmployeeId()).append(",")
                    .append("\"employeeCode\":").append(quote(r.getEmployeeCode())).append(",")
                    .append("\"employeeFullName\":").append(quote(r.getEmployeeFullName())).append(",")
                    .append("\"departmentName\":").append(quote(r.getDepartmentName())).append(",")
                    .append("\"leaveTypeCode\":").append(quote(r.getLeaveTypeCode())).append(",")
                    .append("\"leaveTypeName\":").append(quote(r.getLeaveTypeName())).append(",")
                    .append("\"isPaid\":").append(Boolean.TRUE.equals(r.getIsPaid())).append(",")
                    .append("\"startDate\":").append(quote(r.getStartDate().toString())).append(",")
                    .append("\"endDate\":").append(quote(r.getEndDate().toString())).append(",")
                    .append("\"totalDays\":").append(r.getTotalDays()).append(",")
                    .append("\"reason\":").append(quote(r.getReason()))
                    .append("}");
        }
        sb.append("]}");
        writeJson(response, HttpServletResponse.SC_OK, sb.toString());
    }

    private void handleGetReport(HttpServletRequest request, HttpServletResponse response)
            throws SQLException, IOException {
        TenantPrincipal principal = getRequiredPrincipal(request);
        if (!isAdminOrHr(principal) && !isRole(principal, "MANAGER")) {
            writeJson(response, HttpServletResponse.SC_FORBIDDEN, error("Không có quyền xem báo cáo sử dụng phép."));
            return;
        }

        int year = parseOptionalInt(request.getParameter("year"), LocalDate.now().getYear());
        if (year < 2000 || year > 2100) {
            year = LocalDate.now().getYear();
        }
        Integer departmentId = parseNullableInt(request.getParameter("departmentId"));

        List<LeaveReportDTO> reports = leaveService.getLeaveRequestDAO().getLeaveUtilizationReport(
                principal.getCompanyId(), year, departmentId
        );

        StringBuilder sb = new StringBuilder("{\"success\":true,\"year\":").append(year).append(",\"data\":[");
        for (int i = 0; i < reports.size(); i++) {
            if (i > 0) sb.append(",");
            LeaveReportDTO r = reports.get(i);
            sb.append("{")
                    .append("\"employeeId\":").append(r.getEmployeeId()).append(",")
                    .append("\"employeeCode\":").append(quote(r.getEmployeeCode())).append(",")
                    .append("\"fullName\":").append(quote(r.getFullName())).append(",")
                    .append("\"departmentName\":").append(quote(r.getDepartmentName())).append(",")
                    .append("\"positionTitle\":").append(quote(r.getPositionTitle())).append(",")
                    .append("\"entitledDays\":").append(r.getEntitledDays()).append(",")
                    .append("\"usedDays\":").append(r.getUsedDays()).append(",")
                    .append("\"remainingDays\":").append(r.getRemainingDays()).append(",")
                    .append("\"paidDays\":").append(r.getPaidDays()).append(",")
                    .append("\"unpaidDays\":").append(r.getUnpaidDays()).append(",")
                    .append("\"totalRequests\":").append(r.getTotalRequests()).append(",")
                    .append("\"approvedRequests\":").append(r.getApprovedRequests()).append(",")
                    .append("\"pendingRequests\":").append(r.getPendingRequests()).append(",")
                    .append("\"rejectedRequests\":").append(r.getRejectedRequests())
                    .append("}");
        }
        sb.append("]}");
        writeJson(response, HttpServletResponse.SC_OK, sb.toString());
    }

    private void handleExportExcel(HttpServletRequest request, HttpServletResponse response)
            throws SQLException, IOException {
        TenantPrincipal principal = getRequiredPrincipal(request);
        if (!isAdminOrHr(principal) && !isRole(principal, "MANAGER")) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Không có quyền xuất báo cáo.");
            return;
        }

        int year = parseOptionalInt(request.getParameter("year"), LocalDate.now().getYear());
        if (year < 2000 || year > 2100) {
            year = LocalDate.now().getYear();
        }
        Integer departmentId = parseNullableInt(request.getParameter("departmentId"));

        List<LeaveReportDTO> reports = leaveService.getLeaveRequestDAO().getLeaveUtilizationReport(
                principal.getCompanyId(), year, departmentId
        );

        String xmlSpreadsheet = leaveService.generateExcelSpreadsheetXml(reports, year);
        byte[] bytes = xmlSpreadsheet.getBytes(StandardCharsets.UTF_8);

        response.setContentType("application/vnd.ms-excel; charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=\"Bao_Cao_Nghi_Phep_" + year + ".xls\"");
        response.setContentLength(bytes.length);

        try (OutputStream os = response.getOutputStream()) {
            os.write(bytes);
            os.flush();
        }
    }

    private void handleCalculateWorkingDays(HttpServletRequest request, HttpServletResponse response) throws IOException {
        LocalDate startDate = parseOptionalDate(request.getParameter("startDate"));
        LocalDate endDate = parseOptionalDate(request.getParameter("endDate"));

        if (startDate == null || endDate == null) {
            writeJson(response, HttpServletResponse.SC_OK, "{\"success\":true,\"workingDays\":0}");
            return;
        }

        BigDecimal days = leaveService.calculateWorkingDays(startDate, endDate);
        writeJson(response, HttpServletResponse.SC_OK, "{\"success\":true,\"workingDays\":" + days + "}");
    }

    private void writePagedRequestsJson(HttpServletResponse response, List<LeaveRequest> list, int totalCount, int page, int pageSize) throws IOException {
        StringBuilder sb = new StringBuilder("{");
        sb.append("\"success\":true,");
        sb.append("\"total\":").append(totalCount).append(",");
        sb.append("\"page\":").append(page).append(",");
        sb.append("\"pageSize\":").append(pageSize).append(",");
        sb.append("\"totalPages\":").append((int) Math.ceil((double) totalCount / pageSize)).append(",");
        sb.append("\"data\":[");
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) sb.append(",");
            LeaveRequest r = list.get(i);
            sb.append("{")
                    .append("\"leaveRequestId\":").append(r.getLeaveRequestId()).append(",")
                    .append("\"companyId\":").append(r.getCompanyId()).append(",")
                    .append("\"employeeId\":").append(r.getEmployeeId()).append(",")
                    .append("\"employeeCode\":").append(quote(r.getEmployeeCode())).append(",")
                    .append("\"employeeFullName\":").append(quote(r.getEmployeeFullName())).append(",")
                    .append("\"departmentName\":").append(quote(r.getDepartmentName())).append(",")
                    .append("\"managerName\":").append(quote(r.getManagerName())).append(",")
                    .append("\"leaveTypeId\":").append(r.getLeaveTypeId()).append(",")
                    .append("\"leaveTypeCode\":").append(quote(r.getLeaveTypeCode())).append(",")
                    .append("\"leaveTypeName\":").append(quote(r.getLeaveTypeName())).append(",")
                    .append("\"isPaid\":").append(Boolean.TRUE.equals(r.getIsPaid())).append(",")
                    .append("\"startDate\":").append(quote(r.getStartDate().toString())).append(",")
                    .append("\"endDate\":").append(quote(r.getEndDate().toString())).append(",")
                    .append("\"totalDays\":").append(r.getTotalDays()).append(",")
                    .append("\"reason\":").append(quote(r.getReason())).append(",")
                    .append("\"status\":").append(quote(r.getStatus())).append(",")
                    .append("\"approverId\":").append(r.getApproverId()).append(",")
                    .append("\"approverDisplayName\":").append(quote(r.getApproverDisplayName())).append(",")
                    .append("\"approverComment\":").append(quote(r.getApproverComment())).append(",")
                    .append("\"reviewedAt\":").append(r.getReviewedAt() != null ? quote(r.getReviewedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))) : "null").append(",")
                    .append("\"createdAt\":").append(r.getCreatedAt() != null ? quote(r.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))) : "null")
                    .append("}");
        }
        sb.append("]}");
        writeJson(response, HttpServletResponse.SC_OK, sb.toString());
    }

    private TenantPrincipal getRequiredPrincipal(HttpServletRequest request) {
        TenantPrincipal principal = SecuritySupport.getTenantPrincipal(request);
        if (principal == null) {
            throw new IllegalArgumentException("Vui lòng đăng nhập để tiếp tục.");
        }
        return principal;
    }

    private int getRequiredEmployeeId(TenantPrincipal principal) {
        if (principal.getEmployeeId() == null) {
            throw new IllegalArgumentException("Tài khoản chưa được liên kết với hồ sơ nhân viên.");
        }
        return principal.getEmployeeId();
    }

    private void ensureCanReview(TenantPrincipal principal) {
        if (!isAdminOrHr(principal) && !isRole(principal, "MANAGER")) {
            throw new IllegalArgumentException("Bạn không có quyền duyệt hoặc từ chối đơn nghỉ phép.");
        }
    }

    private boolean isAdminOrHr(TenantPrincipal principal) {
        return isRole(principal, "COMPANY_ADMIN") || isRole(principal, "HR");
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

    private LocalDate parseRequiredDate(String value, String errorMsg) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(errorMsg);
        }
        return LocalDate.parse(value.trim());
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

    private String trimToNull(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }
}

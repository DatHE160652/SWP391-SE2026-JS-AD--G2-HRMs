package com.hrms.controller.admin;

import com.hrms.api.ApiRoutes;
import com.hrms.model.RegistrationRequest;
import com.hrms.model.RegistrationRequestFilter;
import com.hrms.model.RegistrationRequestPage;
import com.hrms.model.RegistrationRequestStats;
import com.hrms.service.RegistrationRequestService;
import com.hrms.util.SecuritySupport;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;

/** Read-only System Admin pages for Registration Requests. */
public class RegistrationRequestController extends HttpServlet {
    private final RegistrationRequestService registrationRequestService;

    public RegistrationRequestController() {
        this(new RegistrationRequestService());
    }

    RegistrationRequestController(RegistrationRequestService registrationRequestService) {
        this.registrationRequestService = registrationRequestService;
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        SecuritySupport.applyNoStore(response::setHeader);
        String path = request.getServletPath();
        if (ApiRoutes.SYSTEM_ADMIN_DASHBOARD.equals(path)) {
            showDashboard(request, response);
            return;
        }
        if (ApiRoutes.REGISTRATION_REQUESTS.equals(path)) {
            showList(request, response);
            return;
        }
        if (ApiRoutes.REGISTRATION_REQUEST_DETAILS.equals(path)) {
            showDetails(request, response);
            return;
        }
        response.sendError(HttpServletResponse.SC_NOT_FOUND);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
    }

    private void showDashboard(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            setNavigation(request, "dashboard");
            request.setAttribute("registrationRequests", registrationRequestService.getRecentPending());
            request.getRequestDispatcher("/WEB-INF/views/sysadmin/system-admin-dashboard.jsp")
                    .forward(request, response);
        } catch (SQLException exception) {
            showError(request, response, "Không thể tải tổng quan quản trị.");
        }
    }

    private void showList(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            RegistrationRequestFilter filter = registrationRequestService.createFilter(
                    request.getParameter("q"), request.getParameter("status"), request.getParameter("region"),
                    request.getParameter("date"), request.getParameter("page"), request.getParameter("size"));
            RegistrationRequestPage page = registrationRequestService.getPage(filter);
            setNavigation(request, "requests");
            request.setAttribute("filter", filter);
            request.setAttribute("requestPage", page);
            request.setAttribute("registrationRequests", page.getItems());
            request.getRequestDispatcher("/WEB-INF/views/sysadmin/registration-request-list.jsp")
                    .forward(request, response);
        } catch (IllegalArgumentException exception) {
            showErrorWithStatus(request, response, HttpServletResponse.SC_BAD_REQUEST, exception.getMessage());
        } catch (SQLException exception) {
            showError(request, response, "Không thể tải danh sách Registration Request.");
        }
    }

    private void showDetails(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String idValue = request.getParameter("id");
        int requestId;
        try {
            requestId = parsePositiveId(idValue);
        } catch (IllegalArgumentException exception) {
            showErrorWithStatus(request, response, HttpServletResponse.SC_BAD_REQUEST,
                    "Mã Registration Request không hợp lệ.");
            return;
        }

        try {
            RegistrationRequest registrationRequest = registrationRequestService.getVisibleRequest(requestId);
            if (registrationRequest == null) {
                showErrorWithStatus(request, response, HttpServletResponse.SC_NOT_FOUND,
                        "Không tìm thấy Registration Request.");
                return;
            }
            setNavigation(request, "requests");
            request.setAttribute("registrationRequest", registrationRequest);
            request.getRequestDispatcher("/WEB-INF/views/sysadmin/registration-request-details.jsp")
                    .forward(request, response);
        } catch (SQLException exception) {
            showError(request, response, "Không thể tải chi tiết Registration Request.");
        }
    }

    private void setNavigation(HttpServletRequest request, String activeNav) throws SQLException {
        RegistrationRequestStats stats = registrationRequestService.getStats("dashboard".equals(activeNav));
        request.setAttribute("activeNav", activeNav);
        request.setAttribute("stats", stats);
        request.setAttribute("pendingCount", stats.getPendingCount());
    }

    private int parsePositiveId(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException();
        }
        int parsed;
        try {
            parsed = Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException();
        }
        if (parsed <= 0) {
            throw new IllegalArgumentException();
        }
        return parsed;
    }

    private void showError(HttpServletRequest request, HttpServletResponse response, String message)
            throws ServletException, IOException {
        showErrorWithStatus(request, response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, message);
    }

    private void showErrorWithStatus(HttpServletRequest request, HttpServletResponse response,
                                     int status, String message) throws ServletException, IOException {
        response.setStatus(status);
        request.setAttribute("errorMessage", message);
        request.getRequestDispatcher("/WEB-INF/views/error.jsp").forward(request, response);
    }
}

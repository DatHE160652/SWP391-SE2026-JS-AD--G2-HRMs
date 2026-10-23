package com.hrms.controller.sysadmin;

import com.hrms.api.ApiRoutes;
import com.hrms.model.CompanyAdministrationFilter;
import com.hrms.model.CompanyAdministrationView;
import com.hrms.service.CompanyAdministrationService;
import com.hrms.service.RequestActionException;
import com.hrms.service.RegistrationRequestService;
import com.hrms.service.security.SystemAdminPrincipal;
import com.hrms.util.SecuritySupport;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

/** System Admin Company pages and guarded Subscription suspend/activate actions. */
public class CompanyAdministrationController extends HttpServlet {
    private static final String FLASH_MESSAGE = "companyActionMessage";
    private final CompanyAdministrationService companyService;
    private final RegistrationRequestService registrationRequestService;

    public CompanyAdministrationController() {
        this(new CompanyAdministrationService(), new RegistrationRequestService());
    }

    CompanyAdministrationController(CompanyAdministrationService companyService,
                                    RegistrationRequestService registrationRequestService) {
        this.companyService = companyService;
        this.registrationRequestService = registrationRequestService;
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        SecuritySupport.applyNoStore(response::setHeader);
        String path = request.getServletPath();
        if (ApiRoutes.SYSTEM_ADMIN_COMPANIES.equals(path)) {
            showList(request, response);
            return;
        }
        if (ApiRoutes.SYSTEM_ADMIN_COMPANY_DETAILS.equals(path)) {
            showDetails(request, response);
            return;
        }
        if (ApiRoutes.SYSTEM_ADMIN_COMPANY_SUSPEND.equals(path)
                || ApiRoutes.SYSTEM_ADMIN_COMPANY_ACTIVATE.equals(path)) {
            response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
            return;
        }
        response.sendError(HttpServletResponse.SC_NOT_FOUND);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        SecuritySupport.applyNoStore(response::setHeader);
        String path = request.getServletPath();
        if (!ApiRoutes.SYSTEM_ADMIN_COMPANY_SUSPEND.equals(path)
                && !ApiRoutes.SYSTEM_ADMIN_COMPANY_ACTIVATE.equals(path)) {
            response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
            return;
        }
        if (!SecuritySupport.isSystemAdmin(request) || !SecuritySupport.hasValidCsrfToken(request)
                || SecuritySupport.getPrincipal(request).getUserId() == null
                || SecuritySupport.getPrincipal(request).getUserId() <= 0) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Phiên thao tác không hợp lệ.");
            return;
        }
        int companyId;
        try {
            companyId = parsePositiveId(request.getParameter("id"));
        } catch (IllegalArgumentException exception) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Mã Company không hợp lệ.");
            return;
        }

        SystemAdminPrincipal principal = SecuritySupport.getPrincipal(request);
        try {
            if (ApiRoutes.SYSTEM_ADMIN_COMPANY_SUSPEND.equals(path)) {
                companyService.suspend(companyId, principal.getUserId(), request.getParameter("reason"),
                        request.getRemoteAddr());
                request.getSession(true).setAttribute(FLASH_MESSAGE,
                        "Đã tạm ngưng Subscription. Workspace đã bị chặn cho mọi phiên tenant.");
            } else {
                companyService.activate(companyId, principal.getUserId(), request.getRemoteAddr());
                request.getSession(true).setAttribute(FLASH_MESSAGE,
                        "Đã kích hoạt lại Subscription. Workspace có thể đăng nhập nếu Company và tài khoản vẫn hợp lệ.");
            }
            response.sendRedirect(request.getContextPath() + ApiRoutes.SYSTEM_ADMIN_COMPANY_DETAILS + "?id=" + companyId);
        } catch (RequestActionException exception) {
            showErrorWithStatus(request, response, statusFor(exception), exception.getMessage());
        } catch (IllegalArgumentException exception) {
            showErrorWithStatus(request, response, HttpServletResponse.SC_BAD_REQUEST, exception.getMessage());
        } catch (SQLException exception) {
            getServletContext().log("System Admin subscription mutation failed; SQLState="
                    + exception.getSQLState() + "; errorCode=" + exception.getErrorCode());
            showError(request, response, "Không thể lưu thao tác Subscription. Vui lòng thử lại sau.");
        }
    }

    private void showList(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            CompanyAdministrationFilter filter = companyService.createFilter(
                    request.getParameter("status"), request.getParameter("region"), request.getParameter("sort"));
            setNavigation(request);
            request.setAttribute("allCompanies", companyService.getCompanies());
            request.setAttribute("companies", companyService.getCompanies(filter));
            request.setAttribute("filter", filter);
            request.getRequestDispatcher("/WEB-INF/views/sysadmin/company-list.jsp").forward(request, response);
        } catch (IllegalArgumentException exception) {
            showErrorWithStatus(request, response, HttpServletResponse.SC_BAD_REQUEST, exception.getMessage());
        } catch (SQLException exception) {
            showError(request, response, "Không thể tải danh sách Company.");
        }
    }

    private void showDetails(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        int companyId;
        try {
            companyId = parsePositiveId(request.getParameter("id"));
        } catch (IllegalArgumentException exception) {
            showErrorWithStatus(request, response, HttpServletResponse.SC_BAD_REQUEST, "Mã Company không hợp lệ.");
            return;
        }
        try {
            CompanyAdministrationView company = companyService.getCompany(companyId);
            if (company == null) {
                showErrorWithStatus(request, response, HttpServletResponse.SC_NOT_FOUND, "Không tìm thấy Company.");
                return;
            }
            setNavigation(request);
            consumeFlash(request);
            request.setAttribute("company", company);
            request.getRequestDispatcher("/WEB-INF/views/sysadmin/company-details.jsp").forward(request, response);
        } catch (SQLException exception) {
            showError(request, response, "Không thể tải chi tiết Company.");
        }
    }

    private void setNavigation(HttpServletRequest request) throws SQLException {
        SecuritySupport.ensureCsrfToken(request.getSession(true));
        request.setAttribute("activeNav", "companies");
        request.setAttribute("pendingCount", registrationRequestService.getStats(false).getPendingCount());
    }

    private void consumeFlash(HttpServletRequest request) {
        if (request.getSession(false) == null) return;
        Object message = request.getSession(false).getAttribute(FLASH_MESSAGE);
        request.getSession(false).removeAttribute(FLASH_MESSAGE);
        if (message != null) request.setAttribute("actionMessage", message);
    }

    private int parsePositiveId(String value) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException();
        try {
            int id = Integer.parseInt(value);
            if (id <= 0) throw new IllegalArgumentException();
            return id;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException();
        }
    }

    private int statusFor(RequestActionException exception) {
        return switch (exception.getReason()) {
            case FORBIDDEN -> HttpServletResponse.SC_FORBIDDEN;
            case NOT_FOUND -> HttpServletResponse.SC_NOT_FOUND;
            case CONFLICT -> HttpServletResponse.SC_CONFLICT;
        };
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

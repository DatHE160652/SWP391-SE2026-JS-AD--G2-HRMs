package com.hrms.controller.auth;

import com.hrms.api.ApiRoutes;
import com.hrms.api.SystemAdminPrincipal;
import com.hrms.service.SystemAdminAuthService;
import com.hrms.util.SecuritySupport;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.sql.SQLException;

/** Handles the platform System Admin login screen. */
public class SystemAdminLoginController extends HttpServlet {
    private final SystemAdminAuthService authService;

    public SystemAdminLoginController() {
        this(new SystemAdminAuthService());
    }

    SystemAdminLoginController(SystemAdminAuthService authService) {
        this.authService = authService;
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        SecuritySupport.applyNoStore(response::setHeader);
        if (SecuritySupport.isSystemAdmin(request)) {
            response.sendRedirect(request.getContextPath() + ApiRoutes.SYSTEM_ADMIN_DASHBOARD);
            return;
        }
        SecuritySupport.ensureCsrfToken(request.getSession(true));
        request.getRequestDispatcher("/WEB-INF/views/auth/system-admin-login.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        SecuritySupport.applyNoStore(response::setHeader);
        if (!SecuritySupport.hasValidCsrfToken(request)) {
            showError(request, response, HttpServletResponse.SC_FORBIDDEN,
                    "Phiên đăng nhập không hợp lệ. Vui lòng tải lại trang và thử lại.");
            return;
        }

        String identifier = trimToNull(request.getParameter("identifier"));
        String password = request.getParameter("password");
        if (identifier == null || password == null || password.isEmpty()) {
            showError(request, response, HttpServletResponse.SC_BAD_REQUEST,
                    "Vui lòng nhập tên đăng nhập/email và mật khẩu.");
            return;
        }

        try {
            SystemAdminPrincipal principal = authService.authenticate(identifier, password);
            if (principal == null) {
                showError(request, response, HttpServletResponse.SC_UNAUTHORIZED,
                        "Thông tin đăng nhập không hợp lệ.");
                return;
            }

            request.changeSessionId();
            HttpSession session = request.getSession(false);
            session.setAttribute(SecuritySupport.PRINCIPAL_ATTRIBUTE, principal);
            SecuritySupport.ensureCsrfToken(session);
            response.sendRedirect(request.getContextPath() + ApiRoutes.SYSTEM_ADMIN_DASHBOARD);
        } catch (SQLException exception) {
            String diagnostic = "Database connection or authentication query failed";
            if ("Missing database configuration: HRM_DB_USER".equals(exception.getMessage())) {
                diagnostic = "Missing HRM_DB_USER (or hrm.db.user) in the Tomcat process";
            } else if ("Missing database configuration: HRM_DB_PASSWORD".equals(exception.getMessage())) {
                diagnostic = "Missing HRM_DB_PASSWORD (or hrm.db.password) in the Tomcat process";
            } else if ("SQL Server JDBC driver was not found.".equals(exception.getMessage())) {
                diagnostic = "SQL Server JDBC driver is missing from the runtime classpath";
            }
            // Log diagnostic codes, never credentials, JDBC URLs or raw driver messages.
            getServletContext().log("System Admin login: " + diagnostic
                    + "; SQLState=" + exception.getSQLState()
                    + "; errorCode=" + exception.getErrorCode());
            showError(request, response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Không thể kết nối dịch vụ xác thực. Vui lòng thử lại sau.");
        } finally {
            password = null;
        }
    }

    private void showError(HttpServletRequest request, HttpServletResponse response,
                           int status, String message) throws ServletException, IOException {
        response.setStatus(status);
        SecuritySupport.ensureCsrfToken(request.getSession(true));
        request.setAttribute("errorMessage", message);
        request.getRequestDispatcher("/WEB-INF/views/auth/system-admin-login.jsp").forward(request, response);
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}

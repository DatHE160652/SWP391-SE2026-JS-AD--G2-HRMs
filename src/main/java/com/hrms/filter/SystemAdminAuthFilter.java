package com.hrms.filter;

import com.hrms.api.ApiRoutes;
import com.hrms.service.security.SystemAdminPrincipal;
import com.hrms.service.SystemAdminAuthService;
import com.hrms.util.SecuritySupport;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;

/** Protects System Admin pages; login and CSRF-protected logout remain reachable. */
public class SystemAdminAuthFilter implements Filter {
    private final SystemAdminAuthService authService;

    public SystemAdminAuthFilter() {
        this(new SystemAdminAuthService());
    }

    SystemAdminAuthFilter(SystemAdminAuthService authService) {
        this.authService = authService;
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse,
                         FilterChain chain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;
        SecuritySupport.applyNoStore(response::setHeader);

        String path = request.getServletPath();
        if (ApiRoutes.SYSTEM_ADMIN_LOGIN.equals(path) || ApiRoutes.SYSTEM_ADMIN_LOGOUT.equals(path)) {
            chain.doFilter(request, response);
            return;
        }

        SystemAdminPrincipal principal = SecuritySupport.getPrincipal(request);
        try {
            if (!authService.isAuthorized(principal)) {
                redirectToLogin(request, response);
                return;
            }
        } catch (SQLException exception) {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Không thể xác thực phiên quản trị.");
            return;
        }
        chain.doFilter(request, response);
    }

    private void redirectToLogin(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (request.getSession(false) != null) {
            request.getSession(false).invalidate();
        }
        response.sendRedirect(request.getContextPath() + ApiRoutes.SYSTEM_ADMIN_LOGIN);
    }
}

package com.hrms.filter;

import com.hrms.service.security.TenantPrincipal;
import com.hrms.util.SecuritySupport;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Set;

public class TenantAuthorizationFilter implements Filter {
    private static final Set<String> ADMIN_ROLES = Set.of("COMPANY_ADMIN");
    private static final Set<String> TENANT_ROLES = Set.of("COMPANY_ADMIN", "HR", "MANAGER", "EMPLOYEE");

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        if (!(request instanceof HttpServletRequest httpRequest)
                || !(response instanceof HttpServletResponse httpResponse)) {
            chain.doFilter(request, response);
            return;
        }

        String path = httpRequest.getServletPath();
        if (!isTenantPath(path)) {
            chain.doFilter(request, response);
            return;
        }

        TenantPrincipal principal = SecuritySupport.getTenantPrincipal(httpRequest);
        if (principal == null || !TENANT_ROLES.contains(role(principal))) {
            rejectUnauthenticated(httpRequest, httpResponse);
            return;
        }

        if (principal.isMustChangePassword()) {
            rejectPasswordChangeRequired(httpRequest, httpResponse);
            return;
        }

        if (!isAllowed(httpRequest, principal)) {
            rejectForbidden(httpRequest, httpResponse);
            return;
        }

        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {
    }

    private boolean isAllowed(HttpServletRequest request, TenantPrincipal principal) {
        String role = role(principal);
        if (ADMIN_ROLES.contains(role)) {
            return true;
        }

        String path = request.getServletPath();
        String method = request.getMethod();

        if ("HR".equals(role)) {
            return isGet(method) && (path.equals("/employee/profile-update-periods")
                    || path.equals("/employees/profile-update-periods"))
                    || isPost(method) && (path.equals("/employees/profile-update-periods/create")
                    || path.equals("/employees/profile-update-periods/close"));
        }

        if ("MANAGER".equals(role)) {
            return isManagerAllowed(path, method);
        }

        if ("EMPLOYEE".equals(role)) {
            return isEmployeeAllowed(request, principal, path, method);
        }

        return false;
    }

    private boolean isManagerAllowed(String path, String method) {
        if (isGet(method) && (
                path.equals("/employee/my-profile")
                        || path.equals("/employee/my-profile.html")
                        || path.equals("/employees/me")
                        || path.equals("/employees/me/history")
                        || path.equals("/documents/me")
                        || path.equals("/documents/me/download")
                        || path.equals("/attendance/manager-attendance")
                        || path.equals("/attendance/manager-attendance.html")
                        || path.equals("/attendance/manager-personal-attendance")
                        || path.equals("/attendance/manager-personal-attendance.html")
                        || path.equals("/attendance/employee-attendance")
                        || path.equals("/attendance/employee-attendance.html")
                        || path.equals("/attendance/attendance-report")
                        || path.equals("/attendance/attendance-report.html")
                        || path.equals("/manager/team-attendance")
                        || path.equals("/manager/employee-attendance")
                        || path.equals("/manager/employee-attendance.html")
                        || path.equals("/manager/attendance-report.html")
                        || path.equals("/attendance/all")
                        || path.equals("/attendance/team")
                        || path.equals("/attendance/report")
                        || path.equals("/attendance/export-report")
                        || path.equals("/attendance/meta"))) {
            return true;
        }

        if (isPost(method) && (
                path.equals("/employees/me/update-profile")
                        || path.equals("/documents/me/upload")
                        || path.equals("/documents/me/delete"))) {
            return true;
        }

        if (isGet(method) && path.equals("/attendance/personal")) {
            return true;
        }

        return isPost(method) && (
                path.equals("/attendance/check-in")
                        || path.equals("/attendance/check-out")
                        || path.equals("/attendance/batch-complete"));
    }

    private boolean isEmployeeAllowed(HttpServletRequest request, TenantPrincipal principal,
                                      String path, String method) {
        if (isGet(method) && (
                path.equals("/employee/my-profile")
                        || path.equals("/employee/my-profile.html")
                        || path.equals("/employees/me")
                        || path.equals("/employees/me/history")
                        || path.equals("/documents/me")
                        || path.equals("/documents/me/download")
                        || path.equals("/attendance/employee-attendance")
                        || path.equals("/attendance/employee-attendance.html")
                        || path.equals("/attendance/meta"))) {
            return true;
        }

        if (isPost(method) && path.equals("/employees/me/update-profile")) {
            return true;
        }

        if (isPost(method) && (
                path.equals("/documents/me/upload")
                        || path.equals("/documents/me/delete"))) {
            return true;
        }

        if (isGet(method) && path.equals("/attendance/personal")) {
            Integer employeeId = principal.getEmployeeId();
            String requested = request.getParameter("employeeId");
            return employeeId != null && requested != null && requested.equals(employeeId.toString());
        }

        return false;
    }

    private boolean isTenantPath(String path) {
        return path.startsWith("/employee/")
                || path.startsWith("/department/")
                || path.startsWith("/position/")
                || path.startsWith("/document/")
                || path.startsWith("/attendance/")
                || path.startsWith("/manager/")
                || path.equals("/employees")
                || path.startsWith("/employees/")
                || path.equals("/departments")
                || path.startsWith("/departments/")
                || path.equals("/positions")
                || path.startsWith("/positions/")
                || path.equals("/documents")
                || path.startsWith("/documents/");
    }

    private void rejectUnauthenticated(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (expectsJson(request)) {
            writeJson(response, HttpServletResponse.SC_UNAUTHORIZED,
                    "{\"success\":false,\"message\":\"Please sign in to continue.\"}");
            return;
        }
        response.sendRedirect(request.getContextPath() + "/login");
    }

    private void rejectForbidden(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (expectsJson(request)) {
            writeJson(response, HttpServletResponse.SC_FORBIDDEN,
                    "{\"success\":false,\"message\":\"You do not have permission to access this resource.\"}");
            return;
        }
        response.sendError(HttpServletResponse.SC_FORBIDDEN, "You do not have permission to access this page.");
    }

    private void rejectPasswordChangeRequired(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (expectsJson(request)) {
            writeJson(response, HttpServletResponse.SC_FORBIDDEN,
                    "{\"success\":false,\"message\":\"Please change your temporary password before continuing.\"}");
            return;
        }
        response.sendRedirect(request.getContextPath() + "/change-password");
    }

    private void writeJson(HttpServletResponse response, int status, String json) throws IOException {
        response.setStatus(status);
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json; charset=UTF-8");
        response.getWriter().write(json);
    }

    private boolean expectsJson(HttpServletRequest request) {
        String accept = request.getHeader("Accept");
        String path = request.getServletPath();
        return accept != null && accept.contains("application/json")
                || path.equals("/employees")
                || path.startsWith("/employees/")
                || path.equals("/departments")
                || path.startsWith("/departments/")
                || path.equals("/positions")
                || path.startsWith("/positions/")
                || path.equals("/documents")
                || path.startsWith("/documents/")
                || path.startsWith("/attendance/") && !isAttendancePagePath(path) && !path.endsWith(".html");
    }

    private boolean isGet(String method) {
        return "GET".equalsIgnoreCase(method);
    }

    private boolean isPost(String method) {
        return "POST".equalsIgnoreCase(method);
    }

    private boolean isAttendancePagePath(String path) {
        return path.equals("/attendance/manager-attendance")
                || path.equals("/attendance/employee-attendance")
                || path.equals("/attendance/company-attendance")
                || path.equals("/attendance/manager-personal-attendance")
                || path.equals("/attendance/attendance-report");
    }

    private String role(TenantPrincipal principal) {
        return principal.getRoleCode() == null ? "" : principal.getRoleCode().toUpperCase();
    }
}

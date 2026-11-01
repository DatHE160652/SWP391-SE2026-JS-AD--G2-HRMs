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
    private final SessionValidator sessionValidator;

    public TenantAuthorizationFilter() {
        this(principal -> new com.hrms.dao.UserDAO().isCurrentTenantSessionAuthorized(principal));
    }

    public TenantAuthorizationFilter(SessionValidator sessionValidator) {
        this.sessionValidator = sessionValidator;
    }

    @FunctionalInterface
    public interface SessionValidator {
        boolean valid(TenantPrincipal principal) throws java.sql.SQLException;
    }
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

        String path = fullPath(httpRequest);
        if (path.startsWith("/images/employees/") || path.startsWith("/uploads/employees/")) {
            httpResponse.sendError(404);
            return;
        }
        if (!isTenantPath(path)) {
            chain.doFilter(request, response);
            return;
        }

        SecuritySupport.applyNoStore(httpResponse::setHeader);
        TenantPrincipal principal = SecuritySupport.getTenantPrincipal(httpRequest);
        if (principal == null || principal.getCompanyId() == null || principal.getCompanyId() <= 0
                || !TENANT_ROLES.contains(role(principal))) {
            rejectUnauthenticated(httpRequest, httpResponse);
            return;
        }

        if (principal.isMustChangePassword() && !path.equals("/change-password")) {
            rejectPasswordChangeRequired(httpRequest, httpResponse);
            return;
        }

        if (!isAllowed(httpRequest, principal)) {
            rejectForbidden(httpRequest, httpResponse);
            return;
        }

        try {
            if (!sessionValidator.valid(principal)) {
                httpRequest.getSession(false).invalidate();
                rejectUnauthenticated(httpRequest, httpResponse);
                return;
            }
        } catch (java.sql.SQLException exception) {
            httpResponse.sendError(503, "Không xác minh được quyền truy cập tài khoản.");
            return;
        }

        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {
    }

    private boolean isAllowed(HttpServletRequest request, TenantPrincipal principal) {
        if (request.getServletPath().equals("/change-password") || request.getServletPath().startsWith("/leave/")) return true;
        String role = role(principal);
        String payrollPath = fullPath(request);
        if (payrollPath.equals("/payroll") || payrollPath.startsWith("/payroll/")) {
            return principal.canManagePayroll();
        }
        if (payrollPath.equals("/payslips") || payrollPath.startsWith("/payslips/")) {
            return principal.getEmployeeId() != null && (isGet(request.getMethod())
                    || isPost(request.getMethod()) && payrollPath.equals("/payslips/api/read"));
        }
        String path = request.getServletPath();
        String method = request.getMethod();

        if (principal.isCompanyAdministrator()) {
            if (isPost(method) && (
                    path.equals("/attendance/check-in")
                    || path.equals("/attendance/check-out")
                    || path.equals("/attendance/batch-complete")
                    || path.equals("/attendance/manual-adjust"))) {
                return false;
            }
            return true;
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
        if (isGet(method) && Set.of("/employee/organization","/employee/my-contracts","/employees/me/organization-chart","/employees/me/colleague","/employees/me/contracts").contains(path)) return true;
        if (isGet(method) && path.equals("/employees/me/team-member")) return true;
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
                        || path.equals("/attendance/export-timesheet")
                        || path.equals("/attendance/meta")
                        || path.equals("/attendance/shift-setting"))) {
            return true;
        }

        if (isPost(method) && (
                path.equals("/employees/me/update-profile")
                        || path.equals("/documents/me/upload")
                        || (path.equals("/documents/me/deactivate") || path.equals("/documents/me/activate")))) {
            return true;
        }

        if (isGet(method) && path.equals("/attendance/personal")) {
            return true;
        }

        return isPost(method) && (
                path.equals("/attendance/check-in")
                        || path.equals("/attendance/check-out")
                        || path.equals("/attendance/batch-complete")
                        || path.equals("/attendance/shift-setting")
                        || path.equals("/attendance/manual-adjust"));
    }

    private boolean isEmployeeAllowed(HttpServletRequest request, TenantPrincipal principal,
                                      String path, String method) {
        if (isGet(method) && Set.of("/employee/organization","/employee/my-contracts","/employees/me/organization-chart","/employees/me/colleague","/employees/me/contracts").contains(path)) return true;
        if (isGet(method) && (
                path.equals("/employee/my-profile")
                        || path.equals("/employee/my-profile.html")
                        || path.equals("/employees/me")
                        || path.equals("/employees/me/history")
                        || path.equals("/documents/me")
                        || path.equals("/documents/me/download")
                        || path.equals("/attendance/employee-attendance")
                        || path.equals("/attendance/employee-attendance.html")
                        || path.equals("/attendance/meta")
                        || path.equals("/attendance/shift-setting"))) {
            return true;
        }

        if (isPost(method) && path.equals("/employees/me/update-profile")) {
            return true;
        }

        if (isPost(method) && (
                path.equals("/documents/me/upload")
                        || (path.equals("/documents/me/deactivate") || path.equals("/documents/me/activate")))) {
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
        if (path.equals("/change-password") || path.startsWith("/leave/")) return true;
        if (path.startsWith("/lifecycle/")) return true;
        if (path.startsWith("/responsibility-level/") || path.equals("/responsibility-levels") || path.startsWith("/responsibility-levels/")) return true;
        return path.equals("/payroll") || path.startsWith("/payroll/")
                || path.equals("/payslips") || path.startsWith("/payslips/")
                || path.startsWith("/employee/")
                || path.startsWith("/job-title/")
                || path.equals("/job-titles")
                || path.startsWith("/job-titles/")
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
        response.sendError(HttpServletResponse.SC_FORBIDDEN, "Bạn không có quyền truy cập trang này.");
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
        if (request.getServletPath().startsWith("/lifecycle/api/") || request.getServletPath().startsWith("/leave/api/")) return true;
        if (request.getServletPath().equals("/responsibility-levels") || request.getServletPath().startsWith("/responsibility-levels/")) return true;
        String accept = request.getHeader("Accept");
        String path = fullPath(request);
        return accept != null && accept.contains("application/json")
                || path.startsWith("/payroll/api/") || path.startsWith("/payslips/api/")
                || path.equals("/job-titles")
                || path.startsWith("/job-titles/")
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

    private String fullPath(HttpServletRequest request) {
        return request.getServletPath() + (request.getPathInfo() == null ? "" : request.getPathInfo());
    }
}

package com.hrms.controller.sysadmin;

import com.hrms.dao.sysadmin.CompanyAdministrationDAO;
import com.hrms.dao.sysadmin.PlatformAuditDAO;
import com.hrms.dao.sysadmin.RegistrationRequestDAO;
import com.hrms.service.security.SystemAdminPrincipal;
import com.hrms.util.SecuritySupport;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;

/** System Admin view for platform AuditLogs and EmailAttempts. */
public class PlatformAuditController extends HttpServlet {
    private static final String FLASH_MESSAGE = "auditActionMessage";
    private static final String FLASH_ERROR = "auditActionError";
    private final PlatformAuditDAO auditDAO = new PlatformAuditDAO();
    private final RegistrationRequestDAO requestDAO = new RegistrationRequestDAO();
    private final CompanyAdministrationDAO companyDAO = new CompanyAdministrationDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        SecuritySupport.applyNoStore(response::setHeader);
        String tab = "email".equalsIgnoreCase(request.getParameter("tab")) ? "email" : "audit";
        String actionFilter = request.getParameter("actionFilter");
        String statusFilter = request.getParameter("statusFilter");
        String q = request.getParameter("q");

        try {
            request.setAttribute("tab", tab);
            request.setAttribute("actionFilter", actionFilter == null ? "" : actionFilter);
            request.setAttribute("statusFilter", statusFilter == null ? "" : statusFilter);
            request.setAttribute("q", q == null ? "" : q);
            request.setAttribute("auditLogs", auditDAO.findPlatformAuditLogs(actionFilter, q, 100));
            request.setAttribute("emailAttempts", auditDAO.findEmailAttempts(statusFilter, q, 100));
            request.setAttribute("activeNav", "audit");
            request.setAttribute("pendingCount", requestDAO.getStats(false).getPendingCount());
            moveFlash(request, FLASH_MESSAGE, "actionMessage");
            moveFlash(request, FLASH_ERROR, "actionError");
            SecuritySupport.ensureCsrfToken(request.getSession(true));
            request.getRequestDispatcher("/WEB-INF/views/sysadmin/audit-logs.jsp").forward(request, response);
        } catch (SQLException ex) {
            getServletContext().log("Failed to load platform audit logs", ex);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Không tải được nhật ký hệ thống.");
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        SecuritySupport.applyNoStore(response::setHeader);
        if (!"/systemadmin/audit-logs/rollback-renew".equals(request.getServletPath())) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        if (!SecuritySupport.isSystemAdmin(request) || !SecuritySupport.hasValidCsrfToken(request)
                || SecuritySupport.getPrincipal(request).getUserId() == null) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Phiên thao tác không hợp lệ.");
            return;
        }
        SystemAdminPrincipal principal = SecuritySupport.getPrincipal(request);
        try {
            long logId = Long.parseLong(request.getParameter("logId").trim());
            CompanyAdministrationDAO.MutationResult result = companyDAO.rollbackRenew(
                    logId, principal.getUserId(), request.getRemoteAddr());
            if ("CHANGED".equals(result.outcome())) {
                request.getSession(true).setAttribute(FLASH_MESSAGE,
                        "Đã khôi phục lại thời hạn Subscription trước khi gia hạn (Nhật ký #" + logId + ").");
            } else if ("FORBIDDEN".equals(result.outcome())) {
                request.getSession(true).setAttribute(FLASH_ERROR,
                        "Chỉ tài khoản đã thực hiện thao tác gia hạn này mới có quyền khôi phục.");
            } else {
                request.getSession(true).setAttribute(FLASH_ERROR,
                        "Không thể khôi phục bản ghi này (đã được khôi phục trước đó hoặc không hợp lệ).");
            }
        } catch (Exception ex) {
            request.getSession(true).setAttribute(FLASH_ERROR, "Không thể khôi phục lại. Vui lòng thử lại.");
        }
        response.sendRedirect(request.getContextPath() + "/systemadmin/audit-logs?tab=audit");
    }

    private static void moveFlash(HttpServletRequest request, String sessionKey, String requestKey) {
        if (request.getSession(false) == null) return;
        Object val = request.getSession(false).getAttribute(sessionKey);
        if (val != null) {
            request.setAttribute(requestKey, val);
            request.getSession(false).removeAttribute(sessionKey);
        }
    }
}

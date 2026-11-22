package com.hrms.controller.sysadmin;

import com.hrms.dao.sysadmin.PlatformAuditDAO;
import com.hrms.dao.sysadmin.RegistrationRequestDAO;
import com.hrms.util.SecuritySupport;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;

/** Read-only System Admin view for platform AuditLogs and EmailAttempts. */
public class PlatformAuditController extends HttpServlet {
    private final PlatformAuditDAO auditDAO = new PlatformAuditDAO();
    private final RegistrationRequestDAO requestDAO = new RegistrationRequestDAO();

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
        SecuritySupport.applyNoStore(response::setHeader);
        response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
    }
}

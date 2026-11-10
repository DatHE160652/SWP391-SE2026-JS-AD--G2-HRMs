package com.hrms.controller;

import com.hrms.dao.WorkspaceNotificationDAO;
import com.hrms.service.security.TenantPrincipal;
import com.hrms.util.Json;
import com.hrms.util.SecuritySupport;
import com.hrms.util.ValidationSupport;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class WorkspaceNotificationController extends HttpServlet {

    private final WorkspaceNotificationDAO notificationDAO = new WorkspaceNotificationDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        SecuritySupport.applyNoStore(response::setHeader);
        TenantPrincipal principal = SecuritySupport.getTenantPrincipal(request);
        if (principal == null || principal.getCompanyId() == null || principal.getUserId() == null) {
            writeJson(response, HttpServletResponse.SC_UNAUTHORIZED,
                    "{\"success\":false,\"message\":\"Vui lòng đăng nhập.\"}");
            return;
        }
        try {
            String csrfToken = SecuritySupport.ensurePublicCsrfToken(request.getSession(true));
            Map<String, Object> data = notificationDAO.getNotificationsForUser(principal);
            writeJson(response, HttpServletResponse.SC_OK,
                    "{\"success\":true,\"csrfToken\":" + ValidationSupport.quote(csrfToken)
                            + ",\"data\":" + Json.encode(data) + "}");
        } catch (SQLException e) {
            getServletContext().log("WorkspaceNotificationController.doGet failed", e);
            writeJson(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "{\"success\":false,\"message\":\"Không thể tải thông báo.\"}");
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        SecuritySupport.applyNoStore(response::setHeader);
        TenantPrincipal principal = SecuritySupport.getTenantPrincipal(request);
        if (principal == null || principal.getCompanyId() == null || principal.getUserId() == null) {
            writeJson(response, HttpServletResponse.SC_UNAUTHORIZED,
                    "{\"success\":false,\"message\":\"Vui lòng đăng nhập.\"}");
            return;
        }
        try {
            boolean markAll = "true".equalsIgnoreCase(request.getParameter("markAll"));
            List<String> keys = new ArrayList<>();
            String[] paramKeys = request.getParameterValues("key");
            if (paramKeys != null) {
                for (String pk : paramKeys) {
                    if (pk != null && !pk.isBlank()) {
                        keys.add(pk.trim());
                    }
                }
            }
            notificationDAO.markNotificationsRead(principal, keys, markAll);
            Map<String, Object> data = notificationDAO.getNotificationsForUser(principal);
            writeJson(response, HttpServletResponse.SC_OK,
                    "{\"success\":true,\"data\":" + Json.encode(data) + "}");
        } catch (SQLException e) {
            getServletContext().log("WorkspaceNotificationController.doPost failed", e);
            writeJson(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "{\"success\":false,\"message\":\"Không thể cập nhật trạng thái thông báo.\"}");
        }
    }

    private void writeJson(HttpServletResponse response, int status, String content) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json; charset=UTF-8");
        response.getWriter().write(content);
    }
}


package com.hrms.controller.auth;

import com.hrms.api.ApiRoutes;
import com.hrms.util.SecuritySupport;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/** Ends a System Admin session. */
public class SystemAdminLogoutController extends HttpServlet {
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        SecuritySupport.applyNoStore(response::setHeader);
        if (!SecuritySupport.hasValidCsrfToken(request)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        if (request.getSession(false) != null) {
            request.getSession(false).invalidate();
        }
        response.sendRedirect(request.getContextPath() + ApiRoutes.SYSTEM_ADMIN_LOGIN);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
    }
}

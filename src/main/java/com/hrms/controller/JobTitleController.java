package com.hrms.controller;

import com.hrms.util.SecuritySupport;
import com.hrms.util.ValidationSupport;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

public class JobTitleController extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        retired(request, response, false);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        retired(request, response, true);
    }

    private void retired(HttpServletRequest request, HttpServletResponse response, boolean mutation) throws IOException {
        SecuritySupport.applyNoStore(response::setHeader);
        response.setContentType("application/json;charset=UTF-8");
        try {
            SecuritySupport.requireCompanyId(request);
            SecuritySupport.requireOrganizationEditor(request);
            if (mutation && !SecuritySupport.hasValidPublicCsrfToken(request)) {
                throw new SecurityException("Phiên thao tác không hợp lệ. Vui lòng tải lại trang.");
            }
            response.setStatus(HttpServletResponse.SC_GONE);
            response.getWriter().write("{\"success\":false,\"message\":"
                    + ValidationSupport.quote("Danh mục Job Title đã ngừng sử dụng. Vui lòng quản lý Chức danh tại /positions.") + "}");
        } catch (SecurityException exception) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.getWriter().write("{\"success\":false,\"message\":" + ValidationSupport.quote(exception.getMessage()) + "}");
        }
    }
}

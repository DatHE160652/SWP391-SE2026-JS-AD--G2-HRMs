package com.hrms.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Map;

public class TenantPageController extends HttpServlet {
    private static final Map<String, String> VIEWS = Map.ofEntries(
            Map.entry("/employee/my-profile", "/WEB-INF/views/employee/my-profile.jsp"),
            Map.entry("/employee/organization", "/WEB-INF/views/employee/personal-workspace.jsp"),
            Map.entry("/employee/my-contracts", "/WEB-INF/views/employee/personal-workspace.jsp"),
            Map.entry("/employee/employee-list", "/WEB-INF/views/comadmin-hr/employee-list.jsp"),
            Map.entry("/employee/employee-form", "/WEB-INF/views/comadmin-hr/employee-form.jsp"),
            Map.entry("/employee/employee-detail", "/WEB-INF/views/comadmin-hr/employee-detail.jsp"),
            Map.entry("/department/department-list", "/WEB-INF/views/comadmin-hr/department-list.jsp"),
            Map.entry("/department/department-form", "/WEB-INF/views/comadmin-hr/department-form.jsp"),
            Map.entry("/department/department-detail", "/WEB-INF/views/comadmin-hr/organization-detail.jsp"),
            Map.entry("/position/position-list", "/WEB-INF/views/comadmin-hr/position-list.jsp"),
            Map.entry("/position/position-form", "/WEB-INF/views/comadmin-hr/position-form.jsp"),
            Map.entry("/position/position-detail", "/WEB-INF/views/comadmin-hr/organization-detail.jsp"),
            Map.entry("/responsibility-level/responsibility-level-list", "/WEB-INF/views/comadmin-hr/responsibility-level-list.jsp"),
            Map.entry("/responsibility-level/responsibility-level-form", "/WEB-INF/views/comadmin-hr/responsibility-level-form.jsp"),
            Map.entry("/responsibility-level/responsibility-level-detail", "/WEB-INF/views/comadmin-hr/organization-detail.jsp"),
            Map.entry("/document/employee-document-list", "/WEB-INF/views/comadmin-hr/employee-document-list.jsp"),
            Map.entry("/document/document-upload", "/WEB-INF/views/comadmin-hr/document-upload.jsp")
    );

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String path = request.getServletPath();
        if (path.endsWith(".html")) {
            String canonical = path.substring(0, path.length() - ".html".length());
            String query = request.getQueryString();
            response.sendRedirect(request.getContextPath() + canonical + (query == null ? "" : "?" + query));
            return;
        }

        if (path.startsWith("/job-title/")) {
            response.sendRedirect(request.getContextPath() + "/position/position-list");
            return;
        }

        if ("/position/position-form".equals(path) && "1".equals(request.getParameter("view"))) {
            String id = request.getParameter("id");
            if (id == null || !id.matches("[1-9][0-9]*")) {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST);
                return;
            }
            response.sendRedirect(request.getContextPath() + "/position/position-detail?id=" + id);
            return;
        }

        String view = VIEWS.get(path);
        if (view == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        request.setAttribute("organizationCsrfToken",
                com.hrms.util.SecuritySupport.ensurePublicCsrfToken(request.getSession()));
        request.setAttribute("personalView","/employee/my-contracts".equals(path)?"contracts":"organization");
        request.getRequestDispatcher(view).forward(request, response);
    }
}

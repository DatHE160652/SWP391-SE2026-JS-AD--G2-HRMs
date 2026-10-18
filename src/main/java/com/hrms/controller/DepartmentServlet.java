package com.hrms.controller;

import com.hrms.util.ValidationSupport;

import com.hrms.model.Department;
import com.hrms.service.DepartmentService;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

public class DepartmentServlet extends HttpServlet {
    private final DepartmentService departmentService = new DepartmentService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            if ("/departments/detail".equals(request.getServletPath())) {
                Department department = departmentService.getDepartmentDetail(parseRequiredInt(request.getParameter("id")));
                if (department == null) {
                    writeJson(response, HttpServletResponse.SC_NOT_FOUND, error("Department not found."));
                    return;
                }
                writeJson(response, HttpServletResponse.SC_OK, "{\"success\":true,\"data\":" + toJson(department) + "}");
                return;
            }

            List<Department> departments = departmentService.getDepartments();
            writeJson(response, HttpServletResponse.SC_OK, "{\"success\":true,\"data\":" + toJsonArray(departments) + "}");
        } catch (SQLException | IllegalArgumentException e) {
            writeJson(response, HttpServletResponse.SC_BAD_REQUEST, ValidationSupport.errorJson(e));
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            boolean success;
            String message;
            if ("/departments/delete".equals(request.getServletPath())) {
                success = departmentService.deleteDepartment(parseRequiredInt(request.getParameter("departmentId")));
                message = success ? "Department deleted successfully." : "No department record was changed.";
            } else if ("/departments/update".equals(request.getServletPath())) {
                success = departmentService.updateDepartment(readDepartment(request));
                message = success ? "Department updated successfully." : "No department record was changed.";
            } else {
                success = departmentService.createDepartment(readDepartment(request));
                message = success ? "Department created successfully." : "No department record was changed.";
            }

            writeJson(response, HttpServletResponse.SC_OK,
                    "{\"success\":" + success + ",\"message\":" + quote(message) + "}");
        } catch (SQLException | IllegalArgumentException e) {
            writeJson(response, HttpServletResponse.SC_BAD_REQUEST, ValidationSupport.errorJson(e));
        }
    }

    private Department readDepartment(HttpServletRequest request) {
        Department department = new Department();
        department.setDepartmentId(parseOptionalInt(request.getParameter("departmentId")));
        department.setDepartmentCode(trimToNull(request.getParameter("departmentCode")));
        department.setDepartmentName(trimToNull(request.getParameter("departmentName")));
        return department;
    }

    private String toJsonArray(List<Department> departments) {
        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < departments.size(); i++) {
            if (i > 0) {
                json.append(",");
            }
            json.append(toJson(departments.get(i)));
        }
        return json.append("]").toString();
    }

    private String toJson(Department department) {
        return "{"
                + "\"departmentId\":" + department.getDepartmentId() + ","
                + "\"companyId\":" + department.getCompanyId() + ","
                + "\"departmentCode\":" + quote(department.getDepartmentCode()) + ","
                + "\"departmentName\":" + quote(department.getDepartmentName()) + ","
                + "\"createdAt\":" + quote(department.getCreatedAt() == null ? null : department.getCreatedAt().toString())
                + "}";
    }

    private void writeJson(HttpServletResponse response, int status, String json) throws IOException {
        response.setStatus(status);
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json; charset=UTF-8");
        response.getWriter().write(json);
    }

    private String error(String message) {
        return "{\"success\":false,\"message\":" + quote(message) + "}";
    }

    private String quote(String value) {
        if (value == null) {
            return "null";
        }
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    private int parseRequiredInt(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Required numeric value is missing.");
        }
        return Integer.parseInt(value);
    }

    private int parseOptionalInt(String value) {
        return value == null || value.isBlank() ? 0 : Integer.parseInt(value);
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}

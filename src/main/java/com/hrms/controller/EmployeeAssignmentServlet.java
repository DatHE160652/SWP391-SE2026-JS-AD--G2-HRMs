package com.hrms.controller;

import com.hrms.model.EmployeeAssignment;
import com.hrms.service.EmployeeAssignmentService;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;

public class EmployeeAssignmentServlet extends HttpServlet {
    private final EmployeeAssignmentService employeeAssignmentService = new EmployeeAssignmentService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            EmployeeAssignment assignment = employeeAssignmentService.getEmployeeOrganization(
                    parseRequiredInt(request.getParameter("id")));
            writeJson(response, HttpServletResponse.SC_OK, "{\"success\":true,\"data\":" + toJson(assignment) + "}");
        } catch (SQLException | IllegalArgumentException e) {
            writeJson(response, HttpServletResponse.SC_BAD_REQUEST, error(e.getMessage()));
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            int employeeId = parseRequiredInt(request.getParameter("employeeId"));
            boolean success;
            String message;

            if ("/employees/assign-department".equals(request.getServletPath())) {
                success = employeeAssignmentService.assignDepartment(employeeId,
                        parseRequiredInt(request.getParameter("departmentId")));
                message = success ? "Department assigned successfully." : "No employee record was changed.";
            } else if ("/employees/assign-position".equals(request.getServletPath())) {
                success = employeeAssignmentService.assignPosition(employeeId,
                        parseRequiredInt(request.getParameter("positionId")));
                message = success ? "Position assigned successfully." : "No employee record was changed.";
            } else if ("/employees/assign-manager".equals(request.getServletPath())) {
                success = employeeAssignmentService.assignManager(employeeId,
                        parseRequiredInt(request.getParameter("managerId")));
                message = success ? "Manager assigned successfully." : "No employee record was changed.";
            } else {
                throw new IllegalArgumentException("Unsupported assignment action.");
            }

            writeJson(response, HttpServletResponse.SC_OK,
                    "{\"success\":" + success + ",\"message\":" + quote(message) + "}");
        } catch (SQLException | IllegalArgumentException e) {
            writeJson(response, HttpServletResponse.SC_BAD_REQUEST, error(e.getMessage()));
        }
    }

    private String toJson(EmployeeAssignment assignment) {
        return "{"
                + "\"employeeId\":" + assignment.getEmployeeId() + ","
                + "\"departmentId\":" + nullableInteger(assignment.getDepartmentId()) + ","
                + "\"positionId\":" + nullableInteger(assignment.getPositionId()) + ","
                + "\"managerId\":" + nullableInteger(assignment.getManagerId())
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
        return "\"" + value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "\\r")
                .replace("\n", "\\n")
                .replace("\t", "\\t")
                + "\"";
    }

    private String nullableInteger(Integer value) {
        return value == null ? "null" : value.toString();
    }

    private int parseRequiredInt(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Required numeric value is missing.");
        }
        return Integer.parseInt(value);
    }
}

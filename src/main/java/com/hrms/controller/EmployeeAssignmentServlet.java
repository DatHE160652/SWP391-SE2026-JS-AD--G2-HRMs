package com.hrms.controller;

import com.hrms.util.ValidationSupport;

import com.hrms.model.EmployeeAssignment;
import com.hrms.dao.EmployeeAssignmentDAO.DepartmentMemberOption;
import com.hrms.service.EmployeeAssignmentService;
import com.hrms.service.EmployeeHistoryService;
import com.hrms.service.security.TenantPrincipal;
import com.hrms.util.SecuritySupport;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

public class EmployeeAssignmentServlet extends HttpServlet {
    private final EmployeeAssignmentService employeeAssignmentService = new EmployeeAssignmentService();
    private final EmployeeHistoryService employeeHistoryService = new EmployeeHistoryService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            if ("/departments/members".equals(request.getServletPath())) {
                List<DepartmentMemberOption> employees = employeeAssignmentService.getDepartmentMemberOptions(
                        parseRequiredInt(request.getParameter("departmentId")));
                writeJson(response, HttpServletResponse.SC_OK,
                        "{\"success\":true,\"data\":" + toJsonDepartmentMembers(employees) + "}");
                return;
            }

            EmployeeAssignment assignment = employeeAssignmentService.getEmployeeOrganization(
                    parseRequiredInt(request.getParameter("id")));
            writeJson(response, HttpServletResponse.SC_OK, "{\"success\":true,\"data\":" + toJson(assignment) + "}");
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
            TenantPrincipal principal = SecuritySupport.getTenantPrincipal(request);

            if ("/departments/assign-members".equals(request.getServletPath())) {
                int departmentId = parseRequiredInt(request.getParameter("departmentId"));
                int managerId = parseRequiredInt(request.getParameter("managerId"));
                int[] employeeIds = parseIntArray(request.getParameter("employeeIds"));
                success = employeeAssignmentService.assignDepartmentMembers(
                        departmentId,
                        managerId,
                        employeeIds);
                if (success) {
                    logAssignment(principal, managerId, "ASSIGN_TEAM",
                            "-", "DepartmentId: " + departmentId + "; Team size: " + employeeIds.length, request);
                    for (int employeeId : employeeIds) {
                        logAssignment(principal, employeeId, "ASSIGN_TO_MANAGER",
                                "-", "ManagerId: " + managerId + "; DepartmentId: " + departmentId, request);
                    }
                }
                message = success ? "Department members assigned successfully." : "No employee record was changed.";
            } else if ("/employees/assign-department".equals(request.getServletPath())) {
                int employeeId = parseRequiredInt(request.getParameter("employeeId"));
                EmployeeAssignment before = employeeAssignmentService.getEmployeeOrganization(employeeId);
                int departmentId = parseRequiredInt(request.getParameter("departmentId"));
                success = employeeAssignmentService.assignDepartment(employeeId,
                        departmentId);
                if (success) {
                    logAssignment(principal, employeeId, "CHANGE_DEPARTMENT",
                            "DepartmentId: " + nullableInteger(before.getDepartmentId()),
                            "DepartmentId: " + departmentId, request);
                }
                message = success ? "Department assigned successfully." : "No employee record was changed.";
            } else if ("/employees/assign-position".equals(request.getServletPath())) {
                int employeeId = parseRequiredInt(request.getParameter("employeeId"));
                EmployeeAssignment before = employeeAssignmentService.getEmployeeOrganization(employeeId);
                String positionId = request.getParameter("positionId");
                if (positionId == null || positionId.isBlank()) {
                    success = employeeAssignmentService.clearPosition(employeeId);
                    if (success) {
                        logAssignment(principal, employeeId, "CHANGE_POSITION",
                                "PositionId: " + nullableInteger(before.getPositionId()),
                                "PositionId: -", request);
                    }
                    message = success ? "Position cleared; employee role applied." : "No employee record was changed.";
                } else {
                    int parsedPositionId = parseRequiredInt(positionId);
                    success = employeeAssignmentService.assignPosition(employeeId, parsedPositionId);
                    if (success) {
                        logAssignment(principal, employeeId, "CHANGE_POSITION",
                                "PositionId: " + nullableInteger(before.getPositionId()),
                                "PositionId: " + parsedPositionId, request);
                    }
                    message = success ? "Position assigned successfully." : "No employee record was changed.";
                }
            } else if ("/employees/assign-manager".equals(request.getServletPath())) {
                int employeeId = parseRequiredInt(request.getParameter("employeeId"));
                EmployeeAssignment before = employeeAssignmentService.getEmployeeOrganization(employeeId);
                int managerId = parseRequiredInt(request.getParameter("managerId"));
                success = employeeAssignmentService.assignManager(employeeId,
                        managerId);
                if (success) {
                    logAssignment(principal, employeeId, "CHANGE_MANAGER",
                            "ManagerId: " + nullableInteger(before.getManagerId()),
                            "ManagerId: " + managerId, request);
                }
                message = success ? "Manager assigned successfully." : "No employee record was changed.";
            } else {
                throw new IllegalArgumentException("Unsupported assignment action.");
            }

            writeJson(response, HttpServletResponse.SC_OK,
                    "{\"success\":" + success + ",\"message\":" + quote(message) + "}");
        } catch (SQLException | IllegalArgumentException e) {
            writeJson(response, HttpServletResponse.SC_BAD_REQUEST, ValidationSupport.errorJson(e));
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

    private void logAssignment(TenantPrincipal principal, int employeeId, String action,
                               String oldValue, String newValue, HttpServletRequest request) throws SQLException {
        employeeHistoryService.logEmployeeChange(
                principal == null ? null : principal.getCompanyId(),
                principal == null ? null : principal.getUserId(),
                employeeId,
                action,
                oldValue,
                newValue,
                request.getRemoteAddr());
    }

    private String toJsonDepartmentMembers(List<DepartmentMemberOption> employees) {
        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < employees.size(); i++) {
            if (i > 0) {
                json.append(",");
            }
            DepartmentMemberOption employee = employees.get(i);
            json.append("{")
                    .append("\"employeeId\":").append(employee.employeeId()).append(",")
                    .append("\"employeeCode\":").append(quote(employee.employeeCode())).append(",")
                    .append("\"fullName\":").append(quote(employee.fullName())).append(",")
                    .append("\"departmentId\":").append(nullableInteger(employee.departmentId())).append(",")
                    .append("\"managerId\":").append(nullableInteger(employee.managerId())).append(",")
                    .append("\"positionId\":").append(nullableInteger(employee.positionId())).append(",")
                    .append("\"positionTitle\":").append(quote(employee.positionTitle())).append(",")
                    .append("\"roleCode\":").append(quote(employee.roleCode())).append(",")
                    .append("\"selected\":").append(employee.selected())
                    .append("}");
        }
        return json.append("]").toString();
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

    private int[] parseIntArray(String value) {
        if (value == null || value.isBlank()) {
            return new int[0];
        }
        String[] parts = value.split(",");
        int[] values = new int[parts.length];
        for (int i = 0; i < parts.length; i++) {
            values[i] = parseRequiredInt(parts[i].trim());
        }
        return values;
    }
}

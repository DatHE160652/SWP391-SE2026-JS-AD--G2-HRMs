package com.hrms.controller;

import com.hrms.model.Employee;
import com.hrms.service.EmployeeService;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Date;
import java.sql.SQLException;
import java.util.List;

public class EmployeeServlet extends HttpServlet {
    private final EmployeeService employeeService = new EmployeeService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            if ("/employees/detail".equals(request.getServletPath())) {
                Employee employee = employeeService.getEmployeeById(parseRequiredInt(request.getParameter("id")));
                if (employee == null) {
                    writeJson(response, HttpServletResponse.SC_NOT_FOUND, error("Employee not found."));
                    return;
                }
                writeJson(response, HttpServletResponse.SC_OK, "{\"success\":true,\"data\":" + toJson(employee) + "}");
                return;
            }

            List<Employee> employees = employeeService.getAllEmployees();
            writeJson(response, HttpServletResponse.SC_OK, "{\"success\":true,\"data\":" + toJsonArray(employees) + "}");
        } catch (SQLException | IllegalArgumentException e) {
            writeJson(response, HttpServletResponse.SC_BAD_REQUEST, error(e.getMessage()));
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            boolean success;
            String message;
            if ("/employees/delete".equals(request.getServletPath())) {
                success = employeeService.deleteEmployee(parseRequiredInt(request.getParameter("employeeId")));
                message = success ? "Employee deleted successfully." : "No employee record was changed.";
            } else if ("/employees/update".equals(request.getServletPath())) {
                success = employeeService.updateEmployee(readEmployee(request));
                message = success ? "Employee updated successfully." : "No employee record was changed.";
            } else {
                success = employeeService.createEmployee(readEmployee(request));
                message = success
                        ? "Employee created successfully and account generated."
                        : "No employee record was changed.";
            }

            writeJson(response, HttpServletResponse.SC_OK,
                    "{\"success\":" + success + ",\"message\":" + quote(message) + "}");
        } catch (SQLException | IllegalArgumentException e) {
            writeJson(response, HttpServletResponse.SC_BAD_REQUEST, error(e.getMessage()));
        }
    }

    private Employee readEmployee(HttpServletRequest request) {
        Employee employee = new Employee();
        employee.setEmployeeId(parseOptionalInt(request.getParameter("employeeId")));
        employee.setEmployeeCode(trimToNull(request.getParameter("employeeCode")));
        employee.setFullName(trimToNull(request.getParameter("fullName")));
        employee.setPhone(trimToNull(request.getParameter("phone")));
        employee.setGender(trimToNull(request.getParameter("gender")));
        employee.setDateOfBirth(parseDate(request.getParameter("dateOfBirth")));
        employee.setStartDate(parseDate(request.getParameter("startDate")));
        employee.setBaseSalary(parseBigDecimal(request.getParameter("baseSalary")));
        employee.setStatus(trimToNull(request.getParameter("status")));
        return employee;
    }

    private void writeJson(HttpServletResponse response, int status, String json) throws IOException {
        response.setStatus(status);
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json; charset=UTF-8");
        response.getWriter().write(json);
    }

    private String toJsonArray(List<Employee> employees) {
        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < employees.size(); i++) {
            if (i > 0) {
                json.append(",");
            }
            json.append(toJson(employees.get(i)));
        }
        return json.append("]").toString();
    }

    private String toJson(Employee employee) {
        return "{"
                + "\"employeeId\":" + employee.getEmployeeId() + ","
                + "\"employeeCode\":" + quote(employee.getEmployeeCode()) + ","
                + "\"fullName\":" + quote(employee.getFullName()) + ","
                + "\"phone\":" + quote(employee.getPhone()) + ","
                + "\"gender\":" + quote(employee.getGender()) + ","
                + "\"dateOfBirth\":" + quoteDate(employee.getDateOfBirth()) + ","
                + "\"startDate\":" + quoteDate(employee.getStartDate()) + ","
                + "\"baseSalary\":" + employee.getBaseSalary() + ","
                + "\"status\":" + quote(employee.getStatus()) + ","
                + "\"userId\":" + nullableInteger(employee.getUserId()) + ","
                + "\"username\":" + quote(employee.getUsername()) + ","
                + "\"accountEmail\":" + quote(employee.getAccountEmail()) + ","
                + "\"accountActive\":" + nullableBoolean(employee.getAccountActive())
                + "}";
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

    private String quoteDate(Date date) {
        return date == null ? "null" : quote(date.toString());
    }

    private String nullableInteger(Integer value) {
        return value == null ? "null" : value.toString();
    }

    private String nullableBoolean(Boolean value) {
        return value == null ? "null" : value.toString();
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

    private Date parseDate(String value) {
        return value == null || value.isBlank() ? null : Date.valueOf(value);
    }

    private BigDecimal parseBigDecimal(String value) {
        return value == null || value.isBlank() ? BigDecimal.ZERO : new BigDecimal(value);
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}

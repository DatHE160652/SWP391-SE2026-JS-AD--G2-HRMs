package com.hrms.controller;

import com.hrms.model.Employee;
import com.hrms.dao.AuditLogDAO;
import com.hrms.service.EmployeeHistoryService;
import com.hrms.service.security.TenantPrincipal;
import com.hrms.service.EmployeeService;
import com.hrms.service.FieldValidationException;
import com.hrms.util.SecuritySupport;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.Date;
import java.sql.SQLException;
import java.util.List;

public class EmployeeServlet extends HttpServlet {
    private final EmployeeService employeeService = new EmployeeService();
    private final EmployeeHistoryService employeeHistoryService = new EmployeeHistoryService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            if ("/employees/me".equals(request.getServletPath())) {
                TenantPrincipal principal = SecuritySupport.getTenantPrincipal(request);
                if (principal == null || principal.getEmployeeId() == null) {
                    writeJson(response, HttpServletResponse.SC_UNAUTHORIZED, error("Employee session is required."));
                    return;
                }
                Employee employee = employeeService.getEmployeeById(principal.getEmployeeId());
                if (employee == null) {
                    writeJson(response, HttpServletResponse.SC_NOT_FOUND, error("Employee not found."));
                    return;
                }
                writeJson(response, HttpServletResponse.SC_OK,
                        "{\"success\":true,\"data\":" + toJson(employee, principal.getRoleCode()) + "}");
                return;
            }

            if ("/employees/me/history".equals(request.getServletPath())) {
                TenantPrincipal principal = SecuritySupport.getTenantPrincipal(request);
                if (principal == null || principal.getEmployeeId() == null) {
                    writeJson(response, HttpServletResponse.SC_UNAUTHORIZED, error("Employee session is required."));
                    return;
                }
                writeJson(response, HttpServletResponse.SC_OK,
                        "{\"success\":true,\"data\":" + toHistoryJson(employeeHistoryService.getEmployeeHistory(principal.getEmployeeId())) + "}");
                return;
            }

            if ("/employees/detail".equals(request.getServletPath())) {
                Employee employee = employeeService.getEmployeeById(parseRequiredInt(request.getParameter("id")));
                if (employee == null) {
                    writeJson(response, HttpServletResponse.SC_NOT_FOUND, error("Employee not found."));
                    return;
                }
                writeJson(response, HttpServletResponse.SC_OK, "{\"success\":true,\"data\":" + toJson(employee) + "}");
                return;
            }

            if ("/employees/history".equals(request.getServletPath())) {
                int employeeId = parseRequiredInt(request.getParameter("id"));
                writeJson(response, HttpServletResponse.SC_OK,
                        "{\"success\":true,\"data\":" + toHistoryJson(employeeHistoryService.getEmployeeHistory(employeeId)) + "}");
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
            } else if ("/employees/me/update-profile".equals(request.getServletPath())) {
                TenantPrincipal principal = SecuritySupport.getTenantPrincipal(request);
                if (principal == null || principal.getEmployeeId() == null) {
                    writeJson(response, HttpServletResponse.SC_UNAUTHORIZED, error("Employee session is required."));
                    return;
                }
                Employee employee = employeeService.getEmployeeById(principal.getEmployeeId());
                if (employee == null) {
                    writeJson(response, HttpServletResponse.SC_NOT_FOUND, error("Employee not found."));
                    return;
                }
                String oldValue = profileSnapshot(employee);
                employee.setFullName(trimToNull(request.getParameter("fullName")));
                employee.setPhone(trimToNull(request.getParameter("phone")));
                employee.setGender(trimToNull(request.getParameter("gender")));
                employee.setDateOfBirth(parseDate(request.getParameter("dateOfBirth")));
                success = employeeService.updateEmployee(employee);
                if (success) {
                    employeeHistoryService.logEmployeeChange(principal.getCompanyId(), principal.getUserId(),
                            employee.getEmployeeId(), "UPDATE_PROFILE", oldValue, profileSnapshot(employee),
                            request.getRemoteAddr());
                }
                message = success ? "Personal profile updated successfully." : "No employee record was changed.";
            } else if ("/employees/update".equals(request.getServletPath())) {
                TenantPrincipal principal = SecuritySupport.getTenantPrincipal(request);
                Employee updatedEmployee = readEmployee(request);
                Employee oldEmployee = employeeService.getEmployeeById(updatedEmployee.getEmployeeId());
                success = employeeService.updateEmployee(updatedEmployee);
                if (success) {
                    employeeHistoryService.logEmployeeChange(
                            principal == null ? null : principal.getCompanyId(),
                            principal == null ? null : principal.getUserId(),
                            updatedEmployee.getEmployeeId(),
                            "UPDATE_EMPLOYEE",
                            profileSnapshot(oldEmployee),
                            profileSnapshot(updatedEmployee),
                            request.getRemoteAddr());
                }
                message = success ? "Employee updated successfully." : "No employee record was changed.";
            } else {
                TenantPrincipal principal = SecuritySupport.getTenantPrincipal(request);
                if (principal == null || principal.getCompanyId() == null) {
                    writeJson(response, HttpServletResponse.SC_UNAUTHORIZED, error("Company session is required."));
                    return;
                }
                Employee employee = readEmployee(request);
                success = employeeService.createEmployee(employee, principal.getCompanyId(),
                        request.getParameter("employeeCodeMode"));
                if (success) {
                    employeeHistoryService.logEmployeeChange(
                            principal == null ? null : principal.getCompanyId(),
                            principal == null ? null : principal.getUserId(),
                            employee.getEmployeeId(),
                            "CREATE_EMPLOYEE",
                            "-",
                            profileSnapshot(employee),
                            request.getRemoteAddr());
                }
                message = success
                        ? "Employee created successfully and account generated."
                        : "No employee record was changed.";
            }

            writeJson(response, HttpServletResponse.SC_OK,
                    "{\"success\":" + success + ",\"message\":" + quote(message) + "}");
        } catch (FieldValidationException exception) {
            writeJson(response, HttpServletResponse.SC_BAD_REQUEST,
                    "{\"success\":false,\"message\":" + quote(exception.getMessage())
                            + ",\"fieldErrors\":{" + quote(exception.getField()) + ":"
                            + quote(exception.getMessage()) + "}}");
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
        employee.setIdentityNumber(trimToNull(request.getParameter("identityNumber")));
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
                + "\"identityNumber\":" + quote(employee.getIdentityNumber()) + ","
                + "\"status\":" + quote(employee.getStatus()) + ","
                + "\"userId\":" + nullableInteger(employee.getUserId()) + ","
                + "\"username\":" + quote(employee.getUsername()) + ","
                + "\"accountEmail\":" + quote(employee.getAccountEmail()) + ","
                + "\"accountActive\":" + nullableBoolean(employee.getAccountActive())
                + "}";
    }

    private String toJson(Employee employee, String roleCode) {
        String json = toJson(employee);
        return json.substring(0, json.length() - 1)
                + ",\"roleCode\":" + quote(roleCode)
                + "}";
    }

    private String toHistoryJson(List<AuditLogDAO.HistoryEntry> history) {
        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < history.size(); i++) {
            if (i > 0) {
                json.append(",");
            }
            AuditLogDAO.HistoryEntry entry = history.get(i);
            json.append("{")
                    .append("\"createdAt\":").append(quote(entry.createdAt())).append(",")
                    .append("\"action\":").append(quote(entry.action())).append(",")
                    .append("\"changedBy\":").append(quote(entry.changedBy())).append(",")
                    .append("\"oldValue\":").append(quote(entry.oldValue())).append(",")
                    .append("\"newValue\":").append(quote(entry.newValue()))
                    .append("}");
        }
        return json.append("]").toString();
    }

    private String profileSnapshot(Employee employee) {
        if (employee == null) {
            return "-";
        }
        return "Name: " + safe(employee.getFullName())
                + "; Phone: " + safe(employee.getPhone())
                + "; Gender: " + safe(employee.getGender())
                + "; DOB: " + safe(employee.getDateOfBirth() == null ? null : employee.getDateOfBirth().toString())
                + "; Status: " + safe(employee.getStatus());
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "-" : value;
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
        try {
            return value == null || value.isBlank() ? null : Date.valueOf(value);
        } catch (IllegalArgumentException exception) {
            throw new FieldValidationException("dateOfBirth", "Ngày sinh không hợp lệ.");
        }
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}

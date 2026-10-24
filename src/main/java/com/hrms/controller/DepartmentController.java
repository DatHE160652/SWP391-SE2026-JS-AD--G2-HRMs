package com.hrms.controller;

import com.hrms.util.ValidationSupport;
import com.hrms.util.SecuritySupport;

import com.hrms.model.Department;
import com.hrms.service.DepartmentService;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

public class DepartmentController extends HttpServlet {
    private final DepartmentService departmentService = new DepartmentService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        SecuritySupport.applyNoStore(response::setHeader);
        try {
            int companyId = SecuritySupport.requireCompanyId(request);
            if ("/departments/detail".equals(request.getServletPath())) {
                Department department = departmentService.getDepartmentDetail(companyId, parseRequiredInt(request.getParameter("id")));
                if (department == null) {
                    writeJson(response, HttpServletResponse.SC_NOT_FOUND, error("Không tìm thấy phòng ban."));
                    return;
                }
                writeJson(response, HttpServletResponse.SC_OK, "{\"success\":true,\"data\":" + toJson(department) + "}");
                return;
            }

            List<Department> departments = departmentService.getDepartments(companyId);
            writeJson(response, HttpServletResponse.SC_OK, "{\"success\":true,\"data\":" + toJsonArray(departments) + "}");
        } catch (SecurityException e) {
            writeJson(response, HttpServletResponse.SC_FORBIDDEN, error(e.getMessage()));
        } catch (SQLException e) {
            log("Organization database operation failed", e);
            writeJson(response, HttpServletResponse.SC_BAD_REQUEST, error("Không lưu hoặc tải được dữ liệu. Vui lòng kiểm tra dữ liệu trùng và các bản ghi liên quan."));
        } catch (IllegalArgumentException e) {
            writeJson(response, HttpServletResponse.SC_BAD_REQUEST, ValidationSupport.errorJson(e));
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        SecuritySupport.applyNoStore(response::setHeader);
        try {
            int companyId = SecuritySupport.requireCompanyId(request);
            if (!SecuritySupport.hasValidPublicCsrfToken(request)) throw new SecurityException("Phiên thao tác không hợp lệ. Vui lòng tải lại trang.");
            boolean success;
            String message;
            if ("/departments/deactivate".equals(request.getServletPath()) || "/departments/activate".equals(request.getServletPath())) {
                boolean active = "/departments/activate".equals(request.getServletPath());
                success = departmentService.setDepartmentActive(companyId, parseRequiredInt(request.getParameter("departmentId")), active);
                message = active ? "Đã kích hoạt phòng ban." : "Đã ngừng hoạt động phòng ban.";
            } else if ("/departments/update".equals(request.getServletPath())) {
                success = departmentService.updateDepartment(companyId, readDepartment(request));
                message = success ? "Đã cập nhật phòng ban." : "Không có phòng ban nào được thay đổi.";
            } else {
                success = departmentService.createDepartment(companyId, readDepartment(request));
                message = success ? "Đã tạo phòng ban." : "Không có phòng ban nào được thay đổi.";
            }

            writeJson(response, HttpServletResponse.SC_OK,
                    "{\"success\":" + success + ",\"message\":" + quote(message) + "}");
        } catch (SecurityException e) {
            writeJson(response, HttpServletResponse.SC_FORBIDDEN, error(e.getMessage()));
        } catch (SQLException e) {
            log("Organization database operation failed", e);
            writeJson(response, HttpServletResponse.SC_BAD_REQUEST, error("Không lưu hoặc tải được dữ liệu. Vui lòng kiểm tra dữ liệu trùng và các bản ghi liên quan."));
        } catch (IllegalArgumentException e) {
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
                + "\"isActive\":" + department.isActive() + ","
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
        return ValidationSupport.quote(value);
    }

    private int parseRequiredInt(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Vui lòng nhập giá trị số bắt buộc.");
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

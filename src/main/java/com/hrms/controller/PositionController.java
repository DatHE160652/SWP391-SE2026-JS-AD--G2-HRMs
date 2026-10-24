package com.hrms.controller;

import com.hrms.util.ValidationSupport;
import com.hrms.util.SecuritySupport;

import com.hrms.model.Position;
import com.hrms.service.PositionService;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

public class PositionController extends HttpServlet {
    private final PositionService positionService = new PositionService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        SecuritySupport.applyNoStore(response::setHeader);
        try {
            int companyId = SecuritySupport.requireCompanyId(request);
            SecuritySupport.requireOrganizationEditor(request);
            if ("/positions/detail".equals(request.getServletPath())) {
                Position position = positionService.getPositionDetail(companyId, parseRequiredInt(request.getParameter("id")));
                if (position == null) {
                    writeJson(response, HttpServletResponse.SC_NOT_FOUND, error("Không tìm thấy chức danh."));
                    return;
                }
                writeJson(response, HttpServletResponse.SC_OK, "{\"success\":true,\"data\":" + toJson(position) + "}");
                return;
            }

            if ("catalogs".equals(request.getParameter("action"))) {
                writeJson(response, HttpServletResponse.SC_OK, "{\"success\":true,\"data\":" + positionService.getCatalogJson(companyId) + "}");
                return;
            }
            List<Position> positions = positionService.getPositions(companyId,
                    parseOptionalInteger(request.getParameter("departmentId")), "true".equals(request.getParameter("activeOnly")));
            writeJson(response, HttpServletResponse.SC_OK, "{\"success\":true,\"data\":" + toJsonArray(positions) + "}");
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
            SecuritySupport.requireOrganizationEditor(request);
            if (!SecuritySupport.hasValidPublicCsrfToken(request)) throw new SecurityException("Phiên thao tác không hợp lệ. Vui lòng tải lại trang.");
            boolean success;
            String message;
            if ("/positions/deactivate".equals(request.getServletPath())) {
                success = positionService.deactivatePosition(companyId, parseRequiredInt(request.getParameter("positionId")));
                message = success ? "Đã ngừng hoạt động chức danh." : "Không có chức danh nào được thay đổi.";
            } else if ("/positions/activate".equals(request.getServletPath())) {
                success = positionService.setPositionActive(companyId, parseRequiredInt(request.getParameter("positionId")), true);
                message = success ? "Đã kích hoạt chức danh." : "Không có chức danh nào được thay đổi.";
            } else if ("/positions/update".equals(request.getServletPath())) {
                success = positionService.updatePosition(companyId, readPosition(request));
                message = success ? "Đã cập nhật chức danh." : "Không có chức danh nào được thay đổi.";
            } else {
                success = positionService.createPosition(companyId, readPosition(request));
                message = success ? "Đã tạo chức danh." : "Không có chức danh nào được thay đổi.";
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

    private Position readPosition(HttpServletRequest request) {
        Position position = new Position();
        position.setPositionId(parseOptionalInt(request.getParameter("positionId")));
        position.setDepartmentId(parseOptionalInteger(request.getParameter("departmentId")));
        if (request.getParameter("reportsToPositionId") != null)
            position.setReportsToPositionId(parseOptionalInteger(request.getParameter("reportsToPositionId")));
        position.setResponsibilityLevelId(parseOptionalInteger(request.getParameter("responsibilityLevelId")));
        position.setActive(!"false".equals(request.getParameter("isActive")));
        String canManage = request.getParameter("canManageEmployees");
        if (canManage != null && !java.util.Set.of("true", "false", "on", "1", "0").contains(canManage)) {
            throw new com.hrms.service.FieldValidationException("canManageEmployees", "Quyền quản lý không hợp lệ.");
        }
        position.setCanManageEmployees("true".equals(canManage) || "on".equals(canManage) || "1".equals(canManage));
        position.setPositionTitle(trimToNull(request.getParameter("positionTitle")));
        position.setDescription(trimToNull(request.getParameter("description")));
        String allowance=request.getParameter("positionAllowance");
        if (allowance!=null && !allowance.isBlank())
            throw new com.hrms.service.FieldValidationException("positionAllowance","Phụ cấp được lấy từ cấp trách nhiệm; không cấu hình riêng cho chức danh.");
        return position;
    }

    private String toJsonArray(List<Position> positions) {
        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < positions.size(); i++) {
            if (i > 0) {
                json.append(",");
            }
            json.append(toJson(positions.get(i)));
        }
        return json.append("]").toString();
    }

    private String toJson(Position position) {
        return "{"
                + "\"positionId\":" + position.getPositionId() + ","
                + "\"assignmentOccupied\":" + position.isAssignmentOccupied() + ","
                + "\"reportsToPositionId\":" + nullableInteger(position.getReportsToPositionId()) + ","
                + "\"companyId\":" + position.getCompanyId() + ","
                + "\"departmentId\":" + nullableInteger(position.getDepartmentId()) + ","
                + "\"departmentName\":" + quote(position.getDepartmentName()) + ","
                + "\"positionTitle\":" + quote(position.getPositionTitle()) + ","
                + "\"responsibilityLevelId\":" + position.getResponsibilityLevelId() + ","
                + "\"responsibilityLevelName\":" + quote(position.getResponsibilityLevelName()) + ","
                + "\"effectiveAllowance\":" + position.getEffectiveAllowance() + ","
                + "\"isActive\":" + position.isActive() + ","
                + "\"canManageEmployees\":" + position.isCanManageEmployees() + ","
                + "\"description\":" + quote(position.getDescription()) + ","
                + "\"createdAt\":" + quote(position.getCreatedAt() == null ? null : position.getCreatedAt().toString())
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

    private Integer parseOptionalInteger(String value) {
        return value == null || value.isBlank() ? null : Integer.parseInt(value);
    }

    private String nullableInteger(Integer value) {
        return value == null ? "null" : value.toString();
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}

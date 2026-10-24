package com.hrms.controller;

import com.hrms.util.ValidationSupport;

import com.hrms.model.EmployeeAssignment;
import com.hrms.dao.EmployeeAssignmentDAO.DepartmentMemberOption;
import com.hrms.service.EmployeeAssignmentService;
import com.hrms.service.security.TenantPrincipal;
import com.hrms.util.SecuritySupport;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

public class EmployeeAssignmentController extends HttpServlet {
    private final EmployeeAssignmentService employeeAssignmentService = new EmployeeAssignmentService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        SecuritySupport.applyNoStore(response::setHeader);
        try {
            int companyId = SecuritySupport.requireCompanyId(request);
            requireOrganizationEditor(request);
            if ("/departments/assignment-options".equals(request.getServletPath())) {
                writeJson(response, HttpServletResponse.SC_OK, "{\"success\":true,\"data\":"
                        + com.hrms.util.Json.encode(employeeAssignmentService.getInitialAssignmentCandidates(companyId,
                        parseRequiredInt(request.getParameter("departmentId")))) + "}");
                return;
            }
            if ("/employees/eligible-managers".equals(request.getServletPath())) {
                writeJson(response, HttpServletResponse.SC_OK, "{\"success\":true,\"data\":"
                        + toJsonDepartmentMembers(employeeAssignmentService.getEligibleManagers(companyId,
                        parseNullableInt(request.getParameter("departmentId")), parseRequiredInt(request.getParameter("id")))) + "}");
                return;
            }
            if ("/employees/team-options".equals(request.getServletPath())) {
                writeJson(response, HttpServletResponse.SC_OK, "{\"success\":true,\"data\":"
                        + toJsonDepartmentMembers(employeeAssignmentService.getTeamOptions(companyId,
                        parseRequiredInt(request.getParameter("id")))) + "}");
                return;
            }
            if ("/employees/direct-reports".equals(request.getServletPath())) {
                writeJson(response, HttpServletResponse.SC_OK, "{\"success\":true,\"data\":"
                        + toJsonDepartmentMembers(employeeAssignmentService.getDirectReports(companyId,
                        parseRequiredInt(request.getParameter("id")))) + "}");
                return;
            }
            if ("/departments/members".equals(request.getServletPath())) {
                List<DepartmentMemberOption> employees = employeeAssignmentService.getDepartmentMemberOptions(companyId,
                        parseRequiredInt(request.getParameter("departmentId")));
                writeJson(response, HttpServletResponse.SC_OK,
                        "{\"success\":true,\"data\":" + toJsonDepartmentMembers(employees) + "}");
                return;
            }

            EmployeeAssignment assignment = employeeAssignmentService.getEmployeeOrganization(companyId,
                    parseRequiredInt(request.getParameter("id")));
            DepartmentMemberOption current = employeeAssignmentService.getMember(companyId, assignment.getEmployeeId());
            boolean managerValid = assignment.getManagerId() == null || employeeAssignmentService.getEligibleManagers(
                    companyId, assignment.getDepartmentId(), assignment.getEmployeeId()).stream()
                    .anyMatch(manager -> java.util.Objects.equals(manager.employeeId(), assignment.getManagerId()));
            String details = toJson(assignment);
            details = details.substring(0, details.length()-1) + ",\"canManageTeam\":" + current.canManageTeam()
                    + ",\"roleCode\":" + quote(current.roleCode())
                    + ",\"managerValid\":" + managerValid + ",\"managerName\":" + quote(current.managerName())
                    + ",\"departmentName\":" + quote(current.departmentName())
                    + ",\"positionTitle\":" + quote(current.positionTitle()) + "}";
            writeJson(response, HttpServletResponse.SC_OK, "{\"success\":true,\"data\":" + details + "}");
        } catch (SecurityException e) {
            writeJson(response, HttpServletResponse.SC_FORBIDDEN, error(e.getMessage()));
        } catch (SQLException e) {
            log("Employee assignment failed", e);
            writeJson(response, HttpServletResponse.SC_BAD_REQUEST, error("Không cập nhật được phân công. Vui lòng tải lại và kiểm tra các bản ghi đã chọn."));
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
            requireOrganizationEditor(request);
            if (!SecuritySupport.hasValidPublicCsrfToken(request)) throw new SecurityException("Phiên thao tác không hợp lệ. Vui lòng tải lại trang.");
            boolean success;
            String message;
            TenantPrincipal principal = SecuritySupport.getTenantPrincipal(request);

            if ("/departments/initial-assignment".equals(request.getServletPath())) {
                success = employeeAssignmentService.assignDepartmentEmployees(companyId,
                        parseIntArray(request.getParameter("employeeIds")), parseRequiredInt(request.getParameter("departmentId")),
                        parseRequiredInt(request.getParameter("positionId")), parseNullableInt(request.getParameter("managerId")), principal.getUserId());
                message = "Đã cập nhật phân công và vai trò theo chức danh.";
            } else if ("/employees/assign-organization".equals(request.getServletPath())) {
                int employeeId = parseRequiredInt(request.getParameter("employeeId"));
                EmployeeAssignment before = employeeAssignmentService.getEmployeeOrganization(companyId, employeeId);
                String positionId = request.getParameter("positionId");
                success = employeeAssignmentService.saveOrganization(companyId, employeeId,
                        parseRequiredInt(request.getParameter("departmentId")),
                        positionId == null || positionId.isBlank() ? null : parseRequiredInt(positionId),
                        resolveManager(request, before.getManagerId()),
                        principal.getUserId(), request.getParameter("reason"));
                message = "Đã cập nhật phân công tổ chức.";
            } else if ("/employees/manage-team".equals(request.getServletPath())) {
                int managerId = parseRequiredInt(request.getParameter("managerId"));
                int[] removals = parseIntArray(request.getParameter("removeEmployeeIds"));
                if (removals.length > 0 && !"true".equals(request.getParameter("confirmRemovals"))) {
                    throw new com.hrms.service.FieldValidationException("employeeIds", "Vui lòng xác nhận gỡ quản lý trực tiếp của những nhân viên đã chọn.");
                }
                success = employeeAssignmentService.manageTeam(companyId, managerId,
                        parseIntArray(request.getParameter("addEmployeeIds")), removals,
                        parseExpectedManagers(request.getParameter("expectedManagers")), "true".equals(request.getParameter("confirmTransfers")), principal.getUserId(),request.getParameter("reason"));
                message = "Đã cập nhật thành viên nhóm.";
            } else if ("/departments/assign-members".equals(request.getServletPath())) {
                int departmentId = parseRequiredInt(request.getParameter("departmentId"));
                int managerId = parseRequiredInt(request.getParameter("managerId"));
                int[] employeeIds = parseIntArray(request.getParameter("employeeIds"));
                success = employeeAssignmentService.assignDepartmentMembers(companyId,
                        departmentId,
                        managerId,
                        employeeIds,principal.getUserId());
                message = success ? "Đã phân công nhân viên vào phòng ban." : "Không có hồ sơ nhân viên nào được thay đổi.";
            } else if ("/employees/assign-department".equals(request.getServletPath())) {
                int employeeId = parseRequiredInt(request.getParameter("employeeId"));
                EmployeeAssignment current = employeeAssignmentService.getEmployeeOrganization(companyId, employeeId);
                int departmentId = parseRequiredInt(request.getParameter("departmentId"));
                Integer positionId = request.getParameter("positionId") == null ? current.getPositionId() : parseNullableInt(request.getParameter("positionId"));
                success = employeeAssignmentService.saveOrganization(companyId, employeeId, departmentId, positionId,
                        resolveManager(request,current.getManagerId()),principal.getUserId(),request.getParameter("reason"));
                message = "Đã cập nhật phòng ban, chức danh và vai trò theo phân công.";
            } else if ("/employees/assign-position".equals(request.getServletPath())) {
                int employeeId = parseRequiredInt(request.getParameter("employeeId"));
                EmployeeAssignment current = employeeAssignmentService.getEmployeeOrganization(companyId, employeeId);
                Integer positionId = parseNullableInt(request.getParameter("positionId"));
                if (positionId == null) throw new com.hrms.service.FieldValidationException("positionId","Vui lòng chọn chức danh thay thế; không xóa lịch sử phân công.");
                var position = new com.hrms.service.PositionService().getPositionDetail(companyId,positionId);
                if (position == null) throw new com.hrms.service.FieldValidationException("positionId","Không tìm thấy chức danh trong công ty.");
                success = employeeAssignmentService.saveOrganization(companyId, employeeId, position.getDepartmentId(), positionId,
                        resolveManager(request,current.getManagerId()),principal.getUserId(),request.getParameter("reason"));
                message = "Đã cập nhật chức danh và vai trò theo phân công.";
            } else if ("/employees/assign-manager".equals(request.getServletPath())) {
                int employeeId = parseRequiredInt(request.getParameter("employeeId"));
                EmployeeAssignment current = employeeAssignmentService.getEmployeeOrganization(companyId,employeeId);
                Integer managerId = resolveManager(request,current.getManagerId());
                success = employeeAssignmentService.saveOrganization(companyId, employeeId,current.getDepartmentId(),current.getPositionId(),
                        managerId,principal.getUserId(),request.getParameter("reason"));
                message = success ? "Đã phân công quản lý trực tiếp." : "Không có hồ sơ nhân viên nào được thay đổi.";
            } else {
                throw new IllegalArgumentException("Thao tác phân công không được hỗ trợ.");
            }

            writeJson(response, HttpServletResponse.SC_OK,
                    "{\"success\":" + success + ",\"message\":" + quote(message) + "}");
        } catch (SecurityException e) {
            writeJson(response, HttpServletResponse.SC_FORBIDDEN, error(e.getMessage()));
        } catch (SQLException e) {
            log("Employee assignment failed", e);
            writeJson(response, HttpServletResponse.SC_BAD_REQUEST, error("Không cập nhật được phân công. Vui lòng tải lại và kiểm tra các bản ghi đã chọn."));
        } catch (IllegalArgumentException e) {
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
                    .append("\"departmentName\":").append(quote(employee.departmentName())).append(",")
                    .append("\"managerName\":").append(quote(employee.managerName())).append(",")
                    .append("\"roleCode\":").append(quote(employee.roleCode())).append(",")
                    .append("\"selected\":").append(employee.selected())
                    .append(",\"canManageTeam\":").append(employee.canManageTeam())
                    .append(",\"assignable\":").append(employee.assignable())
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

    private void requireOrganizationEditor(HttpServletRequest request) {
        SecuritySupport.requireOrganizationEditor(request);
    }

    private Integer parseNullableInt(String value) {
        return value == null || value.isBlank() ? null : parseRequiredInt(value);
    }

    private Integer resolveManager(HttpServletRequest request,Integer existing) {
        String action=request.getParameter("managerAction");
        if (action==null) return request.getParameter("managerId")==null ? existing : parseNullableInt(request.getParameter("managerId"));
        return switch(action) {
            case "KEEP" -> existing;
            case "REMOVE" -> null;
            case "ASSIGN" -> parseRequiredInt(request.getParameter("managerId"));
            default -> throw new IllegalArgumentException("Vui lòng chọn giữ nguyên, gán mới hoặc gỡ quản lý trực tiếp.");
        };
    }

    private java.util.Map<Integer,Integer> parseExpectedManagers(String value) {
        java.util.Map<Integer,Integer> expected = new java.util.HashMap<>();
        if (value == null || value.isBlank()) return expected;
        for (String entry : value.split(",")) {
            String[] parts = entry.split(":", -1);
            if (parts.length != 2) throw new IllegalArgumentException("Danh sách nhóm đã thay đổi. Vui lòng tải lại trang.");
            expected.put(parseRequiredInt(parts[0]), "0".equals(parts[1]) ? null : parseRequiredInt(parts[1]));
        }
        return expected;
    }

    private String error(String message) {
        return "{\"success\":false,\"message\":" + quote(message) + "}";
    }

    private String quote(String value) {
        return ValidationSupport.quote(value);
    }

    private String nullableInteger(Integer value) {
        return value == null ? "null" : value.toString();
    }

    private int parseRequiredInt(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Vui lòng nhập giá trị số bắt buộc.");
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

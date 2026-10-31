package com.hrms.controller.hr;

import com.hrms.controller.ProfileUpdatePeriodController;

import com.hrms.model.Employee;
import com.hrms.model.ProfileUpdatePeriod;
import com.hrms.dao.AuditLogDAO;
import com.hrms.service.EmployeeHistoryService;
import com.hrms.service.security.TenantPrincipal;
import com.hrms.service.EmployeeService;
import com.hrms.service.FieldValidationException;
import com.hrms.service.ProfileUpdateService;
import com.hrms.util.SecuritySupport;
import com.hrms.util.ValidationSupport;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.Date;
import java.sql.SQLException;
import java.util.List;
import java.util.Set;

public class EmployeeController extends HttpServlet {
    private final EmployeeService employeeService = new EmployeeService();
    private final EmployeeHistoryService employeeHistoryService = new EmployeeHistoryService();
    private final ProfileUpdateService profileUpdateService;

    public EmployeeController() {
        this(new ProfileUpdateService());
    }

    public EmployeeController(ProfileUpdateService profileUpdateService) {
        this.profileUpdateService = profileUpdateService;
    }

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        SecuritySupport.applyNoStore(response::setHeader);
        try {
            int companyId = SecuritySupport.requireCompanyId(request);
            if (!request.getServletPath().startsWith("/employees/me")) SecuritySupport.requireOrganizationEditor(request);
            requireEmployeeScope(request, companyId);
            if (Set.of("/employees/me/organization-chart","/employees/me/colleague","/employees/me/contracts").contains(request.getServletPath())) {
                String resource=request.getServletPath().substring("/employees/me/".length());
                Integer target="colleague".equals(resource)?parseRequiredInt(request.getParameter("employeeId")):null;
                writeJson(response,HttpServletResponse.SC_OK,"{\"success\":true,\"data\":"
                        +com.hrms.util.Json.encode(new com.hrms.service.EmployeeLifecycleService().personalView(
                        SecuritySupport.getTenantPrincipal(request),resource,target))+"}");
                return;
            }
            if ("/employees/me/team-member".equals(request.getServletPath())) {
                writeJson(response,HttpServletResponse.SC_OK,"{\"success\":true,\"data\":"
                        +com.hrms.util.Json.encode(new com.hrms.service.EmployeeLifecycleService().teamMember(
                        SecuritySupport.getTenantPrincipal(request),parseRequiredInt(request.getParameter("employeeId"))))+"}");
                return;
            }
            if ("/employees/career-history".equals(request.getServletPath())) {
                SecuritySupport.requireOrganizationEditor(request);
                int employeeId=parseRequiredInt(request.getParameter("id"));
                writeJson(response,HttpServletResponse.SC_OK,"{\"success\":true,\"data\":"
                        + toCareerHistoryJson(employeeHistoryService.getCareerHistory(companyId,employeeId)) + "}");
                return;
            }
            if ("/employees/me".equals(request.getServletPath())) {
                TenantPrincipal principal = SecuritySupport.getTenantPrincipal(request);
                if (principal == null || principal.getEmployeeId() == null) {
                    writeJson(response, HttpServletResponse.SC_UNAUTHORIZED, error("Vui lòng đăng nhập bằng tài khoản nhân viên."));
                    return;
                }
                Employee employee = employeeService.getEmployeeById(companyId, principal.getEmployeeId());
                if (employee == null) {
                    writeJson(response, HttpServletResponse.SC_NOT_FOUND, error("Không tìm thấy nhân viên."));
                    return;
                }
                String personalJson = toJson(employee, principal.getRoleCode(),
                        profileUpdateService.getAvailablePeriod(principal), new ProfileUpdateService("DOCUMENT").getAvailablePeriod(principal),
                        SecuritySupport.ensurePublicCsrfToken(request.getSession(true)));
                writeJson(response, HttpServletResponse.SC_OK,
                        "{\"success\":true,\"data\":" + personalJson.substring(0,personalJson.length()-1)
                                + ",\"compensation\":" + com.hrms.util.Json.encode(new com.hrms.service.EmployeeLifecycleService().ownCompensation(principal))
                                + ",\"workspace\":" + com.hrms.util.Json.encode(new com.hrms.service.EmployeeLifecycleService().ownWorkspace(principal)) + "}}");
                return;
            }

            if ("/employees/me/history".equals(request.getServletPath())) {
                TenantPrincipal principal = SecuritySupport.getTenantPrincipal(request);
                if (principal == null || principal.getEmployeeId() == null) {
                    writeJson(response, HttpServletResponse.SC_UNAUTHORIZED, error("Vui lòng đăng nhập bằng tài khoản nhân viên."));
                    return;
                }
                writeJson(response, HttpServletResponse.SC_OK,
                        "{\"success\":true,\"data\":" + toHistoryJson(employeeHistoryService.getEmployeeHistory(principal.getEmployeeId())) + "}");
                return;
            }

            if ("/employees/detail".equals(request.getServletPath())) {
                Employee employee = employeeService.getEmployeeById(companyId, parseRequiredInt(request.getParameter("id")));
                if (employee == null) {
                    writeJson(response, HttpServletResponse.SC_NOT_FOUND, error("Không tìm thấy nhân viên."));
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

            List<Employee> employees = employeeService.getAllEmployees(companyId);
            writeJson(response, HttpServletResponse.SC_OK, "{\"success\":true,\"data\":" + toJsonArray(employees) + "}");
        } catch (SecurityException e) {
            writeJson(response, HttpServletResponse.SC_FORBIDDEN, error(e.getMessage()));
        } catch (SQLException e) {
            log("Employee database operation failed", e);
            writeJson(response, HttpServletResponse.SC_BAD_REQUEST, error("Không xử lý được dữ liệu nhân viên. Vui lòng kiểm tra các bản ghi đã chọn."));
        } catch (IllegalArgumentException e) {
            writeJson(response, HttpServletResponse.SC_BAD_REQUEST, error(e.getMessage()));
        }
    }

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        SecuritySupport.applyNoStore(response::setHeader);
        try {
            int companyId = SecuritySupport.requireCompanyId(request);
            if (!request.getServletPath().startsWith("/employees/me")) SecuritySupport.requireOrganizationEditor(request);
            requireEmployeeScope(request, companyId);
            if (!SecuritySupport.hasValidPublicCsrfToken(request)) throw new SecurityException("Phiên thao tác không hợp lệ. Vui lòng tải lại trang.");
            boolean success;
            Employee savedEmployee = null;
            String message;
            if ("/employees/deactivate".equals(request.getServletPath()) || "/employees/activate".equals(request.getServletPath())) {
                int employeeId = parseRequiredInt(request.getParameter("employeeId"));
                boolean active = "/employees/activate".equals(request.getServletPath());
                if (!active) {
                    new com.hrms.service.EmployeeLifecycleService().terminate(SecuritySupport.getTenantPrincipal(request), employeeId, request.getParameter("reason"));
                    success = true;
                } else {
                    success = employeeService.activateEmployee(companyId, employeeId, SecuritySupport.getTenantPrincipal(request).getUserId());
                }
                message = active ? "Đã kích hoạt hồ sơ nhân viên." : "Đã ngừng hoạt động hồ sơ nhân viên.";
            } else if ("/employees/me/update-profile".equals(request.getServletPath())) {
                TenantPrincipal principal = SecuritySupport.getTenantPrincipal(request);
                if (principal == null || principal.getEmployeeId() == null) {
                    writeJson(response, HttpServletResponse.SC_UNAUTHORIZED, error("Vui lòng đăng nhập bằng tài khoản nhân viên."));
                    return;
                }
                if (!SecuritySupport.hasValidPublicCsrfToken(request)) {
                    throw new SecurityException("Phiên thao tác không hợp lệ. Vui lòng tải lại trang.");
                }
                java.util.Map<String,String> changes=new java.util.LinkedHashMap<>();
                for (String field:request.getParameterMap().keySet()) {
                    if (Set.of("csrfToken","updatedAt").contains(field)) continue;
                    if (!ProfileUpdateService.UPDATABLE_FIELDS.contains(field)) throw new FieldValidationException("phone","Không được cập nhật trường thông tin này trong đợt tự cập nhật hồ sơ.");
                    if (request.getParameterMap().get(field).length!=1) throw new FieldValidationException(field,"Mỗi trường chỉ được gửi một giá trị.");
                    changes.put(field,request.getParameter(field));
                }
                profileUpdateService.updateProfile(principal, changes, request.getParameter("updatedAt"), request.getRemoteAddr());
                success = true;
                message = "Đã cập nhật thông tin cá nhân.";
            } else if ("/employees/update".equals(request.getServletPath())) {
                TenantPrincipal principal = SecuritySupport.getTenantPrincipal(request);
                Employee updatedEmployee = readEmployee(request);
                success = employeeService.updateEmployee(updatedEmployee, principal.getUserId());
                if (success) savedEmployee = employeeService.getEmployeeById(companyId, updatedEmployee.getEmployeeId());
                message = success ? "Đã cập nhật hồ sơ nhân viên." : "Không có hồ sơ nhân viên nào được thay đổi.";
            } else if ("/employees/create".equals(request.getServletPath()) || "/employees".equals(request.getServletPath())) {
                TenantPrincipal principal = SecuritySupport.getTenantPrincipal(request);
                if (principal == null || principal.getCompanyId() == null) {
                    writeJson(response, HttpServletResponse.SC_UNAUTHORIZED, error("Vui lòng đăng nhập vào workspace."));
                    return;
                }
                Employee employee = readEmployee(request);
                for (String field : java.util.List.of("grossSalary", "travelAllowance", "positionAllowance")) {
                    String value = request.getParameter(field);
                    if (value != null && !value.isBlank())
                        throw new FieldValidationException(field, "Lương và phụ cấp được quản lý trong hợp đồng; không nhập khi tạo hồ sơ nhân viên.");
                }
                if (request.getParameter("managerId") != null && !request.getParameter("managerId").isBlank())
                    throw new FieldValidationException("managerId", "Vui lòng phân công quản lý sau tại phần Tổ chức trong hồ sơ nhân viên.");
                success = employeeService.createEmployeeProfile(employee, principal.getCompanyId(),
                        request.getParameter("employeeCodeMode"), principal.getUserId());
                if (success) savedEmployee = employeeService.getEmployeeById(companyId, employee.getEmployeeId());
                message = success
                        ? "Đã tạo hồ sơ nhân viên."
                        : "Không có hồ sơ nhân viên nào được thay đổi.";
            } else {
                response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
                return;
            }

            writeJson(response, HttpServletResponse.SC_OK,
                    "{\"success\":" + success + ",\"message\":" + quote(message)
                            + ",\"data\":" + (savedEmployee == null ? "null" : toJson(savedEmployee)) + "}");
        } catch (SecurityException exception) {
            writeJson(response, HttpServletResponse.SC_FORBIDDEN, error(exception.getMessage()));
        } catch (FieldValidationException exception) {
            writeJson(response, HttpServletResponse.SC_BAD_REQUEST,
                    "{\"success\":false,\"message\":" + quote(exception.getMessage())
                            + ",\"fieldErrors\":{" + quote(exception.getField()) + ":"
                            + quote(exception.getMessage()) + "}}");
        } catch (SQLException e) {
            log("Employee database operation failed", e);
            writeJson(response, HttpServletResponse.SC_BAD_REQUEST, error("Không xử lý được dữ liệu nhân viên."));
        } catch (IllegalArgumentException e) {
            writeJson(response, HttpServletResponse.SC_BAD_REQUEST, error(e.getMessage()));
        }
    }

    private Integer optionalId(String value) {
        return value == null || value.isBlank() ? null : Integer.valueOf(value);
    }

    private void requireEmployeeScope(HttpServletRequest request, int companyId) throws SQLException {
        if (request.getServletPath().startsWith("/employees/me")) return;
        String employeeId = request.getParameter("employeeId");
        String detailId = request.getParameter("id");
        for (String value : new String[] {employeeId, detailId}) {
            if (value == null || value.isBlank()) continue;
            Employee employee = employeeService.getEmployeeById(companyId, parseRequiredInt(value));
            if (employee == null || !Integer.valueOf(companyId).equals(employee.getCompanyId())) {
                throw new SecurityException("Không tìm thấy nhân viên trong công ty của bạn.");
            }
        }
    }

    private Employee readEmployee(HttpServletRequest request) {
        Employee employee = new Employee();
        employee.setCompanyId(SecuritySupport.requireCompanyId(request));
        employee.setDepartmentId(optionalId(request.getParameter("departmentId")));
        employee.setPositionId(optionalId(request.getParameter("positionId")));
        employee.setEmployeeId(parseOptionalInt(request.getParameter("employeeId")));
        employee.setEmployeeCode(trimToNull(request.getParameter("employeeCode")));
        employee.setFullName(trimToNull(request.getParameter("fullName")));
        employee.setPhone(trimToNull(request.getParameter("phone")));
        employee.setGender(trimToNull(request.getParameter("gender")));
        employee.setDateOfBirth(parseDate(request.getParameter("dateOfBirth"),"dateOfBirth"));
        employee.setIdentityNumber(trimToNull(request.getParameter("identityNumber")));
        employee.setStatus(trimToNull(request.getParameter("status")));
        employee.setManagerId(optionalId(request.getParameter("managerId")));
        employee.setEmail(trimToNull(request.getParameter("email")));
        employee.setIdentityIssuedDate(parseDate(request.getParameter("identityIssuedDate"),"identityIssuedDate"));
        employee.setIdentityIssuedPlace(trimToNull(request.getParameter("identityIssuedPlace")));
        employee.setPermanentAddress(trimToNull(request.getParameter("permanentAddress")));
        employee.setHometown(trimToNull(request.getParameter("hometown")));
        employee.setMajor(trimToNull(request.getParameter("major")));
        if (trimToNull(request.getParameter("intendedContractType")) != null)
            throw new FieldValidationException("intendedContractType", "Vui lòng chọn loại hợp đồng khi tạo hoặc ghi nhận hợp đồng, không nhập trong hồ sơ nhân viên.");
        employee.setUpdatedAt(request.getParameter("updatedAt"));
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
                + "\"email\":" + quote(employee.getEmail()) + ","
                + "\"identityIssuedDate\":" + quoteDate(employee.getIdentityIssuedDate()) + ","
                + "\"identityIssuedPlace\":" + quote(employee.getIdentityIssuedPlace()) + ","
                + "\"permanentAddress\":" + quote(employee.getPermanentAddress()) + ","
                + "\"hometown\":" + quote(employee.getHometown()) + ","
                + "\"major\":" + quote(employee.getMajor()) + ","
                + "\"contractType\":" + quote(employee.getContractType()) + ","
                + "\"contractCode\":" + quote(employee.getContractCode()) + ","
                + "\"contractStatus\":" + quote(employee.getContractStatus()) + ","
                + "\"departmentName\":" + quote(employee.getDepartmentName()) + ","
                + "\"positionTitle\":" + quote(employee.getPositionTitle()) + ","
                + "\"managerId\":" + nullableInteger(employee.getManagerId()) + ","
                + "\"managerName\":" + quote(employee.getManagerName()) + ","
                + "\"updatedAt\":" + quote(employee.getUpdatedAt()) + ","
                + "\"employeeId\":" + employee.getEmployeeId() + ","
                + "\"departmentId\":" + nullableInteger(employee.getDepartmentId()) + ","
                + "\"positionId\":" + nullableInteger(employee.getPositionId()) + ","
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

    private String toJson(Employee employee, String roleCode, ProfileUpdatePeriod period, ProfileUpdatePeriod documentPeriod, String csrfToken) {
        String json = toJson(employee);
        return json.substring(0, json.length() - 1)
                + ",\"roleCode\":" + quote(roleCode)
                + ",\"canUpdatePhone\":" + (period != null)
                + ",\"canUpdateProfile\":" + (period != null)
                + ",\"profileUpdatePeriod\":" + ProfileUpdatePeriodController.toJson(period)
                + ",\"canUpdateDocuments\":" + (documentPeriod != null)
                + ",\"documentUpdatePeriod\":" + ProfileUpdatePeriodController.toJson(documentPeriod)
                + ",\"csrfToken\":" + quote(csrfToken)
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

    private String toCareerHistoryJson(List<com.hrms.model.EmployeeCareerEvent> history) {
        return history.stream().map(event->"{\"eventId\":"+quote(event.eventId())
                +",\"eventType\":"+quote(event.eventType())+",\"recordedAt\":"+quote(event.recordedAt())
                +",\"effectiveDate\":"+quote(event.effectiveDate())+",\"status\":"+quote(event.status())
                +",\"oldValue\":"+quote(event.oldValue())+",\"newValue\":"+quote(event.newValue())
                +",\"reason\":"+quote(event.reason())+",\"changedBy\":"+quote(event.changedBy())+"}")
                .collect(java.util.stream.Collectors.joining(",","[","]"));
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    private String error(String message) {
        return "{\"success\":false,\"message\":" + quote(message) + "}";
    }

    private String quote(String value) {
        return ValidationSupport.quote(value);
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
            throw new IllegalArgumentException("Vui lòng nhập giá trị số bắt buộc.");
        }
        return Integer.parseInt(value);
    }

    private int parseOptionalInt(String value) {
        return value == null || value.isBlank() ? 0 : Integer.parseInt(value);
    }

    private Date parseDate(String value,String field) {
        try {
            return value == null || value.isBlank() ? null : Date.valueOf(java.time.LocalDate.parse(value));
        } catch (java.time.format.DateTimeParseException exception) {
            throw new FieldValidationException(field, "Ngày không hợp lệ. Sử dụng định dạng yyyy-MM-dd.");
        }
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}

package com.hrms.controller.companyadmin;

import com.hrms.model.EmploymentContract;
import com.hrms.service.companyadmin.EmployeeLifecycleService;
import com.hrms.util.Json;
import com.hrms.util.SecuritySupport;
import com.hrms.util.ValidationSupport;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Map;
import java.util.Set;

public class EmployeeLifecycleController extends HttpServlet {
    private final EmployeeLifecycleService service = new EmployeeLifecycleService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {
        SecuritySupport.applyNoStore(response::setHeader);
        try {
            var principal = SecuritySupport.getTenantPrincipal(request);
            service.requireEditor(principal);
            String resource = request.getServletPath().substring(request.getServletPath().lastIndexOf('/') + 1);
            if (!request.getServletPath().startsWith("/lifecycle/api/")) {
                if (!Set.of("contracts", "accounts", "organization", "dashboard").contains(resource)) {
                    response.sendError(404);
                    return;
                }
                request.setAttribute("lifecycleResource", resource);
                request.setAttribute("organizationCsrfToken",
                        SecuritySupport.ensurePublicCsrfToken(request.getSession()));
                request.getRequestDispatcher("/WEB-INF/views/comadmin-hr/lifecycle.jsp").forward(request, response);
                return;
            }
            Object data = "allowance-preview".equals(resource) ? service.previewContractAllowances(principal,requiredInt(request.getParameter("employeeId")),date(request.getParameter("startDate")))
                    : "allowance-types".equals(resource) ? service.allowanceTypes(principal)
                    : "dashboard".equals(resource) ? service.dashboard(principal)
                    : service.read(principal, resource, optionalInt(request.getParameter("employeeId")));
            write(response, 200, Map.of("success", true, "data", data));
        } catch (SecurityException exception) {
            write(response, 403, Map.of("success", false, "message", exception.getMessage()));
        } catch (IllegalArgumentException exception) {
            write(response, 400, Map.of("success", false, "message", exception.getMessage()));
        } catch (SQLException exception) {
            log("Lifecycle read failed", exception);
            write(response, 500, Map.of("success", false, "message",
                    "Không tải được dữ liệu nhân sự. Vui lòng kiểm tra các bản nâng cấp cơ sở dữ liệu."));
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        SecuritySupport.applyNoStore(response::setHeader);
        try {
            var principal = SecuritySupport.getTenantPrincipal(request);
            service.requireEditor(principal);
            if (!SecuritySupport.hasValidPublicCsrfToken(request))
                throw new SecurityException("Phiên thao tác không hợp lệ. Vui lòng tải lại trang.");
            String path = request.getServletPath();
            Object data;
            if (path.equals("/lifecycle/api/contracts/save")) {
                String id = request.getParameter("contractId");
                var contract = new EmploymentContract(id == null || id.isBlank() ? null : Long.valueOf(id),
                        requiredInt(request.getParameter("employeeId")), request.getParameter("contractCode"),
                        request.getParameter("contractType"),
                        date(request.getParameter("startDate")), "true".equals(request.getParameter("durationBased"))
                                ? com.hrms.service.companyadmin.ContractDuration.endDate(request.getParameter("contractType"),
                                        date(request.getParameter("startDate")), request.getParameter("duration"), request.getParameter("durationUnit"))
                                : date(request.getParameter("endDate")),
                        decimal(request.getParameter("grossSalary")),
                        request.getParameter("notes"), request.getParameter("version"),
                        null, null);
                String mode = request.getParameter("creationMode");
                if ("SIGNED_EXISTING".equals(mode)) {
                    String salary = request.getParameter("synchronizeSalary");
                    if (salary != null && !Set.of("true", "false").contains(salary))
                        throw new IllegalArgumentException("Lựa chọn đồng bộ lương không hợp lệ.");
                    data = saveWithHardCopy(request,principal,contract,date(request.getParameter("signedDate")),"true".equals(salary),true);
                } else if (mode == null || "DRAFT".equals(mode)) {
                    data = saveWithHardCopy(request,principal,contract,null,false,false);
                } else
                    throw new IllegalArgumentException("Vui lòng chọn cách ghi nhận hợp đồng hợp lệ.");
            } else if (path.equals("/lifecycle/api/contracts/hard-copy")) {
                long contractId=Long.parseLong(request.getParameter("contractId"));
                var contract=service.missingContractHardCopyTarget(principal,contractId);
                var part=request.getContentType()!=null && request.getContentType().toLowerCase(java.util.Locale.ROOT).startsWith("multipart/form-data")
                        ? request.getPart("contractFile") : null;
                try (var copy=com.hrms.service.companyadmin.ContractHardCopy.receive(part,((Number)contract.get("EmployeeId")).intValue(),getServletContext())) {
                    if (copy==null) throw new com.hrms.service.FieldValidationException("contractFile","Vui lòng chọn hợp đồng bản cứng.");
                    service.addContractHardCopy(principal,contractId,copy);
                    copy.retain();
                }
                data="Đã bổ sung hợp đồng bản cứng.";
            } else if (path.equals("/lifecycle/api/allowance-types/save")) {
                String active = request.getParameter("active");
                if (!Set.of("true","false").contains(active == null ? "" : active))
                    throw new IllegalArgumentException("Vui lòng chọn trạng thái loại phụ cấp.");
                data = service.saveAllowanceType(principal, optionalInt(request.getParameter("allowanceTypeId")),
                        request.getParameter("typeName"), readLevelAmounts(request), "true".equals(active));
            } else if (path.equals("/lifecycle/api/contracts/transition")) {
                service.transitionContract(principal, Long.parseLong(request.getParameter("contractId")),
                        request.getParameter("action"),
                        request.getParameter("version"), request.getParameter("reason"));
                data = "Đã cập nhật hợp đồng.";
            } else if (path.equals("/lifecycle/api/accounts/action")) {
                String password = service.account(principal, requiredInt(request.getParameter("employeeId")),
                        request.getParameter("action"),
                        request.getParameter("email"), request.getParameter("roleCode"));
                data = "create".equals(request.getParameter("action"))
                        ? Map.of("message",
                                "Đã tạo tài khoản. Mật khẩu ban đầu là mã định danh cá nhân; bắt buộc đổi ở lần đăng nhập đầu.",
                                "passwordPolicy", "PERSONAL_ID_FIRST_LOGIN")
                        : password == null ? Map.of("message", "Đã cập nhật tài khoản nhân viên.")
                                : Map.of("temporaryPassword", password);
            } else if (path.equals("/lifecycle/api/terminate")) {
                data = service.terminate(principal, requiredInt(request.getParameter("employeeId")),
                        request.getParameter("reason"));
            } else {
                response.sendError(404);
                return;
            }
            write(response, 200, Map.of("success", true, "data", data));
        } catch (SecurityException exception) {
            write(response, 403, Map.of("success", false, "message", exception.getMessage()));
        } catch (jakarta.servlet.ServletException | IllegalStateException exception) {
            write(response,400,Map.of("success",false,"message","Tệp tải lên không hợp lệ hoặc vượt quá 10 MB."));
        } catch (IOException exception) {
            log("Contract hard-copy upload failed",exception);
            write(response,500,Map.of("success",false,"message","Không lưu được tệp hợp đồng. Vui lòng thử lại."));
        } catch (IllegalArgumentException exception) {
            response.setStatus(400);
            response.setContentType("application/json; charset=UTF-8");
            response.getWriter().write(ValidationSupport.errorJson(exception));
        } catch (SQLException exception) {
            log("Lifecycle write failed", exception);
            String message = exception.getErrorCode() == 51162
                    ? com.hrms.dao.companyadmin.ActiveAccountQuotaDAO.LIMIT_MESSAGE
                    : "Không lưu được dữ liệu. Vui lòng kiểm tra mã/email trùng, bản ghi đã chọn và thay đổi đồng thời.";
            write(response, 409, Map.of("success", false, "message", message));
        }
    }

    private long saveWithHardCopy(HttpServletRequest request,com.hrms.service.security.TenantPrincipal principal,
            EmploymentContract contract,LocalDate signedDate,boolean synchronizeSalary,boolean signedExisting)
            throws SQLException,IOException,jakarta.servlet.ServletException {
        if (new com.hrms.dao.companyadmin.EmployeeDAO().getEmployeeById(principal.getCompanyId(),contract.employeeId())==null)
            throw new SecurityException("Không tìm thấy nhân viên trong công ty của bạn.");
        var part=request.getContentType()!=null && request.getContentType().toLowerCase(java.util.Locale.ROOT).startsWith("multipart/form-data")
                ? request.getPart("contractFile") : null;
        try (var copy=com.hrms.service.companyadmin.ContractHardCopy.receive(part,contract.employeeId(),getServletContext())) {
            long id=signedExisting ? service.recordSignedContract(principal,contract,signedDate,synchronizeSalary,copy)
                    : service.saveContract(principal,contract,copy);
            if (copy!=null) copy.retain();
            return id;
        }
    }

    static java.util.Map<Integer,BigDecimal> readLevelAmounts(HttpServletRequest request) {
        String[] ids=request.getParameterValues("responsibilityLevelId");
        String[] amounts=request.getParameterValues("levelAmount");
        if (ids==null && amounts==null) return java.util.Map.of();
        if (ids==null || amounts==null || ids.length!=amounts.length || ids.length>100)
            throw new IllegalArgumentException("Danh sách mức phụ cấp theo cấp trách nhiệm không hợp lệ.");
        var result=new java.util.LinkedHashMap<Integer,BigDecimal>();
        for (int i=0;i<ids.length;i++) {
            int id=requiredInt(ids[i]);
            if (result.containsKey(id)) throw new IllegalArgumentException("Cấp trách nhiệm bị trùng.");
            BigDecimal amount=decimal(amounts[i]);
            if (amount==null) throw new IllegalArgumentException("Nhập mức phụ cấp cho từng cấp trách nhiệm.");
            com.hrms.dao.companyadmin.OrganizationAllowanceDAO.validate(amount,"levelAmount");
            result.put(id,amount);
        }
        return result;
    }

    private static Integer optionalInt(String value) {
        return value == null || value.isBlank() ? null : requiredInt(value);
    }

    private static int requiredInt(String value) {
        if (value == null || !value.matches("[1-9][0-9]*"))
            throw new IllegalArgumentException("Vui lòng chọn bản ghi hợp lệ.");
        return Integer.parseInt(value);
    }

    private static LocalDate date(String value) {
        try {
            return value == null || value.isBlank() ? null : LocalDate.parse(value);
        } catch (java.time.format.DateTimeParseException exception) {
            throw new IllegalArgumentException("Nhập ngày hợp lệ theo định dạng năm-tháng-ngày.");
        }
    }

    private static BigDecimal decimal(String value) {
        return value == null || value.isBlank() ? null : new BigDecimal(value);
    }

    private static void write(HttpServletResponse response, int status, Object value) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json; charset=UTF-8");
        response.getWriter().write(Json.encode(value));
    }
}

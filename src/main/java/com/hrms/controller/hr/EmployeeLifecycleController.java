package com.hrms.controller.hr;

import com.hrms.model.EmploymentContract;
import com.hrms.service.EmployeeLifecycleService;
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
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        SecuritySupport.applyNoStore(response::setHeader);
        try {
            var principal = SecuritySupport.getTenantPrincipal(request);
            service.requireEditor(principal);
            String resource = request.getServletPath().substring(request.getServletPath().lastIndexOf('/')+1);
            if (!request.getServletPath().startsWith("/lifecycle/api/")) {
                if (!Set.of("contracts", "accounts", "organization", "dashboard").contains(resource)) {
                    response.sendError(404);
                    return;
                }
                request.setAttribute("lifecycleResource", resource);
                request.setAttribute("organizationCsrfToken", SecuritySupport.ensurePublicCsrfToken(request.getSession()));
                request.getRequestDispatcher("/WEB-INF/views/comadmin-hr/lifecycle.jsp").forward(request, response);
                return;
            }
            Object data = "dashboard".equals(resource) ? service.dashboard(principal)
                    : service.read(principal, resource, optionalInt(request.getParameter("employeeId")));
            write(response, 200, Map.of("success", true, "data", data));
        } catch (SecurityException exception) {
            write(response, 403, Map.of("success", false, "message", exception.getMessage()));
        } catch (IllegalArgumentException exception) {
            write(response, 400, Map.of("success", false, "message", exception.getMessage()));
        } catch (SQLException exception) {
            log("Lifecycle read failed", exception);
            write(response, 500, Map.of("success", false, "message", "Không tải được dữ liệu nhân sự. Vui lòng kiểm tra các bản nâng cấp cơ sở dữ liệu."));
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        SecuritySupport.applyNoStore(response::setHeader);
        try {
            var principal = SecuritySupport.getTenantPrincipal(request);
            service.requireEditor(principal);
            if (!SecuritySupport.hasValidPublicCsrfToken(request)) throw new SecurityException("Phiên thao tác không hợp lệ. Vui lòng tải lại trang.");
            String path = request.getServletPath();
            Object data;
            if (path.equals("/lifecycle/api/contracts/save")) {
                String id = request.getParameter("contractId");
                var contract = new EmploymentContract(id == null || id.isBlank() ? null : Long.valueOf(id),
                        requiredInt(request.getParameter("employeeId")), request.getParameter("contractCode"), request.getParameter("contractType"),
                        date(request.getParameter("startDate")), date(request.getParameter("endDate")), decimal(request.getParameter("grossSalary")),
                        request.getParameter("notes"), request.getParameter("version"), decimal(request.getParameter("travelAllowance")));
                String mode = request.getParameter("creationMode");
                if ("SIGNED_EXISTING".equals(mode)) {
                    String salary = request.getParameter("synchronizeSalary");
                    if (salary != null && !Set.of("true", "false").contains(salary))
                        throw new IllegalArgumentException("Lựa chọn đồng bộ lương không hợp lệ.");
                    data = service.recordSignedContract(principal, contract, date(request.getParameter("signedDate")), "true".equals(salary));
                } else if (mode == null || "DRAFT".equals(mode)) {
                    data = service.saveContract(principal, contract);
                } else throw new IllegalArgumentException("Vui lòng chọn cách ghi nhận hợp đồng hợp lệ.");
            } else if (path.equals("/lifecycle/api/contracts/transition")) {
                service.transitionContract(principal, Long.parseLong(request.getParameter("contractId")), request.getParameter("action"),
                        request.getParameter("version"), request.getParameter("reason"));
                data = "Đã cập nhật hợp đồng.";
            } else if (path.equals("/lifecycle/api/accounts/action")) {
                String password = service.account(principal, requiredInt(request.getParameter("employeeId")), request.getParameter("action"),
                        request.getParameter("email"), request.getParameter("roleCode"));
                data = "create".equals(request.getParameter("action"))
                        ? Map.of("message","Đã tạo tài khoản. Mật khẩu ban đầu là mã định danh cá nhân; bắt buộc đổi ở lần đăng nhập đầu.","passwordPolicy","PERSONAL_ID_FIRST_LOGIN")
                        : password == null ? Map.of("message", "Đã cập nhật tài khoản nhân viên.") : Map.of("temporaryPassword", password);
            } else if (path.equals("/lifecycle/api/changes/request") || path.equals("/lifecycle/api/changes/transition")) {
                service.requireEditor(principal);
                write(response, HttpServletResponse.SC_GONE, Map.of("success",false,"message",
                        "Đã ngừng quy trình đề xuất. Vui lòng phân công hoặc chuyển chức danh trực tiếp tại Phòng ban."));
                return;
            } else if (path.equals("/lifecycle/api/compensation/save")) {
                service.adjustCompensation(principal, requiredInt(request.getParameter("employeeId")),date(request.getParameter("effectiveDate")),
                        new com.hrms.model.EmployeeCompensation(decimal(request.getParameter("grossSalary")),
                                null,decimal(request.getParameter("travelAllowance"))),request.getParameter("reason"));
                data = "Đã cập nhật lịch sử lương.";
            } else if (path.equals("/lifecycle/api/terminate")) {
                service.terminate(principal, requiredInt(request.getParameter("employeeId")), request.getParameter("reason"));
                data = "Đã ghi nhận ngừng làm việc. Lịch sử nhân viên được giữ nguyên.";
            } else {
                response.sendError(404);
                return;
            }
            write(response, 200, Map.of("success", true, "data", data));
        } catch (SecurityException exception) {
            write(response, 403, Map.of("success", false, "message", exception.getMessage()));
        } catch (IllegalArgumentException exception) {
            response.setStatus(400);
            response.setContentType("application/json; charset=UTF-8");
            response.getWriter().write(ValidationSupport.errorJson(exception));
        } catch (SQLException exception) {
            log("Lifecycle write failed", exception);
            write(response, 409, Map.of("success", false, "message", "Không lưu được dữ liệu. Vui lòng kiểm tra mã/email trùng, bản ghi đã chọn và thay đổi đồng thời."));
        }
    }

    private static Integer optionalInt(String value) { return value == null || value.isBlank() ? null : requiredInt(value); }
    private static int requiredInt(String value) {
        if (value == null || !value.matches("[1-9][0-9]*")) throw new IllegalArgumentException("Vui lòng chọn bản ghi hợp lệ.");
        return Integer.parseInt(value);
    }
    private static LocalDate date(String value) {
        try { return value == null || value.isBlank() ? null : LocalDate.parse(value); }
        catch (java.time.format.DateTimeParseException exception) { throw new IllegalArgumentException("Nhập ngày hợp lệ theo định dạng năm-tháng-ngày."); }
    }
    private static BigDecimal decimal(String value) { return value == null || value.isBlank() ? null : new BigDecimal(value); }
    private static void write(HttpServletResponse response, int status, Object value) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json; charset=UTF-8");
        response.getWriter().write(Json.encode(value));
    }
}

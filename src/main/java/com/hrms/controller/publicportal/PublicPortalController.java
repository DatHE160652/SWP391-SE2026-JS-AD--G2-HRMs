package com.hrms.controller.publicportal;

import com.hrms.api.ApiRoutes;
import com.hrms.service.security.TenantPrincipal;
import com.hrms.model.publicportal.ConsultationSubmission;
import com.hrms.model.publicportal.FaqTopic;
import com.hrms.service.TenantAuthService;
import com.hrms.service.BCryptPasswordHasher;
import com.hrms.service.EmployeeHistoryService;
import com.hrms.service.FieldValidationException;
import com.hrms.service.publicportal.ConsultationRequestService;
import com.hrms.service.publicportal.FaqService;
import com.hrms.util.SecuritySupport;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Public portal, tenant login, consultation, and FAQ endpoints. */
public class PublicPortalController extends HttpServlet {
    private static final String FLASH_SUCCESS = "publicConsultationSuccess";
    private static final String FLASH_EMAIL_WARNING = "publicConsultationEmailWarning";
    private final ConsultationRequestService consultationService;
    private final FaqService faqService;
    private final TenantAuthService tenantAuthService;
    private final BCryptPasswordHasher passwordHasher;
    private final EmployeeHistoryService employeeHistoryService = new EmployeeHistoryService();

    public PublicPortalController() {
        this(new ConsultationRequestService(), new FaqService(), new TenantAuthService(), new BCryptPasswordHasher());
    }

    PublicPortalController(ConsultationRequestService consultationService, FaqService faqService,
                           TenantAuthService tenantAuthService, BCryptPasswordHasher passwordHasher) {
        this.consultationService = consultationService;
        this.faqService = faqService;
        this.tenantAuthService = tenantAuthService;
        this.passwordHasher = passwordHasher;
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        SecuritySupport.applyNoStore(response::setHeader);
        String path = request.getServletPath();
        if (path == null || path.isEmpty()) path = ApiRoutes.PUBLIC_HOME;
        switch (path) {
            case ApiRoutes.PUBLIC_HOME -> render(request, response, "home.jsp");
            case ApiRoutes.PUBLIC_FEATURES -> render(request, response, "features-solutions.jsp");
            case ApiRoutes.PUBLIC_PRICING -> render(request, response, "pricing.jsp");
            case ApiRoutes.PUBLIC_FAQ -> showFaq(request, response);
            case ApiRoutes.PUBLIC_CONSULTATION -> showConsultation(request, response);
            case ApiRoutes.TENANT_LOGIN -> render(request, response, "tenant-login.jsp");
            case ApiRoutes.TENANT_LOGOUT -> logoutTenant(request, response);
            case "/change-password" -> showChangePassword(request, response);
            default -> response.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        SecuritySupport.applyNoStore(response::setHeader);
        String path = request.getServletPath();
        if (ApiRoutes.PUBLIC_CONSULTATION.equals(path)) {
            submitConsultation(request, response);
        } else if (ApiRoutes.PUBLIC_FAQ_TOPICS.equals(path) || ApiRoutes.PUBLIC_FAQ_COMMENTS.equals(path)) {
            submitFaqMutation(request, response, path);
        } else if (ApiRoutes.TENANT_LOGIN.equals(path)) {
            submitTenantLogin(request, response);
        } else if (ApiRoutes.TENANT_LOGOUT.equals(path)) {
            logoutTenant(request, response);
        } else if ("/change-password".equals(path)) {
            submitChangePassword(request, response);
        } else {
            response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
        }
    }

    private void submitTenantLogin(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String workspace = request.getParameter("tenantDomain");
        String email = request.getParameter("tenantEmail");
        String password = request.getParameter("tenantPassword");
        try {
            TenantPrincipal principal = tenantAuthService.authenticate(workspace, email, password);
            if (principal == null) {
                showTenantLoginError(request, response, workspace, email,
                        "Workspace, email hoặc mật khẩu không chính xác.");
                return;
            }
            HttpSession session = request.getSession(true);
            request.changeSessionId();
            session.setAttribute(SecuritySupport.TENANT_PRINCIPAL_ATTRIBUTE, principal);
            if (principal.isMustChangePassword()) {
                response.sendRedirect(request.getContextPath() + "/change-password");
                return;
            }
            response.sendRedirect(request.getContextPath() + tenantLandingPath(principal));
        } catch (SQLException exception) {
            getServletContext().log("Tenant login failed; SQLState=" + exception.getSQLState()
                    + "; errorCode=" + exception.getErrorCode());
            showTenantLoginError(request, response, workspace, email,
                    "Không thể kết nối dịch vụ đăng nhập. Vui lòng thử lại sau.");
        } finally {
            password = null;
        }
    }

    private void logoutTenant(HttpServletRequest request, HttpServletResponse response) throws IOException {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        response.sendRedirect(request.getContextPath() + ApiRoutes.TENANT_LOGIN);
    }

    private void showChangePassword(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        TenantPrincipal principal = SecuritySupport.getTenantPrincipal(request);
        if (principal == null) {
            response.sendRedirect(request.getContextPath() + ApiRoutes.TENANT_LOGIN);
            return;
        }
        render(request, response, "change-password.jsp");
    }

    private void submitChangePassword(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        TenantPrincipal principal = SecuritySupport.getTenantPrincipal(request);
        if (principal == null || principal.getUserId() == null) {
            response.sendRedirect(request.getContextPath() + ApiRoutes.TENANT_LOGIN);
            return;
        }

        String newPassword = request.getParameter("newPassword");
        String confirmPassword = request.getParameter("confirmPassword");
        if (newPassword == null || newPassword.isBlank()) {
            request.setAttribute("changePasswordField", "newPassword");
            renderChangePasswordError(request, response, "Mật khẩu mới không được để trống.");
            return;
        }
        if ("1".equals(newPassword)) {
            request.setAttribute("changePasswordField", "newPassword");
            renderChangePasswordError(request, response, "Vui lòng chọn mật khẩu khác mật khẩu tạm.");
            return;
        }
        if (!newPassword.equals(confirmPassword)) {
            request.setAttribute("changePasswordField", "confirmPassword");
            renderChangePasswordError(request, response, "Mật khẩu xác nhận không khớp.");
            return;
        }

        try {
            boolean updated = tenantAuthService.changePassword(principal.getUserId(), passwordHasher.hash(newPassword));
            if (!updated) {
                renderChangePasswordError(request, response, "Chưa thể đổi mật khẩu. Vui lòng đăng nhập lại.");
                return;
            }
            if (principal.getEmployeeId() != null) {
                employeeHistoryService.logEmployeeChange(principal.getCompanyId(), principal.getUserId(),
                        principal.getEmployeeId(), "CHANGE_PASSWORD", "Password updated", "Password updated",
                        request.getRemoteAddr());
            }
            request.getSession(true).setAttribute(SecuritySupport.TENANT_PRINCIPAL_ATTRIBUTE,
                    new TenantPrincipal(principal.getUserId(), principal.getCompanyId(), principal.getWorkspaceCode(),
                            principal.getCompanyName(), principal.getDisplayName(), principal.getEmail(),
                            principal.getRoleCode(), principal.getEmployeeId(), false));
            response.sendRedirect(request.getContextPath() + tenantLandingPath(principal));
        } catch (SQLException exception) {
            getServletContext().log("Tenant password change failed; SQLState=" + exception.getSQLState()
                    + "; errorCode=" + exception.getErrorCode());
            renderChangePasswordError(request, response, "Chưa thể đổi mật khẩu. Vui lòng thử lại sau.");
        }
    }

    private void renderChangePasswordError(HttpServletRequest request, HttpServletResponse response, String message)
            throws ServletException, IOException {
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        request.setAttribute("changePasswordError", message);
        render(request, response, "change-password.jsp");
    }

    private String tenantLandingPath(TenantPrincipal principal) {
        String role = principal.getRoleCode() == null ? "" : principal.getRoleCode().toUpperCase();
        return switch (role) {
            case "COMPANY_ADMIN" -> "/employee/employee-list";
            case "MANAGER" -> "/attendance/manager-attendance";
            case "EMPLOYEE" -> "/employee/my-profile";
            default -> "/login";
        };
    }

    private void showTenantLoginError(HttpServletRequest request, HttpServletResponse response,
                                      String workspace, String email, String message)
            throws ServletException, IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        request.setAttribute("loginError", message);
        request.setAttribute("tenantDomain", workspace);
        request.setAttribute("tenantEmail", email);
        render(request, response, "tenant-login.jsp");
    }

    private void showConsultation(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setAttribute("submitted", Boolean.TRUE.equals(consumeFlash(request, FLASH_SUCCESS)));
        request.setAttribute("emailWarning", consumeFlash(request, FLASH_EMAIL_WARNING));
        render(request, response, "consultation-request.jsp");
    }

    private void submitConsultation(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if (hasUnexpectedBusinessParameter(request)) {
            renderConsultationError(request, response, "Biểu mẫu không hợp lệ. Vui lòng chỉ gửi 5 trường bắt buộc.", Map.of());
            return;
        }
        try {
            ConsultationSubmission submission = consultationService.submit(
                    request.getParameter("fullName"), request.getParameter("email"), request.getParameter("phone"),
                    request.getParameter("companyName"), request.getParameter("region"));
            HttpSession session = request.getSession(true);
            session.removeAttribute(FLASH_EMAIL_WARNING);
            session.setAttribute(FLASH_SUCCESS, Boolean.TRUE);
            if (!submission.isEmailSent()) session.setAttribute(FLASH_EMAIL_WARNING,
                    "Yêu cầu đã được lưu ở trạng thái NEW. Email xác nhận chưa gửi được; đội ngũ HRMs vẫn sẽ tiếp nhận yêu cầu.");
            response.sendRedirect(request.getContextPath() + ApiRoutes.PUBLIC_CONSULTATION);
        } catch (ConsultationRequestService.InvalidConsultationException exception) {
            renderConsultationError(request, response, "Vui lòng kiểm tra lại thông tin.", exception.getValidation().getErrors());
        } catch (ConsultationRequestService.DuplicateConsultationException exception) {
            renderConsultationError(request, response,
                    "Email hoặc tên công ty đã có yêu cầu đang được xử lý. Vui lòng không gửi lại biểu mẫu.",
                    Map.of("email", "Email hoặc tên công ty đã có yêu cầu đang được xử lý.",
                            "companyName", "Email hoặc tên công ty đã có yêu cầu đang được xử lý."));
        } catch (SQLException exception) {
            getServletContext().log("Public consultation persistence failed; SQLState=" + exception.getSQLState()
                    + "; errorCode=" + exception.getErrorCode());
            renderConsultationError(request, response, "Chưa thể lưu yêu cầu. Vui lòng thử lại sau.", Map.of());
        }
    }

    private void showFaq(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            List<FaqTopic> topics = faqService.getTopics();
            request.setAttribute("faqTopics", topics);
            request.setAttribute("faqCsrfToken", SecuritySupport.ensurePublicCsrfToken(request.getSession(true)));
        } catch (SQLException exception) {
            getServletContext().log("Public FAQ read failed; SQLState=" + exception.getSQLState());
            request.setAttribute("faqTopics", List.of());
            request.setAttribute("faqReadError", "Chưa thể tải thảo luận. Vui lòng thử lại sau.");
        }
        render(request, response, "faqs.jsp");
    }

    private void submitFaqMutation(HttpServletRequest request, HttpServletResponse response, String path)
            throws ServletException, IOException {
        if (!SecuritySupport.hasValidPublicCsrfToken(request)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Phiên thao tác không hợp lệ.");
            return;
        }
        TenantPrincipal principal = SecuritySupport.getTenantPrincipal(request);
        if (principal == null || principal.getUserId() == null || principal.getUserId() <= 0) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Vui lòng đăng nhập để tạo chủ đề hoặc bình luận.");
            return;
        }
        try {
            int userId = principal.getUserId();
            if (!faqService.isTenantUserActive(userId)) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Phiên đăng nhập tenant không còn hợp lệ.");
                return;
            }
            if (ApiRoutes.PUBLIC_FAQ_TOPICS.equals(path)) {
                faqService.createTopic(userId, request.getParameter("title"), request.getParameter("content"));
            } else {
                faqService.createComment(userId, parsePositiveId(request.getParameter("topicId")), request.getParameter("content"));
            }
            response.sendRedirect(request.getContextPath() + ApiRoutes.PUBLIC_FAQ);
        } catch (FieldValidationException exception) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            request.setAttribute("faqErrorField", exception.getField());
            request.setAttribute("faqErrorMessage", exception.getMessage());
            request.setAttribute("faqErrorTopicId", request.getParameter("topicId"));
            request.setAttribute("faqTitle", request.getParameter("title"));
            request.setAttribute("faqContent", request.getParameter("content"));
            showFaq(request, response);
        } catch (IllegalArgumentException exception) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, exception.getMessage());
        } catch (SQLException exception) {
            getServletContext().log("FAQ mutation failed; SQLState=" + exception.getSQLState());
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Không thể lưu thao tác FAQ.");
        }
    }

    private void renderConsultationError(HttpServletRequest request, HttpServletResponse response,
                                         String message, Map<String, String> errors)
            throws ServletException, IOException {
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        request.setAttribute("formError", message);
        request.setAttribute("fieldErrors", errors);
        request.setAttribute("formFullName", request.getParameter("fullName"));
        request.setAttribute("formEmail", request.getParameter("email"));
        request.setAttribute("formPhone", request.getParameter("phone"));
        request.setAttribute("formCompanyName", request.getParameter("companyName"));
        request.setAttribute("formRegion", request.getParameter("region"));
        render(request, response, "consultation-request.jsp");
    }

    private void render(HttpServletRequest request, HttpServletResponse response, String view)
            throws ServletException, IOException {
        request.getRequestDispatcher("/WEB-INF/views/public/" + view).forward(request, response);
    }

    private boolean hasUnexpectedBusinessParameter(HttpServletRequest request) {
        Set<String> allowed = Set.of("fullName", "email", "phone", "companyName", "region");
        Map<String, String[]> parameters = request.getParameterMap();
        if (parameters == null) return false;
        for (String parameterName : parameters.keySet()) {
            if (!allowed.contains(parameterName)) return true;
        }
        return false;
    }

    private int parsePositiveId(String value) {
        try {
            int id = Integer.parseInt(value == null ? "" : value);
            if (id <= 0) throw new NumberFormatException();
            return id;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Chủ đề không hợp lệ.");
        }
    }

    private Object consumeFlash(HttpServletRequest request, String key) {
        HttpSession session = request.getSession(false);
        if (session == null) return null;
        Object value = session.getAttribute(key);
        session.removeAttribute(key);
        return value;
    }
}

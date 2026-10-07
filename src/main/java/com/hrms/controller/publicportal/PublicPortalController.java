package com.hrms.controller.publicportal;

import com.hrms.api.ApiRoutes;
import com.hrms.service.security.TenantPrincipal;
import com.hrms.model.publicportal.ConsultationSubmission;
import com.hrms.model.publicportal.FaqTopic;
import com.hrms.service.TenantAuthService;
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

    public PublicPortalController() {
        this(new ConsultationRequestService(), new FaqService(), new TenantAuthService());
    }

    PublicPortalController(ConsultationRequestService consultationService, FaqService faqService,
                           TenantAuthService tenantAuthService) {
        this.consultationService = consultationService;
        this.faqService = faqService;
        this.tenantAuthService = tenantAuthService;
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
            request.changeSessionId();
            request.getSession(true).setAttribute(SecuritySupport.TENANT_PRINCIPAL_ATTRIBUTE, principal);
            response.sendRedirect(request.getContextPath() + "/employee/employee-list.html");
        } catch (SQLException exception) {
            getServletContext().log("Tenant login failed; SQLState=" + exception.getSQLState()
                    + "; errorCode=" + exception.getErrorCode());
            showTenantLoginError(request, response, workspace, email,
                    "Không thể kết nối dịch vụ đăng nhập. Vui lòng thử lại sau.");
        } finally {
            password = null;
        }
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
                    "Email hoặc tên công ty đã có yêu cầu đang được xử lý. Vui lòng không gửi lại biểu mẫu.", Map.of());
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

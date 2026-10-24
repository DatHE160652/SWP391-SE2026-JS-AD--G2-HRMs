package com.hrms.controller.sysadmin;

import com.hrms.api.ApiRoutes;
import com.hrms.model.RegistrationRequest;
import com.hrms.model.RegistrationRequestFilter;
import com.hrms.model.RegistrationRequestPage;
import com.hrms.model.RegistrationRequestStats;
import com.hrms.service.RegistrationRequestService;
import com.hrms.service.RequestActionException;
import com.hrms.service.security.SystemAdminPrincipal;
import com.hrms.util.SecuritySupport;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;

/** System Admin request pages and guarded provisioning/soft-delete actions. */
public class RegistrationRequestController extends HttpServlet {
    private static final String FLASH_MESSAGE = "registrationActionMessage";
    private static final String FLASH_WARNING = "registrationActionWarning";
    private final RegistrationRequestService registrationRequestService;

    public RegistrationRequestController() {
        this(new RegistrationRequestService());
    }

    RegistrationRequestController(RegistrationRequestService registrationRequestService) {
        this.registrationRequestService = registrationRequestService;
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        SecuritySupport.applyNoStore(response::setHeader);
        String path = request.getServletPath();
        if (ApiRoutes.SYSTEM_ADMIN_DASHBOARD.equals(path)) {
            showDashboard(request, response);
            return;
        }
        if (ApiRoutes.REGISTRATION_REQUESTS.equals(path)) {
            showList(request, response);
            return;
        }
        if (ApiRoutes.REGISTRATION_REQUEST_DETAILS.equals(path)) {
            showDetails(request, response);
            return;
        }
        if (ApiRoutes.REGISTRATION_REQUEST_CONFIRM.equals(path) || ApiRoutes.REGISTRATION_REQUEST_DELETE.equals(path)) {
            response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
            return;
        }
        response.sendError(HttpServletResponse.SC_NOT_FOUND);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        SecuritySupport.applyNoStore(response::setHeader);
        String path = request.getServletPath();
        if (!ApiRoutes.REGISTRATION_REQUEST_CONFIRM.equals(path)
                && !ApiRoutes.REGISTRATION_REQUEST_DELETE.equals(path)) {
            response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
            return;
        }
        if (!SecuritySupport.isSystemAdmin(request) || !SecuritySupport.hasValidCsrfToken(request)
                || SecuritySupport.getPrincipal(request).getUserId() == null
                || SecuritySupport.getPrincipal(request).getUserId() <= 0) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Phiên thao tác không hợp lệ.");
            return;
        }
        int requestId;
        try {
            requestId = parsePositiveId(request.getParameter("id"));
        } catch (IllegalArgumentException exception) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Mã Registration Request không hợp lệ.");
            return;
        }
        SystemAdminPrincipal principal = SecuritySupport.getPrincipal(request);
        try {
            if (ApiRoutes.REGISTRATION_REQUEST_CONFIRM.equals(path)) {
                RegistrationRequestService.ConfirmationResult result = registrationRequestService.confirm(
                        requestId, principal.getUserId(), request.getRemoteAddr());
                request.getSession(true).removeAttribute(FLASH_WARNING);
                String emailNotice = switch (result.emailStatus()) {
                    case "ACCEPTED" -> " Máy chủ email đã chấp nhận gửi thông tin đăng nhập (chưa bảo đảm đã đến hộp thư).";
                    case "UNKNOWN" -> " Chưa xác định được kết quả gửi email; không phê duyệt lại để gửi lại.";
                    default -> " Email kích hoạt chưa gửi được; tenant vẫn được giữ ở trạng thái CONVERTED.";
                };
                request.getSession(true).setAttribute(FLASH_MESSAGE,
                        "Đã phê duyệt và khởi tạo tenant " + result.workspaceCode() + "." + emailNotice);
                if (!result.emailMetadataRecorded()) {
                    request.getSession(true).setAttribute(FLASH_WARNING,
                            "Chưa ghi nhận được kết quả gửi email vào hệ thống. Không tạo lại tenant; gửi lại email thuộc.");
                }
                response.sendRedirect(request.getContextPath() + ApiRoutes.REGISTRATION_REQUEST_DETAILS + "?id=" + requestId);
            } else {
                registrationRequestService.softDelete(requestId, principal.getUserId(), request.getRemoteAddr());
                request.getSession(true).setAttribute(FLASH_MESSAGE,
                        "Đã xóa mềm Registration Request. Hồ sơ gốc vẫn được lưu và không thể đăng ký lại bằng email/tên công ty này.");
                response.sendRedirect(request.getContextPath() + ApiRoutes.REGISTRATION_REQUESTS);
            }
        } catch (RequestActionException exception) {
            int status = switch (exception.getReason()) {
                case FORBIDDEN -> HttpServletResponse.SC_FORBIDDEN;
                case NOT_FOUND -> HttpServletResponse.SC_NOT_FOUND;
                case CONFLICT -> HttpServletResponse.SC_CONFLICT;
            };
            showErrorWithStatus(request, response, status, exception.getMessage());
        } catch (IllegalArgumentException exception) {
            showErrorWithStatus(request, response, HttpServletResponse.SC_BAD_REQUEST, exception.getMessage());
        } catch (SQLException exception) {
            getServletContext().log("System Admin registration mutation failed; SQLState="
                    + exception.getSQLState() + "; errorCode=" + exception.getErrorCode());
            showError(request, response, "Không thể lưu thao tác. Vui lòng thử lại sau.");
        }
    }

    private void showDashboard(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            setNavigation(request, "dashboard");
            request.setAttribute("registrationRequests", registrationRequestService.getRecentPending());
            request.getRequestDispatcher("/WEB-INF/views/sysadmin/system-admin-dashboard.jsp")
                    .forward(request, response);
        } catch (SQLException exception) {
            showError(request, response, "Không thể tải tổng quan quản trị.");
        }
    }

    private void showList(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            RegistrationRequestFilter filter = registrationRequestService.createFilter(
                    request.getParameter("q"), request.getParameter("status"), request.getParameter("region"),
                    request.getParameter("date"), request.getParameter("page"), request.getParameter("size"));
            RegistrationRequestPage page = registrationRequestService.getPage(filter);
            setNavigation(request, "requests");
            request.setAttribute("filter", filter);
            request.setAttribute("requestPage", page);
            request.setAttribute("registrationRequests", page.getItems());
            request.getRequestDispatcher("/WEB-INF/views/sysadmin/registration-request-list.jsp")
                    .forward(request, response);
        } catch (IllegalArgumentException exception) {
            showErrorWithStatus(request, response, HttpServletResponse.SC_BAD_REQUEST, exception.getMessage());
        } catch (SQLException exception) {
            showError(request, response, "Không thể tải danh sách Registration Request.");
        }
    }

    private void showDetails(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String idValue = request.getParameter("id");
        int requestId;
        try {
            requestId = parsePositiveId(idValue);
        } catch (IllegalArgumentException exception) {
            showErrorWithStatus(request, response, HttpServletResponse.SC_BAD_REQUEST,
                    "Mã Registration Request không hợp lệ.");
            return;
        }

        try {
            RegistrationRequest registrationRequest = registrationRequestService.getVisibleRequest(requestId);
            if (registrationRequest == null) {
                showErrorWithStatus(request, response, HttpServletResponse.SC_NOT_FOUND,
                        "Không tìm thấy Registration Request.");
                return;
            }
            setNavigation(request, "requests");
            request.setAttribute("registrationRequest", registrationRequest);
            request.getRequestDispatcher("/WEB-INF/views/sysadmin/registration-request-details.jsp")
                    .forward(request, response);
        } catch (SQLException exception) {
            showError(request, response, "Không thể tải chi tiết Registration Request.");
        }
    }

    private void setNavigation(HttpServletRequest request, String activeNav) throws SQLException {
        SecuritySupport.ensureCsrfToken(request.getSession(true));
        consumeFlash(request, FLASH_MESSAGE, "actionMessage");
        consumeFlash(request, FLASH_WARNING, "actionWarning");
        RegistrationRequestStats stats = registrationRequestService.getStats("dashboard".equals(activeNav));
        request.setAttribute("activeNav", activeNav);
        request.setAttribute("stats", stats);
        request.setAttribute("pendingCount", stats.getPendingCount());
    }

    private void consumeFlash(HttpServletRequest request, String key, String attribute) {
        if (request.getSession(false) == null) return;
        Object message = request.getSession(false).getAttribute(key);
        request.getSession(false).removeAttribute(key);
        if (message != null) request.setAttribute(attribute, message);
    }

    private int parsePositiveId(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException();
        }
        int parsed;
        try {
            parsed = Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException();
        }
        if (parsed <= 0) {
            throw new IllegalArgumentException();
        }
        return parsed;
    }

    private void showError(HttpServletRequest request, HttpServletResponse response, String message)
            throws ServletException, IOException {
        showErrorWithStatus(request, response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, message);
    }

    private void showErrorWithStatus(HttpServletRequest request, HttpServletResponse response,
                                     int status, String message) throws ServletException, IOException {
        response.setStatus(status);
        request.setAttribute("errorMessage", message);
        request.getRequestDispatcher("/WEB-INF/views/error.jsp").forward(request, response);
    }
}

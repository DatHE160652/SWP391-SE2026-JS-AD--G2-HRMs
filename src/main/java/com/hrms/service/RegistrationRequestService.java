package com.hrms.service;

import com.hrms.dao.RegistrationRequestDAO;
import com.hrms.model.RegistrationRequest;
import com.hrms.model.RegistrationRequestFilter;
import com.hrms.model.RegistrationRequestPage;
import com.hrms.model.RegistrationRequestStats;
import com.hrms.service.email.ActivationEmailSender;
import com.hrms.service.email.ConfiguredActivationEmailSender;
import com.hrms.service.email.EmailDeliveryException;

import java.net.URI;
import java.security.SecureRandom;
import java.sql.SQLException;
import java.util.Base64;
import java.util.List;

public class RegistrationRequestService {
    private static final SecureRandom RANDOM = new SecureRandom();
    private final RegistrationRequestDAO registrationRequestDAO;
    private final ActivationEmailSender activationEmailSender;

    public RegistrationRequestService() {
        this(new RegistrationRequestDAO());
    }

    public RegistrationRequestService(RegistrationRequestDAO registrationRequestDAO) {
        this(registrationRequestDAO, new ConfiguredActivationEmailSender());
    }

    public RegistrationRequestService(RegistrationRequestDAO dao, ActivationEmailSender sender) {
        this.registrationRequestDAO = dao;
        this.activationEmailSender = sender;
    }

    public ConfirmationResult confirm(int requestId, int actorId, String ipAddress)
            throws SQLException, RequestActionException {
        validateActionIds(requestId, actorId);
        byte[] bytes = new byte[24];
        RANDOM.nextBytes(bytes);
        String temporaryPassword = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        try {
            RegistrationRequestDAO.MutationResult result = registrationRequestDAO.confirmAndProvision(
                    requestId, actorId, temporaryPassword, ipAddress);
            requireSuccess(result);
            String delivery = "ACCEPTED";
            String errorCode = null;
            try {
                String loginUrl = activationLoginUrl(result.workspaceCode());
                activationEmailSender.send(result.request(), result.workspaceCode(), loginUrl, temporaryPassword);
            } catch (EmailDeliveryException exception) {
                delivery = "MAIL_PROVIDER_UNAVAILABLE".equals(exception.getCode()) ? "UNKNOWN" : "FAILED";
                errorCode = exception.getCode();
            } catch (IllegalArgumentException exception) {
                delivery = "FAILED";
                errorCode = "MAIL_LOGIN_URL_INVALID";
            } catch (RuntimeException exception) {
                delivery = "UNKNOWN";
                errorCode = "MAIL_PROVIDER_RUNTIME_ERROR";
            }
            boolean metadataRecorded = true;
            try {
                registrationRequestDAO.markEmailAttempt(result.emailAttemptId(), delivery, errorCode);
            } catch (SQLException exception) {
                metadataRecorded = false;
            }
            return new ConfirmationResult(result.companyId(), result.workspaceCode(), delivery, metadataRecorded);
        } finally {
            java.util.Arrays.fill(bytes, (byte) 0);
            temporaryPassword = null;
        }
    }

    public void softDelete(int requestId, int actorId, String ipAddress)
            throws SQLException, RequestActionException {
        validateActionIds(requestId, actorId);
        requireSuccess(registrationRequestDAO.softDelete(requestId, actorId, ipAddress));
    }

    private void validateActionIds(int requestId, int actorId) {
        if (requestId <= 0 || actorId <= 0) throw new IllegalArgumentException("Mã yêu cầu hoặc người thực hiện không hợp lệ.");
    }

    private void requireSuccess(RegistrationRequestDAO.MutationResult result) throws RequestActionException {
        switch (result.outcome()) {
            case "FORBIDDEN" -> throw new RequestActionException(RequestActionException.Reason.FORBIDDEN,
                    "Phiên quản trị không còn quyền thực hiện thao tác.");
            case "NOT_FOUND" -> throw new RequestActionException(RequestActionException.Reason.NOT_FOUND,
                    "Không tìm thấy Registration Request.");
            case "CONFLICT" -> throw new RequestActionException(RequestActionException.Reason.CONFLICT,
                    "Yêu cầu đã được xử lý hoặc email/tên công ty đã tồn tại. Vui lòng tải lại trang.");
            case "CONFIRMED", "DELETED" -> { }
            default -> throw new IllegalStateException("Invalid mutation result.");
        }
    }

    static String activationLoginUrl(String workspace) {
        if (workspace == null || !workspace.matches("[a-z0-9](?:[a-z0-9-]{0,48}[a-z0-9])?")) {
            throw new IllegalArgumentException("Invalid workspace code.");
        }
        String template = System.getenv("HRM_ACTIVATION_LOGIN_URL_TEMPLATE");
        if (template == null || template.isBlank()) template = System.getProperty("hrm.activation.loginUrlTemplate");
        if (template == null || template.isBlank()) template = "https://{workspace}.hrms.com/login";
        URI url = URI.create(template.replace("{workspace}", workspace));
        String host = url.getHost();
        boolean local = "localhost".equals(host) || "127.0.0.1".equals(host) || "[::1]".equals(host);
        if (host == null || url.getUserInfo() != null || url.getFragment() != null || url.getQuery() != null
                || !("https".equals(url.getScheme()) || (local && "http".equals(url.getScheme())))) {
            throw new IllegalArgumentException("Invalid activation login URL configuration.");
        }
        return url.toASCIIString();
    }

    public record ConfirmationResult(int companyId, String workspaceCode, String emailStatus,
                                     boolean emailMetadataRecorded) { }

    public List<RegistrationRequest> getVisibleRequests() throws SQLException {
        return registrationRequestDAO.findVisibleRequests();
    }

    public RegistrationRequestFilter createFilter(String q, String status, String region, String date,
                                                   String page, String size) {
        return new RegistrationRequestFilter(q, status, region, date, page, size);
    }

    public RegistrationRequestPage getPage(RegistrationRequestFilter filter) throws SQLException {
        return registrationRequestDAO.findPage(filter);
    }

    public RegistrationRequestStats getStats(boolean includePlatformCounts) throws SQLException {
        return registrationRequestDAO.getStats(includePlatformCounts);
    }

    public List<RegistrationRequest> getRecentPending() throws SQLException {
        return registrationRequestDAO.findRecentPending();
    }

    public RegistrationRequest getVisibleRequest(int requestId) throws SQLException {
        return registrationRequestDAO.findVisibleById(requestId);
    }
}

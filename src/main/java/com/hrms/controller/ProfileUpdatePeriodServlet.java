package com.hrms.controller;

import com.hrms.model.ProfileUpdatePeriod;
import com.hrms.service.FieldValidationException;
import com.hrms.service.ProfileUpdateService;
import com.hrms.service.security.TenantPrincipal;
import com.hrms.util.SecuritySupport;
import com.hrms.util.ValidationSupport;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.stream.Collectors;

public class ProfileUpdatePeriodServlet extends HttpServlet {
    private final ProfileUpdateService service = new ProfileUpdateService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        SecuritySupport.applyNoStore(response::setHeader);
        TenantPrincipal principal = SecuritySupport.getTenantPrincipal(request);
        try {
            service.requireAdministrator(principal);
            String csrfToken = SecuritySupport.ensurePublicCsrfToken(request.getSession(true));
            if ("/employee/profile-update-periods".equals(request.getServletPath())) {
                request.setAttribute("canManageEmployees", "COMPANY_ADMIN".equals(principal.getRoleCode()));
                request.getRequestDispatcher("/WEB-INF/views/comadmin-hr/profile-update-periods.jsp").forward(request, response);
                return;
            }
            String periods = service.getPeriods(principal).stream().map(ProfileUpdatePeriodServlet::toJson)
                    .collect(Collectors.joining(",", "[", "]"));
            writeJson(response, HttpServletResponse.SC_OK,
                    "{\"success\":true,\"csrfToken\":" + quote(csrfToken) + ",\"data\":" + periods + "}");
        } catch (SecurityException exception) {
            writeJson(response, HttpServletResponse.SC_FORBIDDEN, ValidationSupport.errorJson(exception));
        } catch (SQLException exception) {
            getServletContext().log("Profile update period read failed", exception);
            writeJson(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "{\"success\":false,\"message\":\"Không thể tải đợt cập nhật hồ sơ.\"}");
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        SecuritySupport.applyNoStore(response::setHeader);
        try {
            TenantPrincipal principal = SecuritySupport.getTenantPrincipal(request);
            service.requireAdministrator(principal);
            if (!SecuritySupport.hasValidPublicCsrfToken(request)) {
                throw new SecurityException("Phiên thao tác không hợp lệ. Vui lòng tải lại trang.");
            }
            if ("/employees/profile-update-periods/create".equals(request.getServletPath())) {
                service.create(principal, request.getParameter("title"),
                        parseTime(request.getParameter("startsAt"), "startsAt"),
                        parseTime(request.getParameter("endsAt"), "endsAt"), request.getRemoteAddr());
            } else if ("/employees/profile-update-periods/close".equals(request.getServletPath())) {
                service.close(principal, Long.parseLong(request.getParameter("periodId")), request.getRemoteAddr());
            } else {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            writeJson(response, HttpServletResponse.SC_OK, "{\"success\":true,\"message\":\"Đã lưu đợt cập nhật hồ sơ.\"}");
        } catch (SecurityException exception) {
            writeJson(response, HttpServletResponse.SC_FORBIDDEN, ValidationSupport.errorJson(exception));
        } catch (IllegalArgumentException exception) {
            writeJson(response, HttpServletResponse.SC_BAD_REQUEST, ValidationSupport.errorJson(exception));
        } catch (SQLException exception) {
            getServletContext().log("Profile update period write failed", exception);
            writeJson(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "{\"success\":false,\"message\":\"Không thể lưu đợt cập nhật hồ sơ.\"}");
        }
    }

    private LocalDateTime parseTime(String value, String field) {
        try {
            return OffsetDateTime.parse(value == null ? "" : value).withOffsetSameInstant(ZoneOffset.UTC).toLocalDateTime();
        } catch (DateTimeParseException exception) {
            throw new FieldValidationException(field, "Vui lòng chọn thời gian hợp lệ.");
        }
    }

    static String toJson(ProfileUpdatePeriod period) {
        if (period == null) {
            return "null";
        }
        return "{\"periodId\":" + period.periodId() + ",\"title\":" + quote(period.title())
                + ",\"startsAt\":" + quote(period.startsAt().atOffset(ZoneOffset.UTC).toString())
                + ",\"endsAt\":" + quote(period.endsAt().atOffset(ZoneOffset.UTC).toString())
                + ",\"closed\":" + (period.closedAt() != null)
                + ",\"open\":" + period.isOpen(LocalDateTime.now(ZoneOffset.UTC)) + "}";
    }

    private static String quote(String value) {
        return ValidationSupport.quote(value);
    }

    private void writeJson(HttpServletResponse response, int status, String content) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json; charset=UTF-8");
        response.getWriter().write(content);
    }
}

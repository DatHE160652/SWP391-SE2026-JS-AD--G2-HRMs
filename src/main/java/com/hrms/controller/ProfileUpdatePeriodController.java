package com.hrms.controller;

import com.hrms.dao.DocumentUpdatePeriodDAO;
import com.hrms.dao.ProfileUpdateDAO;
import com.hrms.model.ProfileUpdatePeriod;
import com.hrms.service.FieldValidationException;
import com.hrms.service.ProfileUpdateService;
import com.hrms.service.security.TenantPrincipal;
import com.hrms.util.Json;
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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ProfileUpdatePeriodController extends HttpServlet {
    private final ProfileUpdateService service = new ProfileUpdateService();
    private final DocumentUpdatePeriodDAO documentPeriodDAO = new DocumentUpdatePeriodDAO();

    private boolean documents(HttpServletRequest request) {
        return request.getServletPath().contains("document-update-periods");
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        SecuritySupport.applyNoStore(response::setHeader);
        TenantPrincipal principal = SecuritySupport.getTenantPrincipal(request);
        try {
            service.requireAdministrator(principal);
            String csrfToken = SecuritySupport.ensurePublicCsrfToken(request.getSession(true));
            if ("/employee/profile-update-periods".equals(request.getServletPath())
                    || "/employee/document-update-periods".equals(request.getServletPath())) {
                request.setAttribute("canManageEmployees", principal.isCompanyAdministrator());
                request.setAttribute("documentUpdatePeriods", documents(request));
                String subject = documents(request) ? "tài liệu" : "hồ sơ";
                request.setAttribute("updatePeriodHeading", com.hrms.i18n.Language.message(request, "Đợt cập nhật " + subject));
                request.setAttribute("updatePeriodBreadcrumb", com.hrms.i18n.Language.message(request, "Quản trị nhân sự / Cập nhật " + subject));
                request.setAttribute("updatePeriodSubtitle", com.hrms.i18n.Language.message(request, "Quản lý thời gian cập nhật " + subject + " độc lập cho nhân viên trong công ty."));
                request.setAttribute("updatePeriodCreateHeading", com.hrms.i18n.Language.message(request, "Tạo đợt cập nhật " + subject));
                request.setAttribute("updatePeriodCreateDescription", com.hrms.i18n.Language.message(request, "Thiết lập khoảng thời gian cho phép nhân viên cập nhật " + subject + "."));
                request.setAttribute("updatePeriodType", documents(request) ? "DOCUMENT" : "PROFILE");
                request.getRequestDispatcher("/WEB-INF/views/comadmin-hr/profile-update-periods.jsp").forward(request, response);
                return;
            }

            int companyId = principal.getCompanyId();
            if (documents(request)) {
                if ("types".equalsIgnoreCase(request.getParameter("action"))) {
                    List<Map<String, Object>> types = documentPeriodDAO.getDocumentTypes(companyId, true).stream().map(t -> {
                        Map<String, Object> m = new LinkedHashMap<>();
                        m.put("documentTypeId", t.documentTypeId());
                        m.put("typeName", t.typeName());
                        m.put("isActive", t.isActive());
                        return m;
                    }).toList();
                    writeJson(response, HttpServletResponse.SC_OK,
                            "{\"success\":true,\"csrfToken\":" + quote(csrfToken) + ",\"data\":" + Json.encode(types) + "}");
                    return;
                }
                String detailPeriodId = request.getParameter("periodId");
                if (detailPeriodId != null && !detailPeriodId.isBlank()) {
                    Map<String, Object> detail = documentPeriodDAO.getPeriodDetail(companyId, Long.parseLong(detailPeriodId.trim()));
                    writeJson(response, HttpServletResponse.SC_OK,
                            "{\"success\":true,\"csrfToken\":" + quote(csrfToken) + ",\"data\":" + Json.encode(detail) + "}");
                    return;
                }
                List<Map<String, Object>> periods = documentPeriodDAO.getPeriods(companyId);
                List<Map<String, Object>> types = documentPeriodDAO.getDocumentTypes(companyId, true).stream().map(t -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("documentTypeId", t.documentTypeId());
                    m.put("typeName", t.typeName());
                    m.put("isActive", t.isActive());
                    return m;
                }).toList();
                List<Map<String, Object>> employees = documentPeriodDAO.getActiveCompanyEmployees(companyId);
                writeJson(response, HttpServletResponse.SC_OK,
                        "{\"success\":true,\"csrfToken\":" + quote(csrfToken)
                                + ",\"data\":" + Json.encode(periods)
                                + ",\"documentTypes\":" + Json.encode(types)
                                + ",\"employees\":" + Json.encode(employees) + "}");
                return;
            }

            String detailPeriodId = request.getParameter("periodId");
            if (detailPeriodId != null && !detailPeriodId.isBlank()) {
                Map<String, Object> detail = service.getPeriodDetail(principal, Long.parseLong(detailPeriodId.trim()));
                writeJson(response, HttpServletResponse.SC_OK,
                        "{\"success\":true,\"csrfToken\":" + quote(csrfToken) + ",\"data\":" + Json.encode(detail) + "}");
                return;
            }

            List<Map<String, Object>> periods = service.getDetailedPeriods(principal);
            List<Map<String, Object>> profileFields = service.getFieldCatalog();
            List<Map<String, Object>> employees = documentPeriodDAO.getActiveCompanyEmployees(companyId);
            writeJson(response, HttpServletResponse.SC_OK,
                    "{\"success\":true,\"csrfToken\":" + quote(csrfToken)
                            + ",\"data\":" + Json.encode(periods)
                            + ",\"profileFields\":" + Json.encode(profileFields)
                            + ",\"employees\":" + Json.encode(employees) + "}");
        } catch (SecurityException exception) {
            writeJson(response, HttpServletResponse.SC_FORBIDDEN, ValidationSupport.errorJson(exception));
        } catch (IllegalArgumentException exception) {
            writeJson(response, HttpServletResponse.SC_BAD_REQUEST, ValidationSupport.errorJson(exception));
        } catch (SQLException exception) {
            getServletContext().log("Profile update period read failed", exception);
            writeJson(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "{\"success\":false,\"message\":\"Không thể tải đợt cập nhật.\"}");
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

            if (documents(request)) {
                int companyId = principal.getCompanyId();
                int userId = principal.getUserId();
                String action = request.getParameter("action");
                if ("addType".equalsIgnoreCase(action)) {
                    var created = documentPeriodDAO.addDocumentType(companyId, userId, request.getParameter("typeName"), request.getRemoteAddr());
                    writeJson(response, HttpServletResponse.SC_OK,
                            "{\"success\":true,\"message\":\"Đã thêm loại tài liệu mới.\",\"data\":{\"documentTypeId\":"
                                    + created.documentTypeId() + ",\"typeName\":" + quote(created.typeName()) + "}}");
                    return;
                }
                if ("deleteType".equalsIgnoreCase(action)) {
                    int typeId = Integer.parseInt(request.getParameter("documentTypeId"));
                    documentPeriodDAO.deleteOrDeactivateDocumentType(companyId, userId, typeId, request.getRemoteAddr());
                    writeJson(response, HttpServletResponse.SC_OK,
                            "{\"success\":true,\"message\":\"Đã xóa loại tài liệu.\"}");
                    return;
                }
                if ("/employees/document-update-periods/create".equals(request.getServletPath())) {
                    List<Integer> typeIds = parseIdList(request, "documentTypeIds");
                    List<Integer> employeeIds = parseIdList(request, "employeeIds");
                    documentPeriodDAO.createPeriod(
                            companyId,
                            userId,
                            request.getParameter("title"),
                            parseTime(request.getParameter("startsAt"), "startsAt"),
                            parseTime(request.getParameter("endsAt"), "endsAt"),
                            request.getParameter("scopeType"),
                            typeIds,
                            employeeIds,
                            request.getRemoteAddr()
                    );
                } else if ("/employees/document-update-periods/close".equals(request.getServletPath())) {
                    documentPeriodDAO.closePeriod(companyId, userId, Long.parseLong(request.getParameter("periodId")), request.getRemoteAddr());
                } else {
                    response.sendError(HttpServletResponse.SC_NOT_FOUND);
                    return;
                }
                writeJson(response, HttpServletResponse.SC_OK,
                        "{\"success\":true,\"message\":\"Đã lưu đợt cập nhật tài liệu.\"}");
                return;
            }

            if ("/employees/profile-update-periods/create".equals(request.getServletPath())) {
                List<String> allowedFields = parseStringList(request, "allowedFields");
                if (allowedFields.isEmpty() && request.getParameterValues("allowedFields") == null) {
                    allowedFields = ProfileUpdateDAO.DEFAULT_FIELD_KEYS;
                }
                List<Integer> employeeIds = parseIdList(request, "employeeIds");
                service.create(
                        principal,
                        request.getParameter("title"),
                        parseTime(request.getParameter("startsAt"), "startsAt"),
                        parseTime(request.getParameter("endsAt"), "endsAt"),
                        request.getParameter("scopeType"),
                        allowedFields,
                        employeeIds,
                        request.getRemoteAddr()
                );
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
                    "{\"success\":false,\"message\":\"Không thể lưu đợt cập nhật.\"}");
        }
    }

    private List<String> parseStringList(HttpServletRequest request, String paramName) {
        List<String> list = new ArrayList<>();
        String[] values = request.getParameterValues(paramName);
        if (values == null) {
            return list;
        }
        for (String val : values) {
            if (val == null || val.isBlank()) {
                continue;
            }
            for (String token : val.split(",")) {
                String trimmed = token.trim();
                if (!trimmed.isEmpty()) {
                    list.add(trimmed);
                }
            }
        }
        return list;
    }

    private List<Integer> parseIdList(HttpServletRequest request, String paramName) {
        List<Integer> ids = new ArrayList<>();
        String[] values = request.getParameterValues(paramName);
        if (values == null) {
            return ids;
        }
        for (String val : values) {
            if (val == null || val.isBlank()) {
                continue;
            }
            for (String token : val.split(",")) {
                String trimmed = token.trim();
                if (!trimmed.isEmpty()) {
                    try {
                        ids.add(Integer.parseInt(trimmed));
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
        }
        return ids;
    }

    private LocalDateTime parseTime(String value, String field) {
        try {
            return OffsetDateTime.parse(value == null ? "" : value).withOffsetSameInstant(ZoneOffset.UTC).toLocalDateTime();
        } catch (DateTimeParseException exception) {
            throw new FieldValidationException(field, "Vui lòng chọn thời gian hợp lệ.");
        }
    }

    public static String toJson(ProfileUpdatePeriod period) {
        if (period == null) {
            return "null";
        }
        return "{\"periodId\":" + period.periodId() + ",\"title\":" + quote(period.title())
                + ",\"periodType\":" + quote(period.periodType())
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

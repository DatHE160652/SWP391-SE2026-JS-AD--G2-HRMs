package com.hrms.util;

import com.hrms.api.ApiRoutes;
import com.hrms.service.security.SystemAdminPrincipal;
import com.hrms.service.security.TenantPrincipal;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import java.security.SecureRandom;
import java.util.Base64;

/** Session and CSRF helpers shared by public, tenant, and System Admin flows. */
public final class SecuritySupport {
    public static final String PRINCIPAL_ATTRIBUTE = "systemAdminPrincipal";
    public static final String CSRF_ATTRIBUTE = "systemAdminCsrfToken";
    public static final String PUBLIC_CSRF_ATTRIBUTE = "publicFaqCsrfToken";
    public static final String TENANT_PRINCIPAL_ATTRIBUTE = "tenantPrincipal";
    public static final String TENANT_CSRF_ATTRIBUTE = "tenantPasswordCsrfToken";

    private static final SecureRandom RANDOM = new SecureRandom();

    private SecuritySupport() {
    }

    public static String ensureCsrfToken(HttpSession session) {
        return ensureToken(session, CSRF_ATTRIBUTE);
    }

    public static boolean hasValidCsrfToken(HttpServletRequest request) {
        return hasValidToken(request, CSRF_ATTRIBUTE);
    }

    public static String ensurePublicCsrfToken(HttpSession session) {
        return ensureToken(session, PUBLIC_CSRF_ATTRIBUTE);
    }

    public static boolean hasValidPublicCsrfToken(HttpServletRequest request) {
        return hasValidToken(request, PUBLIC_CSRF_ATTRIBUTE);
    }

    public static boolean isSystemAdmin(HttpServletRequest request) {
        SystemAdminPrincipal principal = getPrincipal(request);
        return principal != null && principal.isSystemAdmin();
    }

    public static SystemAdminPrincipal getPrincipal(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        Object value = session == null ? null : session.getAttribute(PRINCIPAL_ATTRIBUTE);
        return value instanceof SystemAdminPrincipal principal ? principal : null;
    }

    public static String ensureTenantCsrfToken(HttpSession session) {
        return ensureToken(session, TENANT_CSRF_ATTRIBUTE);
    }

    public static boolean hasValidTenantCsrfToken(HttpServletRequest request) {
        return hasValidToken(request, TENANT_CSRF_ATTRIBUTE);
    }

    public static boolean isTenantUser(HttpServletRequest request) {
        return getTenantPrincipal(request) != null;
    }

    public static TenantPrincipal getTenantPrincipal(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        Object value = session == null ? null : session.getAttribute(TENANT_PRINCIPAL_ATTRIBUTE);
        return value instanceof TenantPrincipal principal ? principal : null;
    }

    public static void applyNoStore(HttpServletResponseAdapter response) {
        response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        response.setHeader("Expires", "0");
    }

    public static String loginUrl(HttpServletRequest request) {
        return request.getContextPath() + ApiRoutes.SYSTEM_ADMIN_LOGIN;
    }

    private static String ensureToken(HttpSession session, String attribute) {
        Object existing = session.getAttribute(attribute);
        if (existing instanceof String token && !token.isBlank()) {
            return token;
        }
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        session.setAttribute(attribute, token);
        return token;
    }

    private static boolean hasValidToken(HttpServletRequest request, String attribute) {
        HttpSession session = request.getSession(false);
        String expected = session == null ? null : (String) session.getAttribute(attribute);
        String submitted = request.getParameter("csrfToken");
        return expected != null && submitted != null && constantTimeEquals(expected, submitted);
    }

    private static boolean constantTimeEquals(String first, String second) {
        byte[] firstBytes = first.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        byte[] secondBytes = second.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        return java.security.MessageDigest.isEqual(firstBytes, secondBytes);
    }

    /** Minimal adapter keeps no servlet state in the helper's public API. */
    public interface HttpServletResponseAdapter {
        void setHeader(String name, String value);
    }
}

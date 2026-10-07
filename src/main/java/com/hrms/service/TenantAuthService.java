package com.hrms.service;

import com.hrms.service.security.TenantPrincipal;
import com.hrms.dao.UserDAO;
import com.hrms.model.TenantAccount;
import com.hrms.service.BCryptPasswordVerifier;

import java.sql.SQLException;
import java.util.Locale;
import java.util.regex.Pattern;

/** Authenticates a tenant user against a workspace and its active subscription. */
public class TenantAuthService {
    private static final Pattern WORKSPACE = Pattern.compile("^[a-z0-9](?:[a-z0-9-]{0,48}[a-z0-9])?$");
    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final int LOCK_MINUTES = 15;

    private final UserDAO userDAO;
    private final BCryptPasswordVerifier passwordVerifier;

    public TenantAuthService() {
        this(new UserDAO(), new BCryptPasswordVerifier());
    }

    public TenantAuthService(UserDAO userDAO, BCryptPasswordVerifier passwordVerifier) {
        this.userDAO = userDAO;
        this.passwordVerifier = passwordVerifier;
    }

    public TenantPrincipal authenticate(String workspaceCode, String email, String rawPassword)
            throws SQLException {
        String workspace = normalizeWorkspace(workspaceCode);
        String normalizedEmail = normalizeEmail(email);
        if (workspace == null || normalizedEmail == null || rawPassword == null || rawPassword.isEmpty()) {
            return null;
        }

        TenantAccount account = userDAO.findTenantAccount(workspace, normalizedEmail);
        if (account == null || !passwordVerifier.matches(rawPassword, account.getUser().getPasswordHash())) {
            if (account != null) {
                userDAO.recordTenantLoginFailure(account.getUser(), MAX_FAILED_ATTEMPTS, LOCK_MINUTES);
            }
            return null;
        }
        if (!userDAO.recordTenantLoginSuccess(account.getUser())) {
            return null;
        }

        return new TenantPrincipal(
                account.getUser().getUserId(), account.getCompanyId(), account.getWorkspaceCode(),
                account.getCompanyName(), account.getUser().getDisplayName(), account.getUser().getEmail(),
                account.getRoleCode(), account.isMustChangePassword());
    }

    private String normalizeWorkspace(String value) {
        if (value == null) return null;
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return WORKSPACE.matcher(normalized).matches() ? normalized : null;
    }

    private String normalizeEmail(String value) {
        if (value == null || value.isBlank()) return null;
        return value.trim().toLowerCase(Locale.ROOT);
    }
}

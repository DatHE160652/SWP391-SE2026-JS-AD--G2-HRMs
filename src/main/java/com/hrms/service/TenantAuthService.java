package com.hrms.service;

import com.hrms.service.security.TenantPrincipal;
import com.hrms.dao.UserDAO;
import com.hrms.model.TenantAccount;
import com.hrms.util.EmailSupport;

import java.sql.SQLException;
import java.util.Locale;
import java.util.regex.Pattern;

/** Authenticates a tenant user against a workspace and its active subscription. */
public class TenantAuthService {
    private static final Pattern WORKSPACE = Pattern.compile("^[a-z0-9](?:[a-z0-9-]{0,48}[a-z0-9])?$");
    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final int LOCK_MINUTES = 15;

    private final UserDAO userDAO;

    public TenantAuthService() {
        this(new UserDAO());
    }

    public TenantAuthService(UserDAO userDAO) {
        this.userDAO = userDAO;
    }

    public TenantPrincipal authenticate(String workspaceCode, String email, String rawPassword)
            throws SQLException {
        String workspace = normalizeWorkspace(workspaceCode);
        String normalizedEmail = EmailSupport.normalize(email);
        if (workspace == null || normalizedEmail == null || rawPassword == null || rawPassword.isEmpty()) {
            return null;
        }

        TenantAccount account = userDAO.findTenantAccount(workspace, normalizedEmail);
        if (account == null || !com.hrms.util.TenantPasswordSupport.verify(rawPassword, account.getUser().getPassword())) {
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
                account.getRoleCode(), account.getEmployeeId(), account.isMustChangePassword(),
                com.hrms.util.SecuritySupport.credentialVersion(account.getUser().getPassword()));
    }

    public boolean changePassword(TenantPrincipal principal, String newPassword) throws SQLException {
        if (principal == null || principal.getUserId() == null || newPassword == null
                || principal.getCompanyId() == null || newPassword.isBlank()) return false;
        TenantAccount account = userDAO.findTenantAccount(principal.getWorkspaceCode(), principal.getEmail());
        if (account == null || !principal.getUserId().equals(account.getUser().getUserId())
                || !principal.getCompanyId().equals(account.getCompanyId())
                || !userDAO.isCurrentTenantSessionAuthorized(principal)) return false;
        if (com.hrms.util.TenantPasswordSupport.verify(newPassword, account.getUser().getPassword())) {
            throw new IllegalArgumentException("Mật khẩu mới phải khác mật khẩu hiện tại.");
        }
        return userDAO.updateTenantPassword(account.getUser(), com.hrms.util.TenantPasswordSupport.hash(newPassword));
    }

    private String normalizeWorkspace(String value) {
        if (value == null) return null;
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return WORKSPACE.matcher(normalized).matches() ? normalized : null;
    }
}

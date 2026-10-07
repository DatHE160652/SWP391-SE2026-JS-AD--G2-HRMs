package com.hrms.service;

import com.hrms.service.security.SystemAdminPrincipal;
import com.hrms.dao.UserDAO;
import com.hrms.model.User;

import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDateTime;

/** Authenticates and revalidates platform System Admin accounts. */
public class SystemAdminAuthService {
    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final int LOCK_MINUTES = 15;
    private final UserDAO userDAO;
    private final BCryptPasswordVerifier passwordVerifier;
    private final Clock clock;

    public SystemAdminAuthService() {
        this(new UserDAO(), new BCryptPasswordVerifier(), Clock.systemUTC());
    }

    public SystemAdminAuthService(UserDAO userDAO, BCryptPasswordVerifier passwordVerifier) {
        this(userDAO, passwordVerifier, Clock.systemUTC());
    }

    SystemAdminAuthService(UserDAO userDAO, BCryptPasswordVerifier passwordVerifier, Clock clock) {
        this.userDAO = userDAO;
        this.passwordVerifier = passwordVerifier;
        this.clock = clock;
    }

    public SystemAdminPrincipal authenticate(String identifier, String rawPassword) throws SQLException {
        if (identifier == null || identifier.isBlank() || rawPassword == null || rawPassword.isEmpty()) {
            return null;
        }
        User user = userDAO.findSystemAdminByIdentifier(identifier.trim());
        if (!isEligible(user)) {
            return null;
        }
        if (!passwordVerifier.matches(rawPassword, user.getPasswordHash())) {
            userDAO.recordSystemAdminLoginFailure(user, MAX_FAILED_ATTEMPTS, LOCK_MINUTES);
            return null;
        }
        if (!userDAO.recordSystemAdminLoginSuccess(user)) {
            return null;
        }
        String displayName = user.getDisplayName() == null || user.getDisplayName().isBlank()
                ? user.getUsername() : user.getDisplayName();
        return new SystemAdminPrincipal(user.getUserId(), displayName, user.getEmail(), user.getRoleCode());
    }

    public boolean isAuthorized(SystemAdminPrincipal principal) throws SQLException {
        return principal != null && principal.isSystemAdmin()
                && principal.getUserId() != null
                && userDAO.isSystemAdminAuthorized(principal.getUserId());
    }

    private boolean isEligible(User user) {
        return user != null
                && "SYSTEM_ADMIN".equals(user.getRoleCode())
                && user.getCompanyId() == null
                && Boolean.TRUE.equals(user.getActive())
                && (user.getLockedUntil() == null
                    ? !Boolean.TRUE.equals(user.getLocked())
                    : !user.getLockedUntil().isAfter(LocalDateTime.now(clock)));
    }
}

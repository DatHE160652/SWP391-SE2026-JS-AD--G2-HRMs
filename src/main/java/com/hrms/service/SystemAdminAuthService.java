package com.hrms.service;

import com.hrms.service.security.SystemAdminPrincipal;
import com.hrms.dao.UserDAO;
import com.hrms.model.User;
import com.hrms.util.EmailSupport;

import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDateTime;

/** Authenticates and revalidates platform System Admin accounts. */
public class SystemAdminAuthService {
    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final int LOCK_MINUTES = 15;
    private final UserDAO userDAO;
    private final Clock clock;

    public SystemAdminAuthService() {
        this(new UserDAO(), Clock.systemUTC());
    }

    public SystemAdminAuthService(UserDAO userDAO) {
        this(userDAO, Clock.systemUTC());
    }

    SystemAdminAuthService(UserDAO userDAO, Clock clock) {
        this.userDAO = userDAO;
        this.clock = clock;
    }

    public SystemAdminPrincipal authenticate(String email, String rawPassword) throws SQLException {
        String normalizedEmail = EmailSupport.normalize(email);
        if (normalizedEmail == null || rawPassword == null || rawPassword.isEmpty()) {
            return null;
        }
        User user = userDAO.findSystemAdminByEmail(normalizedEmail);
        if (!isEligible(user)) {
            return null;
        }
        if (!rawPassword.equals(user.getPassword())) {
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

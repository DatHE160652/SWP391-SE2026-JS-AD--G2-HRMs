package com.hrms.service.security;

import java.io.Serializable;

/** Minimal authenticated identity; never contains a password or password hash. */
public final class SystemAdminPrincipal implements Serializable {
    private static final long serialVersionUID = 1L;

    private final Integer userId;
    private final String displayName;
    private final String email;
    private final String roleCode;

    public SystemAdminPrincipal(Integer userId, String displayName, String email, String roleCode) {
        this.userId = userId;
        this.displayName = displayName;
        this.email = email;
        this.roleCode = roleCode;
    }

    public Integer getUserId() { return userId; }
    public String getDisplayName() { return displayName; }
    public String getEmail() { return email; }
    public String getRoleCode() { return roleCode; }
    public boolean isSystemAdmin() { return "SYSTEM_ADMIN".equals(roleCode); }
}

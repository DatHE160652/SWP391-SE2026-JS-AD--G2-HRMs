package com.hrms.api;

/** Session principal for an authenticated tenant workspace user. */
public final class TenantPrincipal {
    private final Integer userId;
    private final Integer companyId;
    private final String workspaceCode;
    private final String companyName;
    private final String displayName;
    private final String email;
    private final String roleCode;
    private final boolean mustChangePassword;

    public TenantPrincipal(Integer userId, Integer companyId, String workspaceCode,
                           String companyName, String displayName, String email,
                           String roleCode, boolean mustChangePassword) {
        this.userId = userId;
        this.companyId = companyId;
        this.workspaceCode = workspaceCode;
        this.companyName = companyName;
        this.displayName = displayName;
        this.email = email;
        this.roleCode = roleCode;
        this.mustChangePassword = mustChangePassword;
    }

    public Integer getUserId() { return userId; }
    public Integer getCompanyId() { return companyId; }
    public String getWorkspaceCode() { return workspaceCode; }
    public String getCompanyName() { return companyName; }
    public String getDisplayName() { return displayName; }
    public String getEmail() { return email; }
    public String getRoleCode() { return roleCode; }
    public boolean isMustChangePassword() { return mustChangePassword; }
}

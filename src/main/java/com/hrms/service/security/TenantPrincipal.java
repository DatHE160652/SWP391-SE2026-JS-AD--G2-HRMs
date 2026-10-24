package com.hrms.service.security;

/** Session principal for an authenticated tenant workspace user. */
public final class TenantPrincipal {
    private final Integer userId;
    private final Integer companyId;
    private final String workspaceCode;
    private final String companyName;
    private final String displayName;
    private final String email;
    private final String roleCode;
    private final Integer employeeId;
    private final boolean mustChangePassword;
    private final String credentialVersion;

    public TenantPrincipal(Integer userId, Integer companyId, String workspaceCode,
                           String companyName, String displayName, String email,
                           String roleCode, Integer employeeId, boolean mustChangePassword) {
        this(userId,companyId,workspaceCode,companyName,displayName,email,roleCode,employeeId,mustChangePassword,null);
    }

    public TenantPrincipal(Integer userId, Integer companyId, String workspaceCode,
                           String companyName, String displayName, String email,
                           String roleCode, Integer employeeId, boolean mustChangePassword,String credentialVersion) {
        this.userId = userId;
        this.companyId = companyId;
        this.workspaceCode = workspaceCode;
        this.companyName = companyName;
        this.displayName = displayName;
        this.email = email;
        this.roleCode = roleCode;
        this.employeeId = employeeId;
        this.mustChangePassword = mustChangePassword;
        this.credentialVersion = credentialVersion;
    }

    public Integer getUserId() { return userId; }
    public Integer getCompanyId() { return companyId; }
    public String getWorkspaceCode() { return workspaceCode; }
    public String getCompanyName() { return companyName; }
    public String getDisplayName() { return displayName; }
    public String getEmail() { return email; }
    public String getRoleCode() { return roleCode; }
    public boolean isCompanyAdministrator() {
        return "COMPANY_ADMIN".equalsIgnoreCase(roleCode) || "HR".equalsIgnoreCase(roleCode);
    }
    public Integer getEmployeeId() { return employeeId; }
    public boolean isMustChangePassword() { return mustChangePassword; }
    public String getCredentialVersion() { return credentialVersion; }
}

package com.hrms.model;

public final class TenantAccount {
    private final User user;
    private final Integer companyId;
    private final String workspaceCode;
    private final String companyName;
    private final String roleCode;
    private final Integer employeeId;
    private final boolean mustChangePassword;

    public TenantAccount(User user, Integer companyId, String workspaceCode,
                         String companyName, String roleCode, Integer employeeId, boolean mustChangePassword) {
        this.user = user;
        this.companyId = companyId;
        this.workspaceCode = workspaceCode;
        this.companyName = companyName;
        this.roleCode = roleCode;
        this.employeeId = employeeId;
        this.mustChangePassword = mustChangePassword;
    }

    public User getUser() { return user; }
    public Integer getCompanyId() { return companyId; }
    public String getWorkspaceCode() { return workspaceCode; }
    public String getCompanyName() { return companyName; }
    public String getRoleCode() { return roleCode; }
    public Integer getEmployeeId() { return employeeId; }
    public boolean isMustChangePassword() { return mustChangePassword; }
}

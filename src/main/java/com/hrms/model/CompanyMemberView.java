package com.hrms.model;

/** Read-only member summary shown on the System Admin Company detail page. */
public class CompanyMemberView {
    private String displayName;
    private String email;
    private String roleCode;
    private boolean active;

    public CompanyMemberView() {
    }

    public CompanyMemberView(String displayName, String email, String roleCode, boolean active) {
        this.displayName = displayName;
        this.email = email;
        this.roleCode = roleCode;
        this.active = active;
    }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getRoleCode() { return roleCode; }
    public void setRoleCode(String roleCode) { this.roleCode = roleCode; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public String getRoleLabel() {
        return switch (roleCode == null ? "" : roleCode) {
            case "COMPANY_ADMIN" -> "Company Admin";
            case "MANAGER" -> "Manager";
            case "EMPLOYEE" -> "Employee";
            default -> roleCode == null || roleCode.isBlank() ? "—" : roleCode;
        };
    }

    public String getInitials() {
        if (displayName == null || displayName.isBlank()) return "—";
        String[] words = displayName.trim().split("\\s+");
        if (words.length == 1) return words[0].substring(0, 1).toUpperCase(java.util.Locale.ROOT);
        return (words[0].substring(0, 1) + words[words.length - 1].substring(0, 1))
                .toUpperCase(java.util.Locale.ROOT);
    }
}

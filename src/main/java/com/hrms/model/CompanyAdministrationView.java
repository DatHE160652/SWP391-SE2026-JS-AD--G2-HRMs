package com.hrms.model;

import com.hrms.util.TimeSupport;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** Read model for System Admin company and subscription screens. */
public class CompanyAdministrationView {
    private int companyId;
    private String workspaceCode;
    private String companyName;
    private String region;
    private boolean companyActive;
    private String contactEmail;
    private String contactPhone;
    private LocalDateTime companyCreatedAt;
    private String companyAdminName;
    private String companyAdminEmail;
    private LocalDateTime companyAdminActivatedAt;
    private int accountCount;
    private int maxAccounts;
    private int subscriptionId;
    private String subscriptionStatus;
    private String planName;
    private BigDecimal price;
    private String billingCycle;
    private LocalDate subscriptionStartDate;
    private LocalDate subscriptionEndDate;
    private LocalDateTime suspendedAt;
    private String suspensionReason;
    private List<CompanyMemberView> coreMembers = List.of();

    public int getCompanyId() { return companyId; }
    public void setCompanyId(int companyId) { this.companyId = companyId; }
    public String getWorkspaceCode() { return workspaceCode; }
    public void setWorkspaceCode(String workspaceCode) { this.workspaceCode = workspaceCode; }
    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }
    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }
    public boolean isCompanyActive() { return companyActive; }
    public void setCompanyActive(boolean companyActive) { this.companyActive = companyActive; }
    public String getContactEmail() { return contactEmail; }
    public void setContactEmail(String contactEmail) { this.contactEmail = contactEmail; }
    public String getContactPhone() { return contactPhone; }
    public void setContactPhone(String contactPhone) { this.contactPhone = contactPhone; }
    public LocalDateTime getCompanyCreatedAt() { return companyCreatedAt; }
    public void setCompanyCreatedAt(LocalDateTime companyCreatedAt) { this.companyCreatedAt = companyCreatedAt; }
    public String getCompanyAdminName() { return companyAdminName; }
    public void setCompanyAdminName(String companyAdminName) { this.companyAdminName = companyAdminName; }
    public String getCompanyAdminEmail() { return companyAdminEmail; }
    public void setCompanyAdminEmail(String companyAdminEmail) { this.companyAdminEmail = companyAdminEmail; }
    public LocalDateTime getCompanyAdminActivatedAt() { return companyAdminActivatedAt; }
    public void setCompanyAdminActivatedAt(LocalDateTime companyAdminActivatedAt) { this.companyAdminActivatedAt = companyAdminActivatedAt; }
    public int getAccountCount() { return accountCount; }
    public void setAccountCount(int accountCount) { this.accountCount = accountCount; }
    public int getMaxAccounts() { return maxAccounts; }
    public void setMaxAccounts(int maxAccounts) { this.maxAccounts = maxAccounts; }
    public int getSubscriptionId() { return subscriptionId; }
    public void setSubscriptionId(int subscriptionId) { this.subscriptionId = subscriptionId; }
    public String getSubscriptionStatus() { return subscriptionStatus; }
    public void setSubscriptionStatus(String subscriptionStatus) { this.subscriptionStatus = subscriptionStatus; }
    public String getPlanName() { return planName; }
    public void setPlanName(String planName) { this.planName = planName; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public String getBillingCycle() { return billingCycle; }
    public void setBillingCycle(String billingCycle) { this.billingCycle = billingCycle; }
    public LocalDate getSubscriptionStartDate() { return subscriptionStartDate; }
    public void setSubscriptionStartDate(LocalDate subscriptionStartDate) { this.subscriptionStartDate = subscriptionStartDate; }
    public LocalDate getSubscriptionEndDate() { return subscriptionEndDate; }
    public void setSubscriptionEndDate(LocalDate subscriptionEndDate) { this.subscriptionEndDate = subscriptionEndDate; }
    public LocalDateTime getSuspendedAt() { return suspendedAt; }
    public void setSuspendedAt(LocalDateTime suspendedAt) { this.suspendedAt = suspendedAt; }
    public String getSuspensionReason() { return suspensionReason; }
    public void setSuspensionReason(String suspensionReason) { this.suspensionReason = suspensionReason; }
    public List<CompanyMemberView> getCoreMembers() { return coreMembers; }
    public void setCoreMembers(List<CompanyMemberView> coreMembers) { this.coreMembers = coreMembers == null ? List.of() : List.copyOf(coreMembers); }

    public String getCompanyAdminInitial() {
        if (companyAdminName == null || companyAdminName.isBlank()) return "—";
        return companyAdminName.trim().substring(0, 1).toUpperCase(java.util.Locale.ROOT);
    }

    public String getRegionLabel() {
        return switch (region == null ? "" : region) {
            case "NORTH" -> "Miền Bắc";
            case "CENTRAL" -> "Miền Trung";
            case "SOUTH" -> "Miền Nam";
            default -> "—";
        };
    }

    public String getSubscriptionStatusLabel() {
        return "SUSPENDED".equals(subscriptionStatus) ? "TẠM NGƯNG" : "ĐANG HOẠT ĐỘNG";
    }

    public boolean isSubscriptionActive() { return "ACTIVE".equals(subscriptionStatus); }
    public boolean isSubscriptionSuspended() { return "SUSPENDED".equals(subscriptionStatus); }
    public int getAvailableAccounts() { return Math.max(0, maxAccounts - accountCount); }
    public int getAccountUsagePercent() { return maxAccounts <= 0 ? 0 : Math.min(100, accountCount * 100 / maxAccounts); }
    public String getCompanyCreatedAtDisplay() { return TimeSupport.formatUtc(companyCreatedAt); }
    public String getCompanyAdminActivatedAtDisplay() { return TimeSupport.formatUtc(companyAdminActivatedAt); }
    public String getSuspendedAtDisplay() { return TimeSupport.formatUtc(suspendedAt); }
    public String getSubscriptionStartDateDisplay() { return formatDate(subscriptionStartDate); }
    public String getSubscriptionEndDateDisplay() { return formatDate(subscriptionEndDate); }

    private String formatDate(LocalDate date) {
        return date == null ? "Chưa xác định" : date.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }
}

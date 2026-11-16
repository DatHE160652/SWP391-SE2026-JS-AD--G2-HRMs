package com.hrms.model;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.util.Locale;

/** Subscription plan offered by the platform. */
public class Plan {
    private int planId;
    private String planName;
    private int maxAccounts;
    private int maxManagers;
    private BigDecimal price;
    private String billingCycle;
    private boolean active;
    private LocalDateTime createdAt;
    private long activeCompanyCount;

    public int getPlanId() { return planId; }
    public void setPlanId(int planId) { this.planId = planId; }
    public String getPlanName() { return planName; }
    public void setPlanName(String planName) { this.planName = planName; }
    public int getMaxAccounts() { return maxAccounts; }
    public void setMaxAccounts(int maxAccounts) { this.maxAccounts = maxAccounts; }
    public int getMaxManagers() { return maxManagers; }
    public void setMaxManagers(int maxManagers) { this.maxManagers = maxManagers; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public String getBillingCycle() { return billingCycle; }
    public void setBillingCycle(String billingCycle) { this.billingCycle = billingCycle; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public long getActiveCompanyCount() { return activeCompanyCount; }
    public void setActiveCompanyCount(long activeCompanyCount) { this.activeCompanyCount = activeCompanyCount; }

    public String getFormattedPrice() {
        if (price == null) return "0";
        return NumberFormat.getNumberInstance(Locale.US).format(price.longValue());
    }

    public String getBillingCycleLabel() {
        return "YEARLY".equalsIgnoreCase(billingCycle) ? "Năm" : "Tháng";
    }
}

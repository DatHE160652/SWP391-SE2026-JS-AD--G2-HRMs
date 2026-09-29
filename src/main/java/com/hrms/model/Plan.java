package com.hrms.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Plan {
    private Integer planId;
    private String planName;
    private Integer maxEmployees;
    private BigDecimal price;
    private String billingCycle;
    private Boolean active;
    private LocalDateTime createdAt;

    public Integer getPlanId() { return planId; }
    public void setPlanId(Integer planId) { this.planId = planId; }
    public String getPlanName() { return planName; }
    public void setPlanName(String planName) { this.planName = planName; }
    public Integer getMaxEmployees() { return maxEmployees; }
    public void setMaxEmployees(Integer maxEmployees) { this.maxEmployees = maxEmployees; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public String getBillingCycle() { return billingCycle; }
    public void setBillingCycle(String billingCycle) { this.billingCycle = billingCycle; }
    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
    public Boolean getIsActive() { return active; }
    public void setIsActive(Boolean active) { this.active = active; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
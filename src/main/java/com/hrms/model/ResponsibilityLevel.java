package com.hrms.model;

import java.time.LocalDateTime;
import java.math.BigDecimal;

public class ResponsibilityLevel {
    private Integer responsibilityLevelId;
    private BigDecimal defaultAllowance;
    private boolean allowanceSpecified;
    public BigDecimal getDefaultAllowance() { return defaultAllowance; }
    public void setDefaultAllowance(BigDecimal amount) { defaultAllowance=amount; allowanceSpecified=true; }
    public boolean isAllowanceSpecified() { return allowanceSpecified; }
    public void clearAllowanceSpecified() { allowanceSpecified=false; }
    private Integer companyId;
    private String levelCode;
    private String levelName;
    private int rankOrder;
    private boolean active = true;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Integer getResponsibilityLevelId() { return responsibilityLevelId; }
    public void setResponsibilityLevelId(Integer responsibilityLevelId) { this.responsibilityLevelId = responsibilityLevelId; }
    public Integer getCompanyId() { return companyId; }
    public void setCompanyId(Integer companyId) { this.companyId = companyId; }
    public String getLevelCode() { return levelCode; }
    public void setLevelCode(String levelCode) { this.levelCode = levelCode; }
    public String getLevelName() { return levelName; }
    public void setLevelName(String levelName) { this.levelName = levelName; }
    public int getRankOrder() { return rankOrder; }
    public void setRankOrder(int rankOrder) { this.rankOrder = rankOrder; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}

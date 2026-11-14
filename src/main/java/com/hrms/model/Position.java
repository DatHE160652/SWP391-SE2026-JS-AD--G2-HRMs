package com.hrms.model;

import java.time.LocalDateTime;
import java.math.BigDecimal;

public class Position {
    private boolean assignmentOccupied;
    public boolean isAssignmentOccupied() { return assignmentOccupied; }
    public void setAssignmentOccupied(boolean value) { assignmentOccupied=value; }
    private Integer reportsToPositionId;
    private boolean reportingPositionSpecified;
    public Integer getReportsToPositionId() { return reportsToPositionId; }
    public void setReportsToPositionId(Integer value) { reportsToPositionId=value; reportingPositionSpecified=true; }
    public boolean isReportingPositionSpecified() { return reportingPositionSpecified; }
	private Integer positionId;
	private Integer companyId;
	private Integer departmentId;
	private String departmentName;
	private String positionTitle;
	private String description;
	private LocalDateTime createdAt;
	private Integer responsibilityLevelId;
    private BigDecimal positionAllowance;
    private BigDecimal effectiveAllowance;
    private boolean allowanceSpecified;
    public BigDecimal getPositionAllowance() { return positionAllowance; }
    public void setPositionAllowance(BigDecimal amount) { positionAllowance=amount; allowanceSpecified=true; }
    public boolean isAllowanceSpecified() { return allowanceSpecified; }
    public BigDecimal getEffectiveAllowance() { return effectiveAllowance; }
    public void setEffectiveAllowance(BigDecimal amount) { effectiveAllowance=amount; }
	private String responsibilityLevelName;
	private boolean active = true;
	private boolean canManageEmployees;
	private LocalDateTime updatedAt;

	public Integer getResponsibilityLevelId() { return responsibilityLevelId; }
	public void setResponsibilityLevelId(Integer responsibilityLevelId) { this.responsibilityLevelId = responsibilityLevelId; }
	public String getResponsibilityLevelName() { return responsibilityLevelName; }
	public void setResponsibilityLevelName(String responsibilityLevelName) { this.responsibilityLevelName = responsibilityLevelName; }
	public boolean isActive() { return active; }
	public boolean isCanManageEmployees() { return canManageEmployees; }
	public void setCanManageEmployees(boolean canManageEmployees) { this.canManageEmployees = canManageEmployees; }
	public void setActive(boolean active) { this.active = active; }
	public LocalDateTime getUpdatedAt() { return updatedAt; }
	public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

	public Integer getPositionId() { return positionId; }
	public void setPositionId(Integer positionId) { this.positionId = positionId; }
	public Integer getCompanyId() { return companyId; }
	public void setCompanyId(Integer companyId) { this.companyId = companyId; }
	public Integer getDepartmentId() { return departmentId; }
	public void setDepartmentId(Integer departmentId) { this.departmentId = departmentId; }
	public String getDepartmentName() { return departmentName; }
	public void setDepartmentName(String departmentName) { this.departmentName = departmentName; }
	public String getPositionTitle() { return positionTitle; }
	public void setPositionTitle(String positionTitle) { this.positionTitle = positionTitle; }
	public String getDescription() { return description; }
	public void setDescription(String description) { this.description = description; }
	public LocalDateTime getCreatedAt() { return createdAt; }
	public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}

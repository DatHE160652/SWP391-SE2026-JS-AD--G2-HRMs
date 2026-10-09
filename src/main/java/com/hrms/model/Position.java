package com.hrms.model;

import java.time.LocalDateTime;

public class Position {
	private Integer positionId;
	private Integer companyId;
	private Integer departmentId;
	private String departmentName;
	private String positionTitle;
	private String description;
	private LocalDateTime createdAt;

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

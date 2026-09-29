package com.hrms.model;

import java.time.LocalDateTime;

public class Department {
	private Integer departmentId;
	private Integer companyId;
	private String departmentCode;
	private String departmentName;
	private LocalDateTime createdAt;

	public Integer getDepartmentId() { return departmentId; }
	public void setDepartmentId(Integer departmentId) { this.departmentId = departmentId; }
	public Integer getCompanyId() { return companyId; }
	public void setCompanyId(Integer companyId) { this.companyId = companyId; }
	public String getDepartmentCode() { return departmentCode; }
	public void setDepartmentCode(String departmentCode) { this.departmentCode = departmentCode; }
	public String getDepartmentName() { return departmentName; }
	public void setDepartmentName(String departmentName) { this.departmentName = departmentName; }
	public LocalDateTime getCreatedAt() { return createdAt; }
	public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}

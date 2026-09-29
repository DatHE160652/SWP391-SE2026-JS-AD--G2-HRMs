package com.hrms.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class Employee {
	private Integer employeeId;
	private Integer companyId;
	private Integer userId;
	private Integer managerId;
	private Integer departmentId;
	private Integer positionId;
	private String employeeCode;
	private String fullName;
	private String phone;
	private String gender;
	private LocalDate dateOfBirth;
	private LocalDate startDate;
	private LocalDate endDate;
	private BigDecimal baseSalary;
	private String status;
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;

	public Integer getEmployeeId() { return employeeId; }
	public void setEmployeeId(Integer employeeId) { this.employeeId = employeeId; }
	public Integer getCompanyId() { return companyId; }
	public void setCompanyId(Integer companyId) { this.companyId = companyId; }
	public Integer getUserId() { return userId; }
	public void setUserId(Integer userId) { this.userId = userId; }
	public Integer getManagerId() { return managerId; }
	public void setManagerId(Integer managerId) { this.managerId = managerId; }
	public Integer getDepartmentId() { return departmentId; }
	public void setDepartmentId(Integer departmentId) { this.departmentId = departmentId; }
	public Integer getPositionId() { return positionId; }
	public void setPositionId(Integer positionId) { this.positionId = positionId; }
	public String getEmployeeCode() { return employeeCode; }
	public void setEmployeeCode(String employeeCode) { this.employeeCode = employeeCode; }
	public String getFullName() { return fullName; }
	public void setFullName(String fullName) { this.fullName = fullName; }
	public String getPhone() { return phone; }
	public void setPhone(String phone) { this.phone = phone; }
	public String getGender() { return gender; }
	public void setGender(String gender) { this.gender = gender; }
	public LocalDate getDateOfBirth() { return dateOfBirth; }
	public void setDateOfBirth(LocalDate dateOfBirth) { this.dateOfBirth = dateOfBirth; }
	public LocalDate getStartDate() { return startDate; }
	public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
	public LocalDate getEndDate() { return endDate; }
	public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
	public BigDecimal getBaseSalary() { return baseSalary; }
	public void setBaseSalary(BigDecimal baseSalary) { this.baseSalary = baseSalary; }
	public String getStatus() { return status; }
	public void setStatus(String status) { this.status = status; }
	public LocalDateTime getCreatedAt() { return createdAt; }
	public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
	public LocalDateTime getUpdatedAt() { return updatedAt; }
	public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}

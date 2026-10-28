package com.hrms.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Thực thể đại diện cho bảng LeaveBalances trong cơ sở dữ liệu:
 * Quản lý hạn mức ngày phép được cấp (EntitledDays), số ngày đã dùng (UsedDays)
 * và số ngày phép còn lại (RemainingDays - cột tính toán tự động trong SQL Server).
 */
public class LeaveBalance {
	private Integer balanceId;
	private Integer employeeId;
	private Integer leaveTypeId;
	private Integer year;
	private BigDecimal entitledDays;
	private BigDecimal usedDays;
	private BigDecimal remainingDays;
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;

	public Integer getBalanceId() { return balanceId; }
	public void setBalanceId(Integer balanceId) { this.balanceId = balanceId; }
	public Integer getEmployeeId() { return employeeId; }
	public void setEmployeeId(Integer employeeId) { this.employeeId = employeeId; }
	public Integer getLeaveTypeId() { return leaveTypeId; }
	public void setLeaveTypeId(Integer leaveTypeId) { this.leaveTypeId = leaveTypeId; }
	public Integer getYear() { return year; }
	public void setYear(Integer year) { this.year = year; }
	public BigDecimal getEntitledDays() { return entitledDays; }
	public void setEntitledDays(BigDecimal entitledDays) { this.entitledDays = entitledDays; }
	public BigDecimal getUsedDays() { return usedDays; }
	public void setUsedDays(BigDecimal usedDays) { this.usedDays = usedDays; }
	public BigDecimal getRemainingDays() { return remainingDays; }
	public void setRemainingDays(BigDecimal remainingDays) { this.remainingDays = remainingDays; }
	public LocalDateTime getCreatedAt() { return createdAt; }
	public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
	public LocalDateTime getUpdatedAt() { return updatedAt; }
	public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

	// Display helper fields populated from joined queries
	private String leaveTypeCode;
	private String leaveTypeName;
	private Boolean isPaid;
	private Integer defaultDaysPerYear;
	private String employeeCode;
	private String employeeFullName;
	private String departmentName;

	public String getLeaveTypeCode() { return leaveTypeCode; }
	public void setLeaveTypeCode(String leaveTypeCode) { this.leaveTypeCode = leaveTypeCode; }

	public String getLeaveTypeName() { return leaveTypeName; }
	public void setLeaveTypeName(String leaveTypeName) { this.leaveTypeName = leaveTypeName; }

	public Boolean getIsPaid() { return isPaid; }
	public void setIsPaid(Boolean isPaid) { this.isPaid = isPaid; }

	public Integer getDefaultDaysPerYear() { return defaultDaysPerYear; }
	public void setDefaultDaysPerYear(Integer defaultDaysPerYear) { this.defaultDaysPerYear = defaultDaysPerYear; }

	public String getEmployeeCode() { return employeeCode; }
	public void setEmployeeCode(String employeeCode) { this.employeeCode = employeeCode; }

	public String getEmployeeFullName() { return employeeFullName; }
	public void setEmployeeFullName(String employeeFullName) { this.employeeFullName = employeeFullName; }

	public String getDepartmentName() { return departmentName; }
	public void setDepartmentName(String departmentName) { this.departmentName = departmentName; }
}

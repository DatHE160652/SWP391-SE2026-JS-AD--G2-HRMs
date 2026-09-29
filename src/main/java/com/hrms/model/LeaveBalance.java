package com.hrms.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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
}

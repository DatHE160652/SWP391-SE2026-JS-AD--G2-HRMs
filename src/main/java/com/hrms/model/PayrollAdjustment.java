package com.hrms.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class PayrollAdjustment {
	private Long adjustmentId;
	private Integer payrollPeriodId;
	private Integer employeeId;
	private BigDecimal bonus;
	private BigDecimal deduction;
	private String note;
	private Integer createdByUserId;
	private LocalDateTime createdAt;

	public Long getAdjustmentId() { return adjustmentId; }
	public void setAdjustmentId(Long adjustmentId) { this.adjustmentId = adjustmentId; }
	public Integer getPayrollPeriodId() { return payrollPeriodId; }
	public void setPayrollPeriodId(Integer payrollPeriodId) { this.payrollPeriodId = payrollPeriodId; }
	public Integer getEmployeeId() { return employeeId; }
	public void setEmployeeId(Integer employeeId) { this.employeeId = employeeId; }
	public BigDecimal getBonus() { return bonus; }
	public void setBonus(BigDecimal bonus) { this.bonus = bonus; }
	public BigDecimal getDeduction() { return deduction; }
	public void setDeduction(BigDecimal deduction) { this.deduction = deduction; }
	public String getNote() { return note; }
	public void setNote(String note) { this.note = note; }
	public Integer getCreatedByUserId() { return createdByUserId; }
	public void setCreatedByUserId(Integer createdByUserId) { this.createdByUserId = createdByUserId; }
	public LocalDateTime getCreatedAt() { return createdAt; }
	public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}

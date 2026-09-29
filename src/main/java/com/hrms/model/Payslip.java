package com.hrms.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Payslip {
	private Long payslipId;
	private Integer payrollPeriodId;
	private Integer employeeId;
	private BigDecimal baseSalary;
	private BigDecimal bonus;
	private BigDecimal deduction;
	private BigDecimal netSalary;
	private LocalDateTime generatedAt;

	public Long getPayslipId() { return payslipId; }
	public void setPayslipId(Long payslipId) { this.payslipId = payslipId; }
	public Integer getPayrollPeriodId() { return payrollPeriodId; }
	public void setPayrollPeriodId(Integer payrollPeriodId) { this.payrollPeriodId = payrollPeriodId; }
	public Integer getEmployeeId() { return employeeId; }
	public void setEmployeeId(Integer employeeId) { this.employeeId = employeeId; }
	public BigDecimal getBaseSalary() { return baseSalary; }
	public void setBaseSalary(BigDecimal baseSalary) { this.baseSalary = baseSalary; }
	public BigDecimal getBonus() { return bonus; }
	public void setBonus(BigDecimal bonus) { this.bonus = bonus; }
	public BigDecimal getDeduction() { return deduction; }
	public void setDeduction(BigDecimal deduction) { this.deduction = deduction; }
	public BigDecimal getNetSalary() { return netSalary; }
	public void setNetSalary(BigDecimal netSalary) { this.netSalary = netSalary; }
	public LocalDateTime getGeneratedAt() { return generatedAt; }
	public void setGeneratedAt(LocalDateTime generatedAt) { this.generatedAt = generatedAt; }
}

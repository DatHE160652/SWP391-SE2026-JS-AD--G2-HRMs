package com.hrms.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PayrollSlipView(long payslipId, int payrollPeriodId, int employeeId,
                              String employeeCode, String fullName, String companyName,
                              String periodName, int month, int year, BigDecimal baseSalary,
                              BigDecimal bonus, BigDecimal deduction, BigDecimal netSalary,
                              LocalDateTime generatedAt, LocalDateTime publishedAt,
                              BigDecimal responsibilityAllowance, String periodStatus,
                              BigDecimal contractBaseSalary, BigDecimal actualWorkDays,
                              BigDecimal standardWorkDays, BigDecimal travelAllowance) {

    public PayrollSlipView(long payslipId, int payrollPeriodId, int employeeId,
                           String employeeCode, String fullName, String companyName,
                           String periodName, int month, int year, BigDecimal baseSalary,
                           BigDecimal bonus, BigDecimal deduction, BigDecimal netSalary,
                           LocalDateTime generatedAt, LocalDateTime publishedAt,
                           BigDecimal responsibilityAllowance, String periodStatus) {
        this(payslipId, payrollPeriodId, employeeId, employeeCode, fullName, companyName,
                periodName, month, year, baseSalary, bonus, deduction, netSalary,
                generatedAt, publishedAt, responsibilityAllowance, periodStatus,
                baseSalary, null, null, BigDecimal.ZERO);
    }
}

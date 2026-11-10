package com.hrms.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Tenant-scoped employee salary input or immutable calculated snapshot. */
public record PayrollRow(int employeeId, String employeeCode, String fullName,
                String departmentName, String status, BigDecimal baseSalary,
                BigDecimal bonus, BigDecimal deduction, BigDecimal netSalary,
                String note, Long payslipId, BigDecimal responsibilityAllowance,
                String salaryIssue, LocalDate salaryEffectiveFrom, LocalDate salaryEffectiveTo,
                BigDecimal contractBaseSalary, BigDecimal actualWorkDays,
                BigDecimal standardWorkDays, BigDecimal travelAllowance) {

        public PayrollRow(int employeeId, String employeeCode, String fullName,
                        String departmentName, String status, BigDecimal baseSalary,
                        BigDecimal bonus, BigDecimal deduction, BigDecimal netSalary,
                        String note, Long payslipId, BigDecimal responsibilityAllowance,
                        String salaryIssue, LocalDate salaryEffectiveFrom, LocalDate salaryEffectiveTo) {
                this(employeeId, employeeCode, fullName, departmentName, status, baseSalary, bonus, deduction,
                                netSalary, note, payslipId, responsibilityAllowance, salaryIssue, salaryEffectiveFrom,
                                salaryEffectiveTo,
                                baseSalary, null, null, BigDecimal.ZERO);
        }

        public PayrollRow(int employeeId, String employeeCode, String fullName, String departmentName,
                        String status, BigDecimal baseSalary, BigDecimal bonus, BigDecimal deduction,
                        BigDecimal netSalary, String note, Long payslipId) {
                this(employeeId, employeeCode, fullName, departmentName, status, baseSalary, bonus, deduction,
                                netSalary, note, payslipId, BigDecimal.ZERO, null, null, null,
                                baseSalary, null, null, BigDecimal.ZERO);
        }
}

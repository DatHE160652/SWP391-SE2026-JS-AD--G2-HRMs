package com.hrms.model;

import java.math.BigDecimal;

public record PayrollImportInput(
    int rowNumber,
    String employeeCode,
    String fullName,
    BigDecimal contractBaseSalary,
    BigDecimal actualWorkDays,
    BigDecimal standardWorkDays,
    BigDecimal baseSalary,
    BigDecimal responsibilityAllowance,
    BigDecimal travelAllowance,
    BigDecimal bonus,
    BigDecimal netSalary,
    String note
) { }


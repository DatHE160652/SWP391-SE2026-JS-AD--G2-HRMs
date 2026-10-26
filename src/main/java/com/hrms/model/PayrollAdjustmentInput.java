package com.hrms.model;

import java.math.BigDecimal;

public record PayrollAdjustmentInput(int rowNumber, String employeeCode, BigDecimal bonus,
                                     BigDecimal deduction, String note) { }

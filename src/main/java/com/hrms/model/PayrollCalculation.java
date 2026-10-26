package com.hrms.model;

import java.math.BigDecimal;
import java.util.List;

/** Validated, unsaved monthly result; persisted NetSalary remains a SQL computed column. */
public record PayrollCalculation(List<PayrollRow> rows, BigDecimal totalIncome,
                                 BigDecimal totalDeduction, BigDecimal totalNetSalary) {
    public PayrollCalculation {
        rows=List.copyOf(rows);
    }
}

package com.hrms.model;

import java.math.BigDecimal;

public record EmployeeCompensation(BigDecimal grossSalary,
        BigDecimal positionAllowance, BigDecimal travelAllowance) {
    public boolean provided() {
        return grossSalary != null || positionAllowance != null
                || travelAllowance != null;
    }
}

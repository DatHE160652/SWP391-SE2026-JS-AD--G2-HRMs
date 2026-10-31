package com.hrms.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public record EmploymentContract(Long contractId, int employeeId, String contractCode, String contractType,
        LocalDate startDate, LocalDate endDate, BigDecimal grossSalary, String notes, String expectedUpdatedAt,
        BigDecimal travelAllowance) {
    public EmploymentContract(Long contractId, int employeeId, String contractCode, String contractType,
            LocalDate startDate, LocalDate endDate, BigDecimal grossSalary, String notes, String expectedUpdatedAt) {
        this(contractId, employeeId, contractCode, contractType, startDate, endDate, grossSalary, notes, expectedUpdatedAt, null);
    }
}

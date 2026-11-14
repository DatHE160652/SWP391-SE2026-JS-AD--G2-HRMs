package com.hrms.model;

import java.math.BigDecimal;

public record PayrollFormulaSettings(
        int companyId,
        String salaryCalculationMode,
        String standardDaysMode,
        BigDecimal fixedStandardDays,
        String weekdayWeights,
        boolean includePaidLeave,
        boolean includeResponsibilityAllowance,
        boolean includeTravelAllowance
) {
    public PayrollFormulaSettings(
            int companyId,
            String salaryCalculationMode,
            String standardDaysMode,
            BigDecimal fixedStandardDays,
            boolean includePaidLeave,
            boolean includeResponsibilityAllowance,
            boolean includeTravelAllowance
    ) {
        this(companyId, salaryCalculationMode, standardDaysMode, fixedStandardDays,
                defaultWeightsForMode(standardDaysMode),
                includePaidLeave, includeResponsibilityAllowance, includeTravelAllowance);
    }

    public static String defaultWeightsForMode(String mode) {
        if ("MON_SAT".equalsIgnoreCase(mode)) return "1,1,1,1,1,1,0";
        if ("MON_SAT_HALF".equalsIgnoreCase(mode)) return "1,1,1,1,1,0.5,0";
        return "1,1,1,1,1,0,0";
    }

    public static PayrollFormulaSettings defaults(int companyId) {
        return new PayrollFormulaSettings(
                companyId,
                "WORKDAYS",
                "MON_FRI",
                new BigDecimal("22.00"),
                "1,1,1,1,1,0,0",
                true,
                true,
                true
        );
    }
}


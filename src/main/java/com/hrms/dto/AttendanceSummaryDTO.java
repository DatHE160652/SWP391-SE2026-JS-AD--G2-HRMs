package com.hrms.dto;

import java.math.BigDecimal;

public class AttendanceSummaryDTO {
    private int totalWorkingDays;
    private BigDecimal totalHours;
    private int presentDays;
    private int lateDays;
    private int earlyLeaveDays;
    private int absentDays;
    private int onLeaveDays;

    public AttendanceSummaryDTO() {
        this.totalWorkingDays = 0;
        this.totalHours = BigDecimal.ZERO;
        this.presentDays = 0;
        this.lateDays = 0;
        this.earlyLeaveDays = 0;
        this.absentDays = 0;
        this.onLeaveDays = 0;
    }

    public int getTotalWorkingDays() { return totalWorkingDays; }
    public void setTotalWorkingDays(int totalWorkingDays) { this.totalWorkingDays = totalWorkingDays; }

    public BigDecimal getTotalHours() { return totalHours; }
    public void setTotalHours(BigDecimal totalHours) { this.totalHours = totalHours; }

    public int getPresentDays() { return presentDays; }
    public void setPresentDays(int presentDays) { this.presentDays = presentDays; }

    public int getLateDays() { return lateDays; }
    public void setLateDays(int lateDays) { this.lateDays = lateDays; }

    public int getEarlyLeaveDays() { return earlyLeaveDays; }
    public void setEarlyLeaveDays(int earlyLeaveDays) { this.earlyLeaveDays = earlyLeaveDays; }

    public int getAbsentDays() { return absentDays; }
    public void setAbsentDays(int absentDays) { this.absentDays = absentDays; }

    public int getOnLeaveDays() { return onLeaveDays; }
    public void setOnLeaveDays(int onLeaveDays) { this.onLeaveDays = onLeaveDays; }
}

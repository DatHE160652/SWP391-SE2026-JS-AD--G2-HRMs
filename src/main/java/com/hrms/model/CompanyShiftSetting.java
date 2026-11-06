package com.hrms.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Model class for CompanyShiftSettings table.
 * Manages tenant-specific shift working hours, lunch break, grace period, and work credit thresholds.
 */
public class CompanyShiftSetting {
    public static final LocalTime DEFAULT_SHIFT_START = LocalTime.of(8, 30);
    public static final LocalTime DEFAULT_SHIFT_END = LocalTime.of(17, 30);
    public static final int DEFAULT_LUNCH_BREAK_MINUTES = 60;
    public static final int DEFAULT_GRACE_PERIOD_MINUTES = 0;
    public static final int DEFAULT_CUTOFF_DAY = 5;
    public static final BigDecimal DEFAULT_MIN_HOURS_HALF_DAY = new BigDecimal("4.00");
    public static final BigDecimal DEFAULT_MIN_HOURS_FULL_DAY = new BigDecimal("6.00");

    private int companyId;
    private LocalTime shiftStartTime = DEFAULT_SHIFT_START;
    private LocalTime shiftEndTime = DEFAULT_SHIFT_END;
    private int lunchBreakMinutes = DEFAULT_LUNCH_BREAK_MINUTES;
    private int gracePeriodMinutes = DEFAULT_GRACE_PERIOD_MINUTES;
    private int timesheetCutoffDay = DEFAULT_CUTOFF_DAY;
    private BigDecimal minHoursHalfDay = DEFAULT_MIN_HOURS_HALF_DAY;
    private BigDecimal minHoursFullDay = DEFAULT_MIN_HOURS_FULL_DAY;
    private LocalDateTime updatedAt;
    private Integer updatedByUserId;
    private String updatedByName;

    public CompanyShiftSetting() {
    }

    public CompanyShiftSetting(int companyId) {
        this.companyId = companyId;
    }

    public CompanyShiftSetting(int companyId, LocalTime shiftStartTime, LocalTime shiftEndTime,
                               int lunchBreakMinutes, int gracePeriodMinutes) {
        this.companyId = companyId;
        this.shiftStartTime = shiftStartTime != null ? shiftStartTime : DEFAULT_SHIFT_START;
        this.shiftEndTime = shiftEndTime != null ? shiftEndTime : DEFAULT_SHIFT_END;
        this.lunchBreakMinutes = lunchBreakMinutes;
        this.gracePeriodMinutes = gracePeriodMinutes;
    }

    public CompanyShiftSetting(int companyId, LocalTime shiftStartTime, LocalTime shiftEndTime,
                               int lunchBreakMinutes, int gracePeriodMinutes,
                               BigDecimal minHoursHalfDay, BigDecimal minHoursFullDay) {
        this(companyId, shiftStartTime, shiftEndTime, lunchBreakMinutes, gracePeriodMinutes);
        this.minHoursHalfDay = minHoursHalfDay != null ? minHoursHalfDay : DEFAULT_MIN_HOURS_HALF_DAY;
        this.minHoursFullDay = minHoursFullDay != null ? minHoursFullDay : DEFAULT_MIN_HOURS_FULL_DAY;
    }

    public int getCompanyId() {
        return companyId;
    }

    public void setCompanyId(int companyId) {
        this.companyId = companyId;
    }

    public LocalTime getShiftStartTime() {
        return shiftStartTime;
    }

    public void setShiftStartTime(LocalTime shiftStartTime) {
        this.shiftStartTime = shiftStartTime;
    }

    public LocalTime getShiftEndTime() {
        return shiftEndTime;
    }

    public void setShiftEndTime(LocalTime shiftEndTime) {
        this.shiftEndTime = shiftEndTime;
    }

    public int getLunchBreakMinutes() {
        return lunchBreakMinutes;
    }

    public void setLunchBreakMinutes(int lunchBreakMinutes) {
        this.lunchBreakMinutes = lunchBreakMinutes;
    }

    public int getGracePeriodMinutes() {
        return gracePeriodMinutes;
    }

    public void setGracePeriodMinutes(int gracePeriodMinutes) {
        this.gracePeriodMinutes = gracePeriodMinutes;
    }

    public int getTimesheetCutoffDay() {
        return timesheetCutoffDay > 0 ? timesheetCutoffDay : DEFAULT_CUTOFF_DAY;
    }

    public void setTimesheetCutoffDay(int timesheetCutoffDay) {
        this.timesheetCutoffDay = timesheetCutoffDay > 0 ? timesheetCutoffDay : DEFAULT_CUTOFF_DAY;
    }

    public BigDecimal getMinHoursHalfDay() {
        return minHoursHalfDay != null ? minHoursHalfDay : DEFAULT_MIN_HOURS_HALF_DAY;
    }

    public void setMinHoursHalfDay(BigDecimal minHoursHalfDay) {
        this.minHoursHalfDay = minHoursHalfDay != null ? minHoursHalfDay : DEFAULT_MIN_HOURS_HALF_DAY;
    }

    public BigDecimal getMinHoursFullDay() {
        return minHoursFullDay != null ? minHoursFullDay : DEFAULT_MIN_HOURS_FULL_DAY;
    }

    public void setMinHoursFullDay(BigDecimal minHoursFullDay) {
        this.minHoursFullDay = minHoursFullDay != null ? minHoursFullDay : DEFAULT_MIN_HOURS_FULL_DAY;
    }

    /**
     * Calculates the work credit (ngày công: 0.0, 0.5, or 1.0) based on total work hours
     * according to this company's configured thresholds.
     */
    public BigDecimal calculateWorkCredit(BigDecimal hours) {
        if (hours == null) return BigDecimal.ZERO;
        BigDecimal full = getMinHoursFullDay();
        BigDecimal half = getMinHoursHalfDay();
        if (hours.compareTo(full) >= 0) return BigDecimal.valueOf(1.0);
        if (hours.compareTo(half) >= 0) return BigDecimal.valueOf(0.5);
        return BigDecimal.ZERO;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Integer getUpdatedByUserId() {
        return updatedByUserId;
    }

    public void setUpdatedByUserId(Integer updatedByUserId) {
        this.updatedByUserId = updatedByUserId;
    }

    public String getUpdatedByName() {
        return updatedByName;
    }

    public void setUpdatedByName(String updatedByName) {
        this.updatedByName = updatedByName;
    }

    /**
     * Calculates the effective deadline after which check-in is considered "Late".
     */
    public LocalTime getLateThreshold() {
        if (shiftStartTime == null) return DEFAULT_SHIFT_START;
        return gracePeriodMinutes > 0 ? shiftStartTime.plusMinutes(gracePeriodMinutes) : shiftStartTime;
    }

    @Override
    public String toString() {
        return "CompanyShiftSetting{" +
                "companyId=" + companyId +
                ", shiftStartTime=" + shiftStartTime +
                ", shiftEndTime=" + shiftEndTime +
                ", lunchBreakMinutes=" + lunchBreakMinutes +
                ", gracePeriodMinutes=" + gracePeriodMinutes +
                ", updatedAt=" + updatedAt +
                ", updatedByUserId=" + updatedByUserId +
                '}';
    }
}

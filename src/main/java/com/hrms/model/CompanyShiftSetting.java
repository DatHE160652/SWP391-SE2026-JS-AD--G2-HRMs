package com.hrms.model;

import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Model class for CompanyShiftSettings table.
 * Manages tenant-specific shift working hours, lunch break, and grace period.
 */
public class CompanyShiftSetting {
    public static final LocalTime DEFAULT_SHIFT_START = LocalTime.of(8, 30);
    public static final LocalTime DEFAULT_SHIFT_END = LocalTime.of(17, 30);
    public static final int DEFAULT_LUNCH_BREAK_MINUTES = 60;
    public static final int DEFAULT_GRACE_PERIOD_MINUTES = 0;

    private int companyId;
    private LocalTime shiftStartTime = DEFAULT_SHIFT_START;
    private LocalTime shiftEndTime = DEFAULT_SHIFT_END;
    private int lunchBreakMinutes = DEFAULT_LUNCH_BREAK_MINUTES;
    private int gracePeriodMinutes = DEFAULT_GRACE_PERIOD_MINUTES;
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

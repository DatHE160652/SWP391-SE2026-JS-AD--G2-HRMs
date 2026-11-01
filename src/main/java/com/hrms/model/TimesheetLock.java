package com.hrms.model;

import java.time.LocalDateTime;

/**
 * Model representing a Monthly Timesheet Lock record (UC88).
 * Ensures attendance data for a locked month cannot be modified before payroll calculation.
 */
public class TimesheetLock {
    private int lockId;
    private int companyId;
    private int month;
    private int year;
    private boolean isLocked;
    private int lockedByUserId;
    private LocalDateTime lockedAt;
    private String notes;

    // Transient fields for display
    private String lockedByUserName;

    public TimesheetLock() {
        this.isLocked = true;
        this.lockedAt = LocalDateTime.now();
    }

    public TimesheetLock(int companyId, int month, int year, boolean isLocked, int lockedByUserId, String notes) {
        this();
        this.companyId = companyId;
        this.month = month;
        this.year = year;
        this.isLocked = isLocked;
        this.lockedByUserId = lockedByUserId;
        this.notes = notes;
    }

    public int getLockId() {
        return lockId;
    }

    public void setLockId(int lockId) {
        this.lockId = lockId;
    }

    public int getCompanyId() {
        return companyId;
    }

    public void setCompanyId(int companyId) {
        this.companyId = companyId;
    }

    public int getMonth() {
        return month;
    }

    public void setMonth(int month) {
        this.month = month;
    }

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public boolean isLocked() {
        return isLocked;
    }

    public void setLocked(boolean locked) {
        isLocked = locked;
    }

    public int getLockedByUserId() {
        return lockedByUserId;
    }

    public void setLockedByUserId(int lockedByUserId) {
        this.lockedByUserId = lockedByUserId;
    }

    public LocalDateTime getLockedAt() {
        return lockedAt;
    }

    public void setLockedAt(LocalDateTime lockedAt) {
        this.lockedAt = lockedAt;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getLockedByUserName() {
        return lockedByUserName;
    }

    public void setLockedByUserName(String lockedByUserName) {
        this.lockedByUserName = lockedByUserName;
    }
}


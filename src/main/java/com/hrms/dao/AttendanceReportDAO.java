package com.hrms.dao;

import java.math.BigDecimal;

public class AttendanceReportDAO {
    private Integer employeeId;
    private String employeeCode;
    private String fullName;
    private String departmentName;
    private String positionTitle;
    private int totalWorkDays;
    private BigDecimal totalHours;
    private int presentCount;
    private int lateCount;
    private int earlyLeaveCount;
    private int absentCount;
    private int onLeaveCount;
    private double punctualityRate;

    public AttendanceReportDAO() {
        this.totalHours = BigDecimal.ZERO;
    }

    public Integer getEmployeeId() { return employeeId; }
    public void setEmployeeId(Integer employeeId) { this.employeeId = employeeId; }

    public String getEmployeeCode() { return employeeCode; }
    public void setEmployeeCode(String employeeCode) { this.employeeCode = employeeCode; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getDepartmentName() { return departmentName; }
    public void setDepartmentName(String departmentName) { this.departmentName = departmentName; }

    public String getPositionTitle() { return positionTitle; }
    public void setPositionTitle(String positionTitle) { this.positionTitle = positionTitle; }

    public int getTotalWorkDays() { return totalWorkDays; }
    public void setTotalWorkDays(int totalWorkDays) { this.totalWorkDays = totalWorkDays; }

    public BigDecimal getTotalHours() { return totalHours; }
    public void setTotalHours(BigDecimal totalHours) { this.totalHours = totalHours; }

    public int getPresentCount() { return presentCount; }
    public void setPresentCount(int presentCount) { this.presentCount = presentCount; }

    public int getLateCount() { return lateCount; }
    public void setLateCount(int lateCount) { this.lateCount = lateCount; }

    public int getEarlyLeaveCount() { return earlyLeaveCount; }
    public void setEarlyLeaveCount(int earlyLeaveCount) { this.earlyLeaveCount = earlyLeaveCount; }

    public int getAbsentCount() { return absentCount; }
    public void setAbsentCount(int absentCount) { this.absentCount = absentCount; }

    public int getOnLeaveCount() { return onLeaveCount; }
    public void setOnLeaveCount(int onLeaveCount) { this.onLeaveCount = onLeaveCount; }

    public double getPunctualityRate() { return punctualityRate; }
    public void setPunctualityRate(double punctualityRate) { this.punctualityRate = punctualityRate; }
}

package com.hrms.model;

import java.time.LocalDateTime;

public class PayrollPeriod {
    private Integer payrollPeriodId;
    private Integer companyId;
    private String periodName;
    private Integer month;
    private Integer year;
    private String status;
    private Integer approvedByUserId;
    private LocalDateTime approvedAt;
    private LocalDateTime createdAt;

    public Integer getPayrollPeriodId() { return payrollPeriodId; }
    public void setPayrollPeriodId(Integer payrollPeriodId) { this.payrollPeriodId = payrollPeriodId; }
    public Integer getCompanyId() { return companyId; }
    public void setCompanyId(Integer companyId) { this.companyId = companyId; }
    public String getPeriodName() { return periodName; }
    public void setPeriodName(String periodName) { this.periodName = periodName; }
    public Integer getMonth() { return month; }
    public void setMonth(Integer month) { this.month = month; }
    public Integer getYear() { return year; }
    public void setYear(Integer year) { this.year = year; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Integer getApprovedByUserId() { return approvedByUserId; }
    public void setApprovedByUserId(Integer approvedByUserId) { this.approvedByUserId = approvedByUserId; }
    public LocalDateTime getApprovedAt() { return approvedAt; }
    public void setApprovedAt(LocalDateTime approvedAt) { this.approvedAt = approvedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
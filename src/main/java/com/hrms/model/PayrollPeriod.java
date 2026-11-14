package com.hrms.model;

import java.time.LocalDateTime;

public class PayrollPeriod {
    private Integer payrollPeriodId;
    private Integer companyId;
    private String periodName;
    private Integer month;
    private Integer year;
    private String status;
    private Integer reviewedByUserId;
    private LocalDateTime reviewedAt;
    private String reviewedByName;
    private Integer approvedByUserId;
    private LocalDateTime approvedAt;
    private String approvedByName;
    private LocalDateTime createdAt;
    private boolean closed;
    private LocalDateTime publishedAt;
    private int employeeCount;
    private java.math.BigDecimal totalNetSalary;
    private java.math.BigDecimal totalIncome;
    private java.math.BigDecimal totalDeduction;

    public Integer getReviewedByUserId() { return reviewedByUserId; }
    public void setReviewedByUserId(Integer reviewedByUserId) { this.reviewedByUserId = reviewedByUserId; }
    public LocalDateTime getReviewedAt() { return reviewedAt; }
    public void setReviewedAt(LocalDateTime reviewedAt) { this.reviewedAt = reviewedAt; }
    public String getReviewedByName() { return reviewedByName; }
    public void setReviewedByName(String reviewedByName) { this.reviewedByName = reviewedByName; }
    public String getApprovedByName() { return approvedByName; }
    public void setApprovedByName(String approvedByName) { this.approvedByName = approvedByName; }
    public java.math.BigDecimal getTotalIncome() { return totalIncome; }
    public void setTotalIncome(java.math.BigDecimal totalIncome) { this.totalIncome = totalIncome; }
    public java.math.BigDecimal getTotalDeduction() { return totalDeduction; }
    public void setTotalDeduction(java.math.BigDecimal totalDeduction) { this.totalDeduction = totalDeduction; }

    public boolean isClosed() { return closed; }
    public void setClosed(boolean closed) { this.closed = closed; }
    public LocalDateTime getPublishedAt() { return publishedAt; }
    public void setPublishedAt(LocalDateTime publishedAt) { this.publishedAt = publishedAt; }
    public int getEmployeeCount() { return employeeCount; }
    public void setEmployeeCount(int employeeCount) { this.employeeCount = employeeCount; }
    public java.math.BigDecimal getTotalNetSalary() { return totalNetSalary; }
    public void setTotalNetSalary(java.math.BigDecimal totalNetSalary) { this.totalNetSalary = totalNetSalary; }

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

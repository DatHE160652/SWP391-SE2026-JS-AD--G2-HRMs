package com.hrms.dao;

import java.math.BigDecimal;

/**
 * Đối tượng vận chuyển dữ liệu (DTO) tổng hợp báo cáo tình hình sử dụng ngày phép của nhân viên:
 * Bao gồm hạn mức được cấp, số ngày đã dùng, số ngày còn lại, phân bổ phép có/không hưởng lương
 * và tổng số lượng đơn theo từng trạng thái.
 */
public class LeaveReportDTO {
    private int employeeId;
    private String employeeCode;
    private String fullName;
    private String departmentName;
    private String positionTitle;
    private BigDecimal entitledDays = BigDecimal.ZERO;
    private BigDecimal usedDays = BigDecimal.ZERO;
    private BigDecimal remainingDays = BigDecimal.ZERO;
    private BigDecimal paidDays = BigDecimal.ZERO;
    private BigDecimal unpaidDays = BigDecimal.ZERO;
    private int totalRequests = 0;
    private int approvedRequests = 0;
    private int pendingRequests = 0;
    private int rejectedRequests = 0;

    public int getEmployeeId() { return employeeId; }
    public void setEmployeeId(int employeeId) { this.employeeId = employeeId; }

    public String getEmployeeCode() { return employeeCode; }
    public void setEmployeeCode(String employeeCode) { this.employeeCode = employeeCode; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getDepartmentName() { return departmentName; }
    public void setDepartmentName(String departmentName) { this.departmentName = departmentName; }

    public String getPositionTitle() { return positionTitle; }
    public void setPositionTitle(String positionTitle) { this.positionTitle = positionTitle; }

    public BigDecimal getEntitledDays() { return entitledDays; }
    public void setEntitledDays(BigDecimal entitledDays) { this.entitledDays = entitledDays; }

    public BigDecimal getUsedDays() { return usedDays; }
    public void setUsedDays(BigDecimal usedDays) { this.usedDays = usedDays; }

    public BigDecimal getRemainingDays() { return remainingDays; }
    public void setRemainingDays(BigDecimal remainingDays) { this.remainingDays = remainingDays; }

    public BigDecimal getPaidDays() { return paidDays; }
    public void setPaidDays(BigDecimal paidDays) { this.paidDays = paidDays; }

    public BigDecimal getUnpaidDays() { return unpaidDays; }
    public void setUnpaidDays(BigDecimal unpaidDays) { this.unpaidDays = unpaidDays; }

    public int getTotalRequests() { return totalRequests; }
    public void setTotalRequests(int totalRequests) { this.totalRequests = totalRequests; }

    public int getApprovedRequests() { return approvedRequests; }
    public void setApprovedRequests(int approvedRequests) { this.approvedRequests = approvedRequests; }

    public int getPendingRequests() { return pendingRequests; }
    public void setPendingRequests(int pendingRequests) { this.pendingRequests = pendingRequests; }

    public int getRejectedRequests() { return rejectedRequests; }
    public void setRejectedRequests(int rejectedRequests) { this.rejectedRequests = rejectedRequests; }
}

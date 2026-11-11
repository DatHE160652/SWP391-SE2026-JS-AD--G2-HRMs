package com.hrms.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** Dates are authoritative; StartDate/EndDate are only the display bounds. */
public class LeaveRequest {
    private Integer leaveRequestId, companyId, employeeId, approverId, managerId, requesterUserId;
    private LocalDate startDate, endDate;
    private BigDecimal totalDays;
    private String reason, status, approverComment, employeeCode, employeeFullName, departmentName,
            positionTitle, requesterRole, approverDisplayName;
    private LocalDateTime reviewedAt, createdAt;
    private boolean legacy;
    private List<LocalDate> dates = List.of();

    public Integer getLeaveRequestId() { return leaveRequestId; }
    public void setLeaveRequestId(Integer value) { leaveRequestId = value; }
    public Integer getCompanyId() { return companyId; }
    public void setCompanyId(Integer value) { companyId = value; }
    public Integer getEmployeeId() { return employeeId; }
    public void setEmployeeId(Integer value) { employeeId = value; }
    public Integer getApproverId() { return approverId; }
    public void setApproverId(Integer value) { approverId = value; }
    public Integer getManagerId() { return managerId; }
    public void setManagerId(Integer value) { managerId = value; }
    public Integer getRequesterUserId() { return requesterUserId; }
    public void setRequesterUserId(Integer value) { requesterUserId = value; }
    public String getRequesterRole() { return requesterRole; }
    public void setRequesterRole(String value) { requesterRole = value; }
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate value) { startDate = value; }
    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate value) { endDate = value; }
    public BigDecimal getTotalDays() { return totalDays; }
    public void setTotalDays(BigDecimal value) { totalDays = value; }
    public String getReason() { return reason; }
    public void setReason(String value) { reason = value; }
    public String getStatus() { return status; }
    public void setStatus(String value) { status = value; }
    public String getApproverComment() { return approverComment; }
    public void setApproverComment(String value) { approverComment = value; }
    public String getEmployeeCode() { return employeeCode; }
    public void setEmployeeCode(String value) { employeeCode = value; }
    public String getEmployeeFullName() { return employeeFullName; }
    public void setEmployeeFullName(String value) { employeeFullName = value; }
    public String getDepartmentName() { return departmentName; }
    public void setDepartmentName(String value) { departmentName = value; }
    public String getPositionTitle() { return positionTitle; }
    public void setPositionTitle(String value) { positionTitle = value; }
    public String getApproverDisplayName() { return approverDisplayName; }
    public void setApproverDisplayName(String value) { approverDisplayName = value; }
    public LocalDateTime getReviewedAt() { return reviewedAt; }
    public void setReviewedAt(LocalDateTime value) { reviewedAt = value; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime value) { createdAt = value; }
    public boolean isLegacy() { return legacy; }
    public void setLegacy(boolean value) { legacy = value; }
    public List<LocalDate> getDates() { return dates; }
    public void setDates(List<LocalDate> value) { dates = List.copyOf(value); }
    public String getTitle() { return "Đơn nghỉ phép #" + leaveRequestId; }
}

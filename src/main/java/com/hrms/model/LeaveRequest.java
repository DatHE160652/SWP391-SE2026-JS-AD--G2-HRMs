package com.hrms.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class LeaveRequest {
	private Integer leaveRequestId;
	private Integer companyId;
	private Integer employeeId;
	private Integer leaveTypeId;
	private LocalDate startDate;
	private LocalDate endDate;
	private BigDecimal totalDays;
	private String reason;
	private String status;
	private Integer approverId;
	private String approverComment;
	private LocalDateTime reviewedAt;
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;

	public Integer getLeaveRequestId() { return leaveRequestId; }
	public void setLeaveRequestId(Integer leaveRequestId) { this.leaveRequestId = leaveRequestId; }
	public Integer getCompanyId() { return companyId; }
	public void setCompanyId(Integer companyId) { this.companyId = companyId; }
	public Integer getEmployeeId() { return employeeId; }
	public void setEmployeeId(Integer employeeId) { this.employeeId = employeeId; }
	public Integer getLeaveTypeId() { return leaveTypeId; }
	public void setLeaveTypeId(Integer leaveTypeId) { this.leaveTypeId = leaveTypeId; }
	public LocalDate getStartDate() { return startDate; }
	public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
	public LocalDate getEndDate() { return endDate; }
	public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
	public BigDecimal getTotalDays() { return totalDays; }
	public void setTotalDays(BigDecimal totalDays) { this.totalDays = totalDays; }
	public String getReason() { return reason; }
	public void setReason(String reason) { this.reason = reason; }
	public String getStatus() { return status; }
	public void setStatus(String status) { this.status = status; }
	public Integer getApproverId() { return approverId; }
	public void setApproverId(Integer approverId) { this.approverId = approverId; }
	public String getApproverComment() { return approverComment; }
	public void setApproverComment(String approverComment) { this.approverComment = approverComment; }
	public LocalDateTime getReviewedAt() { return reviewedAt; }
	public void setReviewedAt(LocalDateTime reviewedAt) { this.reviewedAt = reviewedAt; }
	public LocalDateTime getCreatedAt() { return createdAt; }
	public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
	public LocalDateTime getUpdatedAt() { return updatedAt; }
	public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}

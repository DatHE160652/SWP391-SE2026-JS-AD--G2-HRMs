package com.hrms.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Thực thể đại diện cho bảng LeaveRequests trong cơ sở dữ liệu:
 * Lưu trữ thông tin đơn xin nghỉ phép của nhân viên, số ngày nghỉ, trạng thái duyệt
 * và các trường dữ liệu bổ trợ hiển thị từ các bảng liên kết (Employees, Departments, LeaveTypes, Users).
 */
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

	// Display helper fields populated from joined queries
	private String employeeCode;
	private String employeeFullName;
	private String departmentName;
	private String leaveTypeCode;
	private String leaveTypeName;
	private Boolean isPaid;
	private String approverDisplayName;
	private String managerName;

	public String getEmployeeCode() { return employeeCode; }
	public void setEmployeeCode(String employeeCode) { this.employeeCode = employeeCode; }

	public String getEmployeeFullName() { return employeeFullName; }
	public void setEmployeeFullName(String employeeFullName) { this.employeeFullName = employeeFullName; }

	public String getDepartmentName() { return departmentName; }
	public void setDepartmentName(String departmentName) { this.departmentName = departmentName; }

	public String getLeaveTypeCode() { return leaveTypeCode; }
	public void setLeaveTypeCode(String leaveTypeCode) { this.leaveTypeCode = leaveTypeCode; }

	public String getLeaveTypeName() { return leaveTypeName; }
	public void setLeaveTypeName(String leaveTypeName) { this.leaveTypeName = leaveTypeName; }

	public Boolean getIsPaid() { return isPaid; }
	public void setIsPaid(Boolean isPaid) { this.isPaid = isPaid; }

	public String getApproverDisplayName() { return approverDisplayName; }
	public void setApproverDisplayName(String approverDisplayName) { this.approverDisplayName = approverDisplayName; }

	public String getManagerName() { return managerName; }
	public void setManagerName(String managerName) { this.managerName = managerName; }
}

package com.hrms.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public class Attendance {
	private Long attendanceId;
	private Integer companyId;
	private Integer employeeId;
	private LocalDate attendanceDate;
	private LocalTime checkInTime;
	private LocalTime checkOutTime;
	private BigDecimal totalHours;
	private String status;
	private String note;
	private Boolean manualAdjusted;
	private Integer adjustedByUserId;
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;

	public Long getAttendanceId() { return attendanceId; }
	public void setAttendanceId(Long attendanceId) { this.attendanceId = attendanceId; }
	public Integer getCompanyId() { return companyId; }
	public void setCompanyId(Integer companyId) { this.companyId = companyId; }
	public Integer getEmployeeId() { return employeeId; }
	public void setEmployeeId(Integer employeeId) { this.employeeId = employeeId; }
	public LocalDate getAttendanceDate() { return attendanceDate; }
	public void setAttendanceDate(LocalDate attendanceDate) { this.attendanceDate = attendanceDate; }
	public LocalTime getCheckInTime() { return checkInTime; }
	public void setCheckInTime(LocalTime checkInTime) { this.checkInTime = checkInTime; }
	public LocalTime getCheckOutTime() { return checkOutTime; }
	public void setCheckOutTime(LocalTime checkOutTime) { this.checkOutTime = checkOutTime; }
	public BigDecimal getTotalHours() { return totalHours; }
	public void setTotalHours(BigDecimal totalHours) { this.totalHours = totalHours; }
	public String getStatus() { return status; }
	public void setStatus(String status) { this.status = status; }
	public String getNote() { return note; }
	public void setNote(String note) { this.note = note; }
	public Boolean getManualAdjusted() { return manualAdjusted; }
	public void setManualAdjusted(Boolean manualAdjusted) { this.manualAdjusted = manualAdjusted; }
	public Boolean getIsManualAdjusted() { return manualAdjusted; }
	public void setIsManualAdjusted(Boolean manualAdjusted) { this.manualAdjusted = manualAdjusted; }
	public Integer getAdjustedByUserId() { return adjustedByUserId; }
	public void setAdjustedByUserId(Integer adjustedByUserId) { this.adjustedByUserId = adjustedByUserId; }
	public LocalDateTime getCreatedAt() { return createdAt; }
	public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
	public LocalDateTime getUpdatedAt() { return updatedAt; }
	public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}

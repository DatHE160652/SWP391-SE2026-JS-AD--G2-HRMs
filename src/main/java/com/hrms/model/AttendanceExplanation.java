package com.hrms.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Model representing an Attendance Explanation / Correction request (UC86, UC87).
 * Submitted by employees when missing punch, late, or absent with justification.
 */
public class AttendanceExplanation {
    private int explanationId;
    private int companyId;
    private int employeeId;
    private LocalDate attendanceDate;
    private LocalTime proposedCheckIn;
    private LocalTime proposedCheckOut;
    private String reasonType;     // FORGOT_PUNCH, BUSINESS_TRIP, TECHNICAL_ISSUE, OTHER
    private String reasonDetail;
    private String status;         // PENDING, APPROVED, REJECTED, CANCELLED
    private Integer reviewerId;
    private String reviewerComment;
    private LocalDateTime reviewedAt;
    private LocalDateTime createdAt;

    // Transient fields for display
    private String employeeCode;
    private String employeeFullName;
    private String departmentName;
    private String reviewerDisplayName;

    public AttendanceExplanation() {
        this.status = "PENDING";
        this.createdAt = LocalDateTime.now();
    }

    public AttendanceExplanation(int companyId, int employeeId, LocalDate attendanceDate,
                                 LocalTime proposedCheckIn, LocalTime proposedCheckOut,
                                 String reasonType, String reasonDetail) {
        this();
        this.companyId = companyId;
        this.employeeId = employeeId;
        this.attendanceDate = attendanceDate;
        this.proposedCheckIn = proposedCheckIn;
        this.proposedCheckOut = proposedCheckOut;
        this.reasonType = reasonType;
        this.reasonDetail = reasonDetail;
    }

    public int getExplanationId() {
        return explanationId;
    }

    public void setExplanationId(int explanationId) {
        this.explanationId = explanationId;
    }

    public int getCompanyId() {
        return companyId;
    }

    public void setCompanyId(int companyId) {
        this.companyId = companyId;
    }

    public int getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(int employeeId) {
        this.employeeId = employeeId;
    }

    public LocalDate getAttendanceDate() {
        return attendanceDate;
    }

    public void setAttendanceDate(LocalDate attendanceDate) {
        this.attendanceDate = attendanceDate;
    }

    public LocalTime getProposedCheckIn() {
        return proposedCheckIn;
    }

    public void setProposedCheckIn(LocalTime proposedCheckIn) {
        this.proposedCheckIn = proposedCheckIn;
    }

    public LocalTime getProposedCheckOut() {
        return proposedCheckOut;
    }

    public void setProposedCheckOut(LocalTime proposedCheckOut) {
        this.proposedCheckOut = proposedCheckOut;
    }

    public String getReasonType() {
        return reasonType;
    }

    public void setReasonType(String reasonType) {
        this.reasonType = reasonType;
    }

    public String getReasonDetail() {
        return reasonDetail;
    }

    public void setReasonDetail(String reasonDetail) {
        this.reasonDetail = reasonDetail;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getReviewerId() {
        return reviewerId;
    }

    public void setReviewerId(Integer reviewerId) {
        this.reviewerId = reviewerId;
    }

    public String getReviewerComment() {
        return reviewerComment;
    }

    public void setReviewerComment(String reviewerComment) {
        this.reviewerComment = reviewerComment;
    }

    public LocalDateTime getReviewedAt() {
        return reviewedAt;
    }

    public void setReviewedAt(LocalDateTime reviewedAt) {
        this.reviewedAt = reviewedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getEmployeeCode() {
        return employeeCode;
    }

    public void setEmployeeCode(String employeeCode) {
        this.employeeCode = employeeCode;
    }

    public String getEmployeeFullName() {
        return employeeFullName;
    }

    public void setEmployeeFullName(String employeeFullName) {
        this.employeeFullName = employeeFullName;
    }

    public String getDepartmentName() {
        return departmentName;
    }

    public void setDepartmentName(String departmentName) {
        this.departmentName = departmentName;
    }

    public String getReviewerDisplayName() {
        return reviewerDisplayName;
    }

    public void setReviewerDisplayName(String reviewerDisplayName) {
        this.reviewerDisplayName = reviewerDisplayName;
    }

    public String getReasonTypeDisplay() {
        if (reasonType == null) return "Khác";
        return switch (reasonType.toUpperCase()) {
            case "FORGOT_PUNCH" -> "Quên chấm công";
            case "BUSINESS_TRIP" -> "Đi công tác / gặp đối tác";
            case "TECHNICAL_ISSUE" -> "Lỗi thiết bị / kết nối";
            default -> "Lý do khác";
        };
    }
}


package com.hrms.model;

import java.time.LocalDateTime;

/**
 * Thực thể đại diện cho bảng LeaveTypes trong cơ sở dữ liệu:
 * Cấu hình danh mục loại nghỉ phép, hạn mức ngày mặc định trong năm,
 * chế độ hưởng lương (IsPaid) và trạng thái hoạt động (IsActive).
 */
public class LeaveType {
    private boolean active = true;

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    private Integer leaveTypeId;
    private Integer companyId;
    private String typeCode;
    private String typeName;
    private Integer defaultDaysPerYear;
    private Boolean paid;
    private LocalDateTime createdAt;

    public Integer getLeaveTypeId() { return leaveTypeId; }
    public void setLeaveTypeId(Integer leaveTypeId) { this.leaveTypeId = leaveTypeId; }
    public Integer getCompanyId() { return companyId; }
    public void setCompanyId(Integer companyId) { this.companyId = companyId; }
    public String getTypeCode() { return typeCode; }
    public void setTypeCode(String typeCode) { this.typeCode = typeCode; }
    public String getTypeName() { return typeName; }
    public void setTypeName(String typeName) { this.typeName = typeName; }
    public Integer getDefaultDaysPerYear() { return defaultDaysPerYear; }
    public void setDefaultDaysPerYear(Integer defaultDaysPerYear) { this.defaultDaysPerYear = defaultDaysPerYear; }
    public Boolean getPaid() { return paid; }
    public void setPaid(Boolean paid) { this.paid = paid; }
    public Boolean getIsPaid() { return paid; }
    public void setIsPaid(Boolean paid) { this.paid = paid; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}

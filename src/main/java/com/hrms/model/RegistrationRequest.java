package com.hrms.model;

import java.time.LocalDateTime;

public class RegistrationRequest {
    private Integer registrationRequestId;
    private String fullName;
    private String email;
    private String phone;
    private String companyName;
    private String region;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Conversion details (populated only when status = CONVERTED)
    private String workspaceCode;
    private LocalDateTime convertedAt;
    private String convertedByName;
    private String companyAdminEmail;
    private LocalDateTime companyAdminActivatedAt;
    private String activationEmailStatus;
    private LocalDateTime activationEmailCompletedAt;

    public Integer getRegistrationRequestId() { return registrationRequestId; }
    public void setRegistrationRequestId(Integer value) { registrationRequestId = value; }
    public String getFullName() { return fullName; }
    public void setFullName(String value) { fullName = value; }
    public String getEmail() { return email; }
    public void setEmail(String value) { email = value; }
    public String getPhone() { return phone; }
    public void setPhone(String value) { phone = value; }
    public String getCompanyName() { return companyName; }
    public void setCompanyName(String value) { companyName = value; }
    public String getRegion() { return region; }
    public void setRegion(String value) { region = value; }
    public String getStatus() { return status; }
    public void setStatus(String value) { status = value; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime value) { createdAt = value; }
    public String getStatusLabel() {
        if (status == null) return "—";
        return switch (status) {
            case "NEW" -> "Mới tiếp nhận";
            case "UNDER_REVIEW" -> "Đang xem xét";
            case "CONVERTING" -> "Đang khởi tạo";
            case "CONVERTED" -> "Đã chuyển đổi";
            default -> status;
        };
    }

    public String getRegionLabel() {
        if (region == null) return "—";
        return switch (region) {
            case "NORTH" -> "Miền Bắc";
            case "CENTRAL" -> "Miền Trung";
            case "SOUTH" -> "Miền Nam";
            default -> region;
        };
    }

    public String getCreatedAtDisplay() {
        return com.hrms.util.TimeSupport.formatUtc(createdAt);
    }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime value) { updatedAt = value; }

    public String getWorkspaceCode() { return workspaceCode; }
    public void setWorkspaceCode(String value) { workspaceCode = value; }
    public LocalDateTime getConvertedAt() { return convertedAt; }
    public void setConvertedAt(LocalDateTime value) { convertedAt = value; }
    public String getConvertedByName() { return convertedByName; }
    public void setConvertedByName(String value) { convertedByName = value; }
    public String getCompanyAdminEmail() { return companyAdminEmail; }
    public void setCompanyAdminEmail(String value) { companyAdminEmail = value; }
    public LocalDateTime getCompanyAdminActivatedAt() { return companyAdminActivatedAt; }
    public void setCompanyAdminActivatedAt(LocalDateTime value) { companyAdminActivatedAt = value; }
    public String getActivationEmailStatus() { return activationEmailStatus; }
    public void setActivationEmailStatus(String value) { activationEmailStatus = value; }

    public String getConvertedAtDisplay() {
        return com.hrms.util.TimeSupport.formatUtc(convertedAt);
    }

    public String getActivationEmailStatusLabel() {
        if (activationEmailStatus == null) return "Chưa có lần gửi được ghi nhận";
        return switch (activationEmailStatus) {
            case "ACCEPTED" -> "Nhà cung cấp đã tiếp nhận";
            case "FAILED" -> "Gửi thất bại";
            case "UNKNOWN" -> "Không xác định";
            case "PENDING" -> "Đang xử lý";
            default -> activationEmailStatus;
        };
    }

    public LocalDateTime getActivationEmailCompletedAt() { return activationEmailCompletedAt; }
    public void setActivationEmailCompletedAt(LocalDateTime value) { activationEmailCompletedAt = value; }

    public String getActivationEmailCompletedAtDisplay() {
        return com.hrms.util.TimeSupport.formatUtc(activationEmailCompletedAt);
    }

    public String getCompanyAdminActivatedAtDisplay() {
        return com.hrms.util.TimeSupport.formatUtc(companyAdminActivatedAt);
    }

    public boolean isCompanyAdminActivated() {
        return companyAdminActivatedAt != null;
    }
}

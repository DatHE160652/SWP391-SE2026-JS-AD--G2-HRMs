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
        return createdAt == null ? "—" : createdAt.format(
                java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
    }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime value) { updatedAt = value; }
}

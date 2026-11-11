package com.hrms.model;

/** One company policy, applied independently to every employee. */
public record LeavePolicy(int companyId, String quotaMode, int quotaDays, int maxDaysPerRequest) {
    public LeavePolicy {
        if (!"MONTHLY".equals(quotaMode) && !"ANNUAL".equals(quotaMode))
            throw new IllegalArgumentException("Chọn cấu hình theo tháng hoặc theo năm.");
        if (quotaDays < 0 || quotaDays > 366 || maxDaysPerRequest < 1 || maxDaysPerRequest > 366)
            throw new IllegalArgumentException("Quota phải từ 0–366 và giới hạn mỗi đơn từ 1–366 ngày.");
    }

    public int daysForMonth(int month) {
        if (month < 1 || month > 12) throw new IllegalArgumentException("Tháng không hợp lệ.");
        return "MONTHLY".equals(quotaMode) ? quotaDays : quotaDays / 12 + (month == 1 ? quotaDays % 12 : 0);
    }
}

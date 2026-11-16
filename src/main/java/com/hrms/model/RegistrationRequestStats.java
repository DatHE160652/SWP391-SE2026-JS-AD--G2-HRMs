package com.hrms.model;

public final class RegistrationRequestStats {
    private final long newCount;
    private final long reviewCount;
    private final long convertingCount;
    private final long convertedCount;
    private final long companyCount;
    private final long tenantUserCount;
    private final long activeSubscriptionCount;
    private final long planCount;

    public RegistrationRequestStats(long newCount, long reviewCount, long convertingCount,
            long convertedCount, long companyCount, long tenantUserCount) {
        this(newCount, reviewCount, convertingCount, convertedCount, companyCount, tenantUserCount, 0L, 0L);
    }

    public RegistrationRequestStats(long newCount, long reviewCount, long convertingCount,
            long convertedCount, long companyCount, long tenantUserCount,
            long activeSubscriptionCount) {
        this(newCount, reviewCount, convertingCount, convertedCount, companyCount, tenantUserCount,
                activeSubscriptionCount, 0L);
    }

    public RegistrationRequestStats(long newCount, long reviewCount, long convertingCount,
            long convertedCount, long companyCount, long tenantUserCount,
            long activeSubscriptionCount, long planCount) {
        this.newCount = newCount;
        this.reviewCount = reviewCount;
        this.convertingCount = convertingCount;
        this.convertedCount = convertedCount;
        this.companyCount = companyCount;
        this.tenantUserCount = tenantUserCount;
        this.activeSubscriptionCount = activeSubscriptionCount;
        this.planCount = planCount;
    }

    public long getNewCount() {
        return newCount;
    }

    public long getReviewCount() {
        return reviewCount;
    }

    public long getConvertingCount() {
        return convertingCount;
    }

    public long getConvertedCount() {
        return convertedCount;
    }

    public long getPendingCount() {
        return newCount + reviewCount;
    }

    public long getCompanyCount() {
        return companyCount;
    }

    public long getTenantUserCount() {
        return tenantUserCount;
    }

    public long getActiveSubscriptionCount() {
        return activeSubscriptionCount;
    }

    public long getPlanCount() {
        return planCount;
    }
}

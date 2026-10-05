package com.hrms.model;

public final class RegistrationRequestStats {
    private final long newCount;
    private final long reviewCount;
    private final long convertingCount;
    private final long convertedCount;
    private final long companyCount;
    private final long tenantUserCount;

    public RegistrationRequestStats(long newCount, long reviewCount, long convertingCount,
                                    long convertedCount, long companyCount, long tenantUserCount) {
        this.newCount = newCount;
        this.reviewCount = reviewCount;
        this.convertingCount = convertingCount;
        this.convertedCount = convertedCount;
        this.companyCount = companyCount;
        this.tenantUserCount = tenantUserCount;
    }

    public long getNewCount() { return newCount; }
    public long getReviewCount() { return reviewCount; }
    public long getConvertingCount() { return convertingCount; }
    public long getConvertedCount() { return convertedCount; }
    public long getPendingCount() { return newCount + reviewCount; }
    public long getCompanyCount() { return companyCount; }
    public long getTenantUserCount() { return tenantUserCount; }
}

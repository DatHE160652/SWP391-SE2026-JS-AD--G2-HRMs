package com.hrms.model;

import java.util.Locale;
import java.util.Set;

/** Validated System Admin Company list filters and trusted sort selection. */
public final class CompanyAdministrationFilter {
    public static final String ACTIVE = "ACTIVE";
    public static final String SUSPENDED = "SUSPENDED";
    public static final String NEWEST = "NEWEST";
    public static final String ACCOUNT_DESC = "ACCOUNT_DESC";
    public static final String NAME_ASC = "NAME_ASC";

    private final String q;
    private final String status;
    private final String region;
    private final String sort;

    public CompanyAdministrationFilter(String status, String region, String sort) {
        this("", status, region, sort);
    }

    public CompanyAdministrationFilter(String q, String status, String region, String sort) {
        String trimmedQ = q == null ? "" : q.trim();
        this.q = trimmedQ.length() > 150 ? trimmedQ.substring(0, 150) : trimmedQ;
        this.status = clean(status);
        this.region = clean(region);
        this.sort = clean(sort).isEmpty() ? NEWEST : clean(sort);
        if (!Set.of("", ACTIVE, SUSPENDED).contains(this.status)) {
            throw new IllegalArgumentException("Trạng thái Subscription không hợp lệ.");
        }
        if (!Set.of("", "NORTH", "CENTRAL", "SOUTH").contains(this.region)) {
            throw new IllegalArgumentException("Khu vực lọc không hợp lệ.");
        }
        if (!Set.of(NEWEST, ACCOUNT_DESC, NAME_ASC).contains(this.sort)) {
            throw new IllegalArgumentException("Cách sắp xếp không hợp lệ.");
        }
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    /** The shared System Admin shell reads this property for its search box. */
    public String getQ() { return q; }
    public String getStatus() { return status; }
    public String getRegion() { return region; }
    public String getSort() { return sort; }
    public boolean isAllStatuses() { return status.isEmpty(); }
    public boolean isActive() { return ACTIVE.equals(status); }
    public boolean isSuspended() { return SUSPENDED.equals(status); }
}

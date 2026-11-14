package com.hrms.model;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Set;

/** Validated, read-only list filters. Dates are Vietnam calendar dates. */
public final class RegistrationRequestFilter {
    private final String q;
    private final String status;
    private final String region;
    private final String date;
    private final int page;
    private final int size;

    public RegistrationRequestFilter(String q, String status, String region, String date,
                                     String page, String size) {
        this.q = clean(q);
        this.status = clean(status);
        this.region = clean(region);
        this.date = clean(date);
        if (this.q.length() > 150) throw new IllegalArgumentException("Từ khóa tối đa 150 ký tự.");
        if (!Set.of("", "PENDING", "NEW", "UNDER_REVIEW", "CONVERTING", "CONVERTED").contains(this.status)) {
            throw new IllegalArgumentException("Trạng thái lọc không hợp lệ.");
        }
        if (!Set.of("", "NORTH", "CENTRAL", "SOUTH").contains(this.region)) {
            throw new IllegalArgumentException("Khu vực lọc không hợp lệ.");
        }
        if (!this.date.isEmpty()) {
            try {
                LocalDate parsed = LocalDate.parse(this.date);
                if (parsed.isBefore(LocalDate.of(1, 1, 2)) || parsed.getYear() >= 9999) throw new IllegalArgumentException();
            } catch (DateTimeParseException | IllegalArgumentException exception) {
                throw new IllegalArgumentException("Ngày lọc không hợp lệ.");
            }
        }
        this.page = positiveOrDefault(page, 1);
        int requestedSize = positiveOrDefault(size, 10);
        this.size = Set.of(10, 20, 50).contains(requestedSize) ? requestedSize : 10;
    }

    private static String clean(String value) { return value == null ? "" : value.trim(); }
    private static int positiveOrDefault(String value, int fallback) {
        try { return Math.max(1, Integer.parseInt(clean(value))); }
        catch (NumberFormatException exception) { return fallback; }
    }

    public String getQ() { return q; }
    public String getStatus() { return status; }
    public String getRegion() { return region; }
    public String getDate() { return date; }
    public int getPage() { return page; }
    public int getSize() { return size; }
}

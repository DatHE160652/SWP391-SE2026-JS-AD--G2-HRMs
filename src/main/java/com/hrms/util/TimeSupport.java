package com.hrms.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/** UTC event display and Vietnam day filters for the public portal and System Admin. */
public final class TimeSupport {
    public static final ZoneId VIETNAM = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter DISPLAY = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private TimeSupport() { }

    public static String formatUtc(LocalDateTime utc) {
        return utc == null ? "—" : utc.atOffset(ZoneOffset.UTC).atZoneSameInstant(VIETNAM).format(DISPLAY);
    }

    public static LocalDateTime startOfDayUtc(LocalDate date) {
        return date.atStartOfDay(VIETNAM).withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime();
    }
}

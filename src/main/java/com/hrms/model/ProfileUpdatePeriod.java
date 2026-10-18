package com.hrms.model;

import java.time.LocalDateTime;

public record ProfileUpdatePeriod(long periodId, int companyId, String title, LocalDateTime startsAt,
                                  LocalDateTime endsAt, LocalDateTime closedAt) {
    public boolean isOpen(LocalDateTime now) {
        return closedAt == null && !now.isBefore(startsAt) && now.isBefore(endsAt);
    }
}

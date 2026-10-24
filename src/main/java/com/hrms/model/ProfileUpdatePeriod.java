package com.hrms.model;

import java.time.LocalDateTime;

public record ProfileUpdatePeriod(long periodId, int companyId, String title, LocalDateTime startsAt,
                                  LocalDateTime endsAt, LocalDateTime closedAt, String periodType) {
    public ProfileUpdatePeriod(long periodId,int companyId,String title,LocalDateTime startsAt,LocalDateTime endsAt,LocalDateTime closedAt) {
        this(periodId,companyId,title,startsAt,endsAt,closedAt,"PROFILE");
    }
    public boolean isOpen(LocalDateTime now) {
        return closedAt == null && !now.isBefore(startsAt) && now.isBefore(endsAt);
    }
}

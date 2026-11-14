package com.hrms.model;

import java.time.LocalDateTime;

public record PayrollNotification(long notificationId, long payslipId, String periodName,
                                  LocalDateTime createdAt, LocalDateTime readAt) { }

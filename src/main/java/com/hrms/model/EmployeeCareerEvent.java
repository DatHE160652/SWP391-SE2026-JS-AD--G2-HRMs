package com.hrms.model;

public record EmployeeCareerEvent(String eventId, String eventType, String recordedAt,
                                  String effectiveDate, String status, String oldValue,
                                  String newValue, String reason, String changedBy) {}

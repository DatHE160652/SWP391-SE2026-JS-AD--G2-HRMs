package com.hrms.model.publicportal;

import com.hrms.model.RegistrationRequest;

public final class ConsultationSubmission {
    private final RegistrationRequest request;
    private final long emailAttemptId;
    private final boolean emailSent;

    public ConsultationSubmission(RegistrationRequest request, long emailAttemptId, boolean emailSent) {
        this.request = request;
        this.emailAttemptId = emailAttemptId;
        this.emailSent = emailSent;
    }

    public RegistrationRequest getRequest() { return request; }
    public long getEmailAttemptId() { return emailAttemptId; }
    public boolean isEmailSent() { return emailSent; }
}

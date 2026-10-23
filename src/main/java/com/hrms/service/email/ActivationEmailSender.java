package com.hrms.service.email;

import com.hrms.model.RegistrationRequest;

/** Sends the one-time Company Admin credential after tenant provisioning commits. */
public interface ActivationEmailSender {
    void send(RegistrationRequest request, String workspaceCode, String loginUrl, String temporaryPassword)
            throws EmailDeliveryException;
}

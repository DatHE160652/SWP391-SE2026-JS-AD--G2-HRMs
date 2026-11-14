package com.hrms.service.email;

import com.hrms.model.RegistrationRequest;

public interface ReceiptEmailSender {
    void send(RegistrationRequest request) throws EmailDeliveryException;
}

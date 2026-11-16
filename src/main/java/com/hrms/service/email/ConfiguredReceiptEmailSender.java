package com.hrms.service.email;

import com.hrms.model.RegistrationRequest;

/** SMTP-configured, credential-free consultation receipt sender. */
public final class ConfiguredReceiptEmailSender implements ReceiptEmailSender {
    private final SmtpEmailTransport transport = new SmtpEmailTransport();
    private final EmailRecipientVerifier verifier = new EmailRecipientVerifier();

    @Override
    public void send(RegistrationRequest request) throws EmailDeliveryException {
        EmailRecipientVerifier.VerifyResult verification = verifier.verify(request.getEmail(), false);
        if (!verification.deliverable()) {
            throw new EmailDeliveryException(verification.errorCode(), verification.message());
        }
        transport.send(request.getEmail(), "HRMs Cloud đã tiếp nhận yêu cầu tư vấn", body(request));
    }

    static String body(RegistrationRequest request) {
        return "Xin chào " + request.getFullName() + ",\n\n"
                + "HRMs Cloud đã tiếp nhận yêu cầu tư vấn của bạn.\n"
                + "Đội ngũ HRMs Cloud sẽ liên hệ để tư vấn. Việc gửi yêu cầu chưa tạo tài khoản hoặc workspace.\n"
                + "Cảm ơn bạn đã quan tâm đến HRMs Cloud.";
    }
}

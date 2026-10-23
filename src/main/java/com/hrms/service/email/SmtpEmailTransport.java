package com.hrms.service.email;

import jakarta.mail.AuthenticationFailedException;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

/** Shared SMTP transport. Never includes provider payloads or message bodies in errors. */
final class SmtpEmailTransport {
    void send(String recipient, String subject, String body) throws EmailDeliveryException {
        SmtpConfiguration configuration = SmtpConfiguration.load();
        boolean sending = false;
        try {
            MimeMessage message = new MimeMessage(Session.getInstance(configuration.properties));
            message.setFrom(new InternetAddress(configuration.from, true));
            message.setRecipient(Message.RecipientType.TO, new InternetAddress(recipient, true));
            message.setSubject(subject, "UTF-8");
            message.setText(body, "UTF-8");
            Transport transport = message.getSession().getTransport(configuration.protocol);
            try {
                transport.connect(configuration.host,
                        configuration.username.isBlank() ? null : configuration.username,
                        configuration.username.isBlank() ? null : configuration.password);
                sending = true;
                transport.sendMessage(message, message.getAllRecipients());
            } finally {
                try { transport.close(); } catch (MessagingException ignored) { }
            }
        } catch (AuthenticationFailedException exception) {
            throw new EmailDeliveryException("MAIL_AUTH_FAILED", "SMTP authentication failed.");
        } catch (MessagingException exception) {
            throw new EmailDeliveryException(sending ? "MAIL_PROVIDER_UNAVAILABLE" : "MAIL_SEND_FAILED",
                    "SMTP could not send the message.");
        } catch (RuntimeException exception) {
            throw new EmailDeliveryException("MAIL_CONFIGURATION_INVALID", "SMTP configuration is invalid.");
        }
    }
}

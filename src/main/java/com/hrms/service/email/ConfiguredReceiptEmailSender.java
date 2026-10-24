package com.hrms.service.email;

import com.hrms.model.RegistrationRequest;
import jakarta.mail.AuthenticationFailedException;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import java.util.Properties;

/** SMTP configuration is supplied by the Tomcat process; no credentials are stored here. */
public final class ConfiguredReceiptEmailSender implements ReceiptEmailSender {
    @Override
    public void send(RegistrationRequest request) throws EmailDeliveryException {
        String host = setting("HRM_SMTP_HOST", "hrm.smtp.host", "");
        String from = setting("HRM_SMTP_FROM", "hrm.smtp.from", "");
        if (host.isBlank() || from.isBlank()) {
            throw new EmailDeliveryException("MAIL_NOT_CONFIGURED", "SMTP is not configured.");
        }
        String username = setting("HRM_SMTP_USER", "hrm.smtp.user", "");
        String password = setting("HRM_SMTP_PASSWORD", "hrm.smtp.password", "");
        boolean ssl = Boolean.parseBoolean(setting("HRM_SMTP_SSL", "hrm.smtp.ssl", "false"));
        boolean tls = !ssl && Boolean.parseBoolean(setting("HRM_SMTP_STARTTLS", "hrm.smtp.starttls", "true"));
        String protocol = ssl ? "smtps" : "smtp";
        String prefix = "mail." + protocol + ".";
        Properties properties = new Properties();
        properties.setProperty(prefix + "host", host);
        properties.setProperty(prefix + "port", setting("HRM_SMTP_PORT", "hrm.smtp.port", ssl ? "465" : "587"));
        properties.setProperty(prefix + "auth", Boolean.toString(!username.isBlank()));
        properties.setProperty(prefix + "starttls.enable", Boolean.toString(tls));
        properties.setProperty(prefix + "starttls.required", Boolean.toString(tls));
        properties.setProperty(prefix + "ssl.checkserveridentity", "true");
        properties.setProperty(prefix + "connectiontimeout", "5000");
        properties.setProperty(prefix + "timeout", "10000");
        properties.setProperty(prefix + "writetimeout", "10000");
        boolean sending = false;
        try {
            MimeMessage message = new MimeMessage(Session.getInstance(properties));
            message.setFrom(new InternetAddress(from, true));
            message.setRecipient(Message.RecipientType.TO, new InternetAddress(request.getEmail(), true));
            message.setSubject("HRMs Cloud đã tiếp nhận yêu cầu tư vấn", "UTF-8");
            message.setText(body(request), "UTF-8");
            Transport transport = message.getSession().getTransport(protocol);
            try {
                transport.connect(host, username.isBlank() ? null : username, username.isBlank() ? null : password);
                sending = true;
                transport.sendMessage(message, message.getAllRecipients());
            } finally {
                try { transport.close(); } catch (MessagingException ignored) { }
            }
        } catch (AuthenticationFailedException exception) {
            throw new EmailDeliveryException("MAIL_AUTH_FAILED", "SMTP authentication failed.");
        } catch (MessagingException exception) {
            throw new EmailDeliveryException(sending ? "MAIL_PROVIDER_UNAVAILABLE" : "MAIL_SEND_FAILED",
                    "SMTP could not send the receipt.");
        } catch (RuntimeException exception) {
            throw new EmailDeliveryException("MAIL_CONFIGURATION_INVALID", "SMTP configuration is invalid.");
        }
    }

    static String body(RegistrationRequest request) {
        return "Xin chào " + request.getFullName() + ",\n\n"
                + "HRMs Cloud đã tiếp nhận yêu cầu tư vấn của bạn.\n"
                + "Mã yêu cầu: REQ-" + request.getRegistrationRequestId() + "\n"
                + "Trạng thái: NEW — Đã tiếp nhận\n\n"
                + "Đội ngũ HRMs Cloud sẽ liên hệ để tư vấn. Việc gửi yêu cầu chưa tạo tài khoản hoặc workspace.\n"
                + "Cảm ơn bạn đã quan tâm đến HRMs Cloud.";
    }

    private static String setting(String environment, String property, String fallback) {
        String value = System.getenv(environment);
        if (value == null || value.isBlank()) value = System.getProperty(property);
        return value == null || value.isBlank() ? fallback : value;
    }
}

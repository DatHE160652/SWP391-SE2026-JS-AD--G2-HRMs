package com.hrms.service.email;

import jakarta.mail.internet.InternetAddress;

public final class ConfiguredPasswordResetEmailSender implements PasswordResetEmailSender {
    @Override
    public Prepared prepare(String recipient, String workspace, String loginUrl) throws EmailDeliveryException {
        SmtpConfiguration config = SmtpConfiguration.load();
        try {
            new InternetAddress(config.from, true).validate();
            new InternetAddress(recipient, true).validate();
            int port = Integer.parseInt(config.properties.getProperty("mail." + config.protocol + ".port"));
            if (port < 1 || port > 65535 || config.host.contains("\r") || config.host.contains("\n")) throw new IllegalArgumentException();
            if (recipient.contains("\r") || recipient.contains("\n") || workspace == null || loginUrl == null) throw new IllegalArgumentException();
        } catch (Exception exception) {
            throw new EmailDeliveryException("MAIL_CONFIGURATION_INVALID", "Invalid reset email configuration.");
        }
        return password -> new SmtpEmailTransport().send(config, recipient,
                "HRMs - Đặt lại mật khẩu", body(recipient, workspace, loginUrl, password));
    }
    static String body(String email, String workspace, String loginUrl, String password) {
        return "Mật khẩu của bạn đã được đặt lại.\n"
                + "Workspace: " + workspace + "\nĐăng nhập: " + loginUrl + "\nEmail: " + email
                + "\nMật khẩu tạm: " + password
                + "\n\nVui lòng đổi mật khẩu ngay sau khi đăng nhập.\n"
                + "Nếu tài khoản đang bị khóa, vui lòng liên hệ quản trị viên để mở khóa riêng.\n";
    }
}

package com.hrms.service.email;

import com.hrms.model.RegistrationRequest;

/** Sends Company Admin activation credentials using the configured SMTP transport. */
public final class ConfiguredActivationEmailSender implements ActivationEmailSender {
    private final SmtpEmailTransport transport = new SmtpEmailTransport();

    @Override
    public void send(RegistrationRequest request, String workspaceCode, String loginUrl, String temporaryPassword)
            throws EmailDeliveryException {
        String subject = "HRMs Cloud — kích hoạt tài khoản quản trị " + request.getCompanyName();
        transport.send(request.getEmail(), subject, body(request, workspaceCode, loginUrl, temporaryPassword));
    }

    static String body(RegistrationRequest request, String workspaceCode, String loginUrl, String temporaryPassword) {
        return "Xin chào " + request.getFullName() + ",\n\n"
                + "Yêu cầu tư vấn của bạn đã được phê duyệt.\n"
                + "Tenant " + request.getCompanyName() + " đã được khởi tạo.\n"
                + "Địa chỉ email bạn đã đăng ký tư vấn được dùng làm email đăng nhập Company Admin.\n"
                + "Workspace: " + workspaceCode + "\n"
                + "Đăng nhập: " + loginUrl + "\n"
                + "Email: " + request.getEmail() + "\n"
                + "Mật khẩu tạm: " + temporaryPassword + "\n\n"
                + "Hãy nhập workspace và email trên vào form đăng nhập, dùng mật khẩu tạm và đổi mật khẩu trước khi sử dụng workspace.\n"
                + "Nếu không thực hiện yêu cầu này, vui lòng liên hệ HRMs Cloud.";
    }
}

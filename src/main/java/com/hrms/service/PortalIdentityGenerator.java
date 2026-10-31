package com.hrms.service;

import com.hrms.dao.UserDAO;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Locale;

public final class PortalIdentityGenerator {
    public Identity fromContactEmail(Connection connection, String contactEmail) throws SQLException {
        String email = contactEmail == null ? "" : contactEmail.strip().toLowerCase(Locale.ROOT);
        if (email.isBlank())
            throw new FieldValidationException("email", "Vui lòng bổ sung email liên hệ trong hồ sơ trước khi cấp tài khoản.");
        if (email.length() > 150 || !email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$"))
            throw new FieldValidationException("email", "Email liên hệ không hợp lệ.");
        UserDAO users = new UserDAO();
        if (users.usernameExists(connection, email) || users.emailExists(connection, email))
            throw new FieldValidationException("email", "Email liên hệ đã được sử dụng bởi một tài khoản khác.");
        return new Identity(email, email);
    }

    public record Identity(String username, String email) {}
}

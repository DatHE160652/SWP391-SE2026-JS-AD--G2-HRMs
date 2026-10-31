package com.hrms.util;

import com.hrms.service.FieldValidationException;

public final class ValidationSupport {
    private ValidationSupport() {
    }

    public static String validatePhone(String phone) {
        if (phone == null || phone.isBlank()) {
            throw new FieldValidationException("phone", "Vui lòng nhập số điện thoại.");
        }
        String normalized = phone.trim();
        if (!normalized.matches("0[0-9]{9}")) {
            throw new FieldValidationException("phone", "Số điện thoại phải gồm 10 chữ số và bắt đầu bằng 0.");
        }
        return normalized;
    }

    public static String errorJson(Exception exception) {
        String fields = exception instanceof FieldValidationException validation
                ? ",\"fieldErrors\":{" + quote(validation.getField()) + ":" + quote(validation.getMessage()) + "}"
                : "";
        return "{\"success\":false,\"message\":" + quote(exception.getMessage()) + fields + "}";
    }

    public static String quote(String value) {
        if (value == null) {
            return "null";
        }
        StringBuilder result = new StringBuilder("\"");
        for (char character : value.toCharArray()) {
            if (character == '"' || character == '\\') {
                result.append('\\').append(character);
            } else if (character < 32) {
                result.append(String.format("\\u%04x", (int) character));
            } else {
                result.append(character);
            }
        }
        return result.append('"').toString();
    }
}

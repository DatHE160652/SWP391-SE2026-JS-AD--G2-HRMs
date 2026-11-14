package com.hrms.service.email;

public class EmailDeliveryException extends Exception {
    private final String code;

    public EmailDeliveryException(String code, String message) {
        super(message);
        this.code = code;
    }

    public EmailDeliveryException(String code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public String getCode() { return code; }
}

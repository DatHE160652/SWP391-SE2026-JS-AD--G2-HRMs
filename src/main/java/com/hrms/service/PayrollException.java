package com.hrms.service;

import java.util.List;

public class PayrollException extends RuntimeException {
    private final int status;
    private final List<String> errors;

    public PayrollException(int status, String message) { this(status, message, List.of()); }
    public PayrollException(int status, String message, List<String> errors) {
        super(message);
        this.status = status;
        this.errors = List.copyOf(errors);
    }
    public int getStatus() { return status; }
    public List<String> getErrors() { return errors; }
}

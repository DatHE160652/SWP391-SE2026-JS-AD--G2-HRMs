package com.hrms.service.email;

/** Preparation validates configuration without network I/O; send occurs after commit. */
public interface PasswordResetEmailSender {
    Prepared prepare(String recipient, String workspace, String loginUrl) throws EmailDeliveryException;
    @FunctionalInterface
    interface Prepared { void send(String temporaryPassword) throws EmailDeliveryException; }

}

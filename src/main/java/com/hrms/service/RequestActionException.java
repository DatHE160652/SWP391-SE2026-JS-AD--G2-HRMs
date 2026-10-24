package com.hrms.service;

/** A rejected request mutation; messages are safe to display to the administrator. */
public final class RequestActionException extends Exception {
    public enum Reason { FORBIDDEN, NOT_FOUND, CONFLICT }

    private final Reason reason;

    public RequestActionException(Reason reason, String message) {
        super(message);
        this.reason = reason;
    }

    public Reason getReason() { return reason; }
}

package com.tzuchi.his.quality.inspection.application.common;

/**
 * Thrown when the actor does not have access to a resource (hospital scope mismatch, etc.).
 * Maps to HTTP 403 Forbidden with error code "ACCESS_DENIED".
 */
public class AccessDeniedException extends RuntimeException {

    private final String errorCode;

    public AccessDeniedException(String message) {
        super(message);
        this.errorCode = "ACCESS_DENIED";
    }

    public AccessDeniedException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() { return errorCode; }
}

package com.tzuchi.his.quality.inspection.application.common;

/**
 * Thrown when the actor lacks the required role(s) for an operation.
 * Maps to HTTP 403 with error code ROLE_DENIED.
 */
public class RoleDeniedException extends RuntimeException {

    private final String errorCode;

    public RoleDeniedException(String message) {
        super(message);
        this.errorCode = "ROLE_DENIED";
    }

    public String getErrorCode() {
        return errorCode;
    }
}

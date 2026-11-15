package com.tzuchi.his.quality.inspection.domain.common;

/**
 * Thrown when an input value is malformed or out of range (error category invalid_input).
 * Maps to HTTP 400 Bad Request.
 */
public class DomainValidationException extends RuntimeException {

    private final String errorCode;

    public DomainValidationException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() { return errorCode; }
}

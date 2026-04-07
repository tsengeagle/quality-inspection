package com.tzuchi.his.quality.inspection.domain.common;

/**
 * Thrown when a state machine conflict occurs (e.g., duplicate, already published).
 * Maps to HTTP 409 Conflict.
 */
public class DomainStateConflictException extends RuntimeException {

    private final String errorCode;

    public DomainStateConflictException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() { return errorCode; }
}

package com.tzuchi.his.quality.inspection.domain.common;

/**
 * Thrown when a domain rule (invariant) is violated.
 * Maps to HTTP 422 Unprocessable Entity.
 */
public class DomainRuleViolationException extends RuntimeException {

    private final String errorCode;

    public DomainRuleViolationException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() { return errorCode; }
}

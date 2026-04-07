package com.tzuchi.his.quality.inspection.application.common;

/**
 * Thrown when the actor does not have the required role.
 * Maps to HTTP 403 Forbidden.
 */
public class UnauthorizedException extends RuntimeException {

    public UnauthorizedException(String message) {
        super(message);
    }
}

package com.tzuchi.his.quality.inspection.infrastructure.security;

/**
 * Thrown when the api-key JWT header cannot be parsed or is structurally invalid.
 */
public class JwtParseException extends RuntimeException {

    public JwtParseException(String message) {
        super(message);
    }

    public JwtParseException(String message, Throwable cause) {
        super(message, cause);
    }
}

package com.tzuchi.his.quality.inspection.interfaces.rest;

/**
 * Thrown for malformed or semantically invalid request input.
 * Maps to HTTP 400 Bad Request.
 */
public class BadRequestException extends RuntimeException {

    public BadRequestException(String message) {
        super(message);
    }
}

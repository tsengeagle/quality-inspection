package com.tzuchi.his.quality.inspection.interfaces.rest.dto;

import java.util.Map;

/**
 * Standard API response envelope.
 * Aligned with api-spec.md §4: {success, data, meta} or {success, error}
 */
public class ApiResponse<T> {

    private boolean success;
    private T data;
    private Map<String, Object> meta;
    private ErrorInfo error;

    private ApiResponse() {}

    public static <T> ApiResponse<T> ok(T data) {
        ApiResponse<T> r = new ApiResponse<>();
        r.success = true;
        r.data = data;
        return r;
    }

    public static ApiResponse<Void> error(String code, String message) {
        ApiResponse<Void> r = new ApiResponse<>();
        r.success = false;
        r.error = new ErrorInfo(code, message);
        return r;
    }

    public boolean isSuccess() { return success; }
    public T getData() { return data; }
    public Map<String, Object> getMeta() { return meta; }
    public ErrorInfo getError() { return error; }

    public static class ErrorInfo {
        private String code;
        private String message;

        public ErrorInfo() {}

        public ErrorInfo(String code, String message) {
            this.code = code;
            this.message = message;
        }

        public String getCode() { return code; }
        public String getMessage() { return message; }
    }
}

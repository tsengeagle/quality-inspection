package com.tzuchi.his.quality.inspection.acceptance.http;

/** 測試期間觀察到的一次 HTTP 交換：方法、請求 URI（含 query）、狀態碼、回應本文。 */
public final class HttpObservation {

    private final String method;
    private final String uri;
    private final int status;
    private final String body;

    public HttpObservation(String method, String uri, int status, String body) {
        this.method = method;
        this.uri = uri;
        this.status = status;
        this.body = body;
    }

    public String method() { return method; }
    public String uri() { return uri; }
    public int status() { return status; }
    public String body() { return body; }

    @Override
    public String toString() {
        return status + " " + method + " " + uri;
    }
}

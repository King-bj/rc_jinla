package com.notifyhub.acl;

import java.util.Map;

/** 供应商 HTTP 请求描述，由 Converter 产出、Channel 消费。 */
public class VendorHttpRequest {

    private final String url;
    private final String method;
    private final Map<String, String> headers;
    private final String body;

    public VendorHttpRequest(String url, String method, Map<String, String> headers, String body) {
        this.url = url;
        this.method = method;
        this.headers = headers;
        this.body = body;
    }

    public String getUrl() {
        return url;
    }

    public String getMethod() {
        return method;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public String getBody() {
        return body;
    }
}

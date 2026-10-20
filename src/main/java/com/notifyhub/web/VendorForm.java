package com.notifyhub.web;

/** 供应商配置表单，同时带上预览用的示例报文。 */
public class VendorForm {

    private String code;
    private String name;
    private boolean enabled = true;
    private String method = "POST";
    private String urlTemplate = "http://127.0.0.1:8080/mock/demo";
    private String headersTemplate = "{\"Content-Type\":\"application/json\"}";
    private String bodyTemplate;
    private String sampleEventType = "PAYMENT_SUCCESS";
    private String sampleRequestId = "req-preview";
    private String samplePayload = "{\"userId\":\"1001\",\"status\":\"PAID\"}";

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getUrlTemplate() {
        return urlTemplate;
    }

    public void setUrlTemplate(String urlTemplate) {
        this.urlTemplate = urlTemplate;
    }

    public String getHeadersTemplate() {
        return headersTemplate;
    }

    public void setHeadersTemplate(String headersTemplate) {
        this.headersTemplate = headersTemplate;
    }

    public String getBodyTemplate() {
        return bodyTemplate;
    }

    public void setBodyTemplate(String bodyTemplate) {
        this.bodyTemplate = bodyTemplate;
    }

    public String getSampleEventType() {
        return sampleEventType;
    }

    public void setSampleEventType(String sampleEventType) {
        this.sampleEventType = sampleEventType;
    }

    public String getSampleRequestId() {
        return sampleRequestId;
    }

    public void setSampleRequestId(String sampleRequestId) {
        this.sampleRequestId = sampleRequestId;
    }

    public String getSamplePayload() {
        return samplePayload;
    }

    public void setSamplePayload(String samplePayload) {
        this.samplePayload = samplePayload;
    }
}

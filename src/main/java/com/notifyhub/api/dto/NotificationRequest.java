package com.notifyhub.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

/** 通知提交请求体，对应 POST /api/v1/notifications。 */
public class NotificationRequest {

    /** 业务幂等键，调用方自行生成并保证唯一 */
    @NotBlank
    private String requestId;

    /** 供应商编码，对应已配置且启用的出口 */
    @NotBlank
    private String targetSystem;

    @NotBlank
    private String eventType;

    /** 优先级，MVP 预留 */
    private Integer priority;

    /** 业务数据，结构由事件类型决定 */
    @NotNull
    private Map<String, Object> payload;

    /** 可选，透传至第三方 HTTP 请求头 */
    private Map<String, String> headers;

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getTargetSystem() {
        return targetSystem;
    }

    public void setTargetSystem(String targetSystem) {
        this.targetSystem = targetSystem;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    public Map<String, Object> getPayload() {
        return payload;
    }

    public void setPayload(Map<String, Object> payload) {
        this.payload = payload;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }
}

package com.notifyhub.domain;

import java.util.Map;

/**
 * 统一领域消息模型，屏蔽各第三方 API 的协议差异。
 * payload 保持业务灵活性，由 Converter 负责转换为供应商请求体。
 */
public class NotificationMessage {

    /** 业务幂等键，全局唯一 */
    private String requestId;
    /** 目标外部系统 */
    private TargetSystem targetSystem;
    /** 业务事件类型，如 PAYMENT_SUCCESS */
    private String eventType;
    /** 优先级（MVP 预留字段，暂未参与调度） */
    private Integer priority;
    /** 业务数据，结构由各事件自行约定 */
    private Map<String, Object> payload;
    /** 透传给第三方的 HTTP 头 */
    private Map<String, String> headers;

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public TargetSystem getTargetSystem() {
        return targetSystem;
    }

    public void setTargetSystem(TargetSystem targetSystem) {
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

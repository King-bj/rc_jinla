package com.notifyhub.delivery.contract;

import java.util.Map;

/**
 * 接入层标准化之后交给送出子系统的消息。
 * 缺少租户或事件身份时不能创建。
 */
public final class NormalizedMessage {

    private final String tenantId;
    private final String eventId;
    private final Map<String, Object> payload;

    public NormalizedMessage(String tenantId, String eventId) {
        this(tenantId, eventId, Map.of());
    }

    public NormalizedMessage(String tenantId, String eventId, Map<String, Object> payload) {
        this.tenantId = requireText(tenantId, "tenantId");
        this.eventId = requireText(eventId, "eventId");
        this.payload = payload == null ? Map.of() : Map.copyOf(payload);
    }

    public String tenantId() {
        return tenantId;
    }

    public String eventId() {
        return eventId;
    }

    public Map<String, Object> payload() {
        return payload;
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value;
    }
}

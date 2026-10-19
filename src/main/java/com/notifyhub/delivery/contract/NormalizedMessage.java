package com.notifyhub.delivery.contract;

/**
 * 接入层标准化之后交给送出子系统的消息。
 * 缺少租户或事件身份时不能创建。
 */
public final class NormalizedMessage {

    private final String tenantId;
    private final String eventId;

    public NormalizedMessage(String tenantId, String eventId) {
        this.tenantId = requireText(tenantId, "tenantId");
        this.eventId = requireText(eventId, "eventId");
    }

    public String tenantId() {
        return tenantId;
    }

    public String eventId() {
        return eventId;
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value;
    }
}

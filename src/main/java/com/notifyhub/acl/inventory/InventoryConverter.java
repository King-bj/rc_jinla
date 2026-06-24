package com.notifyhub.acl.inventory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.notifyhub.acl.NotificationConverter;
import com.notifyhub.acl.VendorHttpRequest;
import com.notifyhub.config.NotifyHubProperties;
import com.notifyhub.domain.NotificationMessage;
import com.notifyhub.domain.TargetSystem;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 库存系统防腐层：将统一消息转换为库存调整接口格式。
 * 请求体字段：event_type + items。
 */
@Component
public class InventoryConverter implements NotificationConverter {

    private final ObjectMapper objectMapper;
    private final String baseUrl;

    public InventoryConverter(ObjectMapper objectMapper, NotifyHubProperties properties) {
        this.objectMapper = objectMapper;
        this.baseUrl = properties.getChannels().get("inventory").getBaseUrl();
    }

    @Override
    public VendorHttpRequest convert(NotificationMessage message) {
        Map<String, Object> body = new HashMap<>();
        body.put("event_type", message.getEventType());
        body.put("items", message.getPayload());

        return new VendorHttpRequest(
                baseUrl + "/stock/adjust",
                "POST",
                mergeHeaders(message),
                toJson(body)
        );
    }

    private Map<String, String> mergeHeaders(NotificationMessage message) {
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        if (message.getHeaders() != null) {
            headers.putAll(message.getHeaders());
        }
        return headers;
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize inventory request", e);
        }
    }

    public TargetSystem supports() {
        return TargetSystem.INVENTORY;
    }
}

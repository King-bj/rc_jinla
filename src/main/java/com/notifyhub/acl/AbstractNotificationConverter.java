package com.notifyhub.acl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.notifyhub.config.NotifyHubProperties;
import com.notifyhub.domain.NotificationMessage;
import com.notifyhub.domain.TargetSystem;

import java.util.HashMap;
import java.util.Map;

/**
 * Converter 公共逻辑：渠道 baseUrl 解析、Header 合并与 JSON 序列化。
 */
public abstract class AbstractNotificationConverter implements NotificationConverter {

    private final ObjectMapper objectMapper;
    private final String baseUrl;

    protected AbstractNotificationConverter(ObjectMapper objectMapper,
                                          NotifyHubProperties properties,
                                          TargetSystem targetSystem) {
        this.objectMapper = objectMapper;
        this.baseUrl = properties.getChannelBaseUrl(targetSystem);
    }

    protected VendorHttpRequest post(String path, Map<String, Object> body, NotificationMessage message) {
        return new VendorHttpRequest(
                baseUrl + path,
                "POST",
                mergeHeaders(message),
                toJson(body)
        );
    }

    protected Map<String, String> mergeHeaders(NotificationMessage message) {
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        if (message.getHeaders() != null) {
            headers.putAll(message.getHeaders());
        }
        return headers;
    }

    protected String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize request for " + supports(), e);
        }
    }
}

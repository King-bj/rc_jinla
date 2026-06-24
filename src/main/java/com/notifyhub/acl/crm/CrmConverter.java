package com.notifyhub.acl.crm;

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
 * CRM 防腐层：将统一消息转换为 CRM 联系人更新接口格式。
 * 请求体字段：event + contact（对应业务 payload）。
 */
@Component
public class CrmConverter implements NotificationConverter {

    private final ObjectMapper objectMapper;
    private final String baseUrl;

    public CrmConverter(ObjectMapper objectMapper, NotifyHubProperties properties) {
        this.objectMapper = objectMapper;
        this.baseUrl = properties.getChannels().get("crm").getBaseUrl();
    }

    @Override
    public VendorHttpRequest convert(NotificationMessage message) {
        Map<String, Object> body = new HashMap<>();
        body.put("event", message.getEventType());
        body.put("contact", message.getPayload());

        return new VendorHttpRequest(
                baseUrl + "/contacts/update",
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
            throw new IllegalStateException("Failed to serialize CRM request", e);
        }
    }

    public TargetSystem supports() {
        return TargetSystem.CRM;
    }
}

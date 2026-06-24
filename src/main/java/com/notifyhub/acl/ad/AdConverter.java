package com.notifyhub.acl.ad;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.notifyhub.acl.AbstractNotificationConverter;
import com.notifyhub.acl.VendorHttpRequest;
import com.notifyhub.config.NotifyHubProperties;
import com.notifyhub.dispatcher.NotificationDispatcher;
import com.notifyhub.domain.NotificationMessage;
import com.notifyhub.domain.TargetSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 广告系统防腐层：将统一消息转换为转化上报接口格式。
 * 请求体字段：conversion_event + data。
 */
@Component
public class AdConverter extends AbstractNotificationConverter {
    private static final Logger log = LoggerFactory.getLogger(AdConverter.class);

    public AdConverter(ObjectMapper objectMapper, NotifyHubProperties properties) {
        super(objectMapper, properties, TargetSystem.AD);
    }

    @Override
    public TargetSystem supports() {
        return TargetSystem.AD;
    }

    @Override
    public VendorHttpRequest convert(NotificationMessage message) {
        log.debug("Ad message:{}",toJson(message));
        Map<String, Object> body = new HashMap<>();
        body.put("conversion_event", message.getEventType());
        body.put("data", message.getPayload());
        return post("/conversions", body, message);
    }
}

package com.notifyhub.acl.inventory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.notifyhub.acl.AbstractNotificationConverter;
import com.notifyhub.acl.VendorHttpRequest;
import com.notifyhub.config.NotifyHubProperties;
import com.notifyhub.domain.NotificationMessage;
import com.notifyhub.domain.TargetSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 库存系统防腐层：将统一消息转换为库存调整接口格式。
 * 请求体字段：event_type + items。
 */
@Component
public class InventoryConverter extends AbstractNotificationConverter {
    private static final Logger log = LoggerFactory.getLogger(InventoryConverter.class);

    public InventoryConverter(ObjectMapper objectMapper, NotifyHubProperties properties) {
        super(objectMapper, properties, TargetSystem.INVENTORY);
    }

    @Override
    public TargetSystem supports() {
        return TargetSystem.INVENTORY;
    }

    @Override
    public VendorHttpRequest convert(NotificationMessage message) {
        log.debug("Inventory message:{}",toJson(message));

        Map<String, Object> body = new HashMap<>();
        body.put("event_type", message.getEventType());
        body.put("items", message.getPayload());
        return post("/stock/adjust", body, message);
    }
}

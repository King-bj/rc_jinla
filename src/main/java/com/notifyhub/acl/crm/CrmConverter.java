package com.notifyhub.acl.crm;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.notifyhub.acl.AbstractNotificationConverter;
import com.notifyhub.config.NotifyHubProperties;
import com.notifyhub.acl.VendorHttpRequest;
import com.notifyhub.dispatcher.NotificationDispatcher;
import com.notifyhub.domain.NotificationMessage;
import com.notifyhub.domain.TargetSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * CRM 防腐层：将统一消息转换为 CRM 联系人更新接口格式。
 * 请求体字段：event + contact（对应业务 payload）。
 */
@Component
public class CrmConverter extends AbstractNotificationConverter {
    private static final Logger log = LoggerFactory.getLogger(CrmConverter.class);

    public CrmConverter(ObjectMapper objectMapper, NotifyHubProperties properties) {
        super(objectMapper, properties, TargetSystem.CRM);
    }

    @Override
    public TargetSystem supports() {
        return TargetSystem.CRM;
    }

    @Override
    public VendorHttpRequest convert(NotificationMessage message) {
        log.debug("Crm message:{}",toJson(message));
        Map<String, Object> body = new HashMap<>();
        body.put("event", message.getEventType());
        body.put("contact", message.getPayload());
        return post("/contacts/update", body, message);
    }
}

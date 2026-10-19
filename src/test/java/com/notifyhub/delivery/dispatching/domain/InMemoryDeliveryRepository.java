package com.notifyhub.delivery.dispatching.domain;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/** 测试用内存仓储。同一个租户和事件只能保存一次。 */
public final class InMemoryDeliveryRepository implements DeliveryRepository {

    private final Map<String, InboundMessage> messages = new HashMap<>();

    @Override
    public Optional<InboundMessage> find(String tenantId, String eventId) {
        return Optional.ofNullable(messages.get(key(tenantId, eventId)));
    }

    @Override
    public void save(InboundMessage message) {
        String key = key(message.tenantId(), message.eventId());
        if (messages.containsKey(key)) {
            throw new IllegalStateException("message already exists");
        }
        messages.put(key, message);
    }

    public int size() {
        return messages.size();
    }

    private static String key(String tenantId, String eventId) {
        return tenantId + "\0" + eventId;
    }
}

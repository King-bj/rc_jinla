package com.notifyhub.delivery.dispatching.domain;

import java.util.Optional;

/** 按租户和事件保存已展开的消息。 */
public interface DeliveryRepository {

    Optional<InboundMessage> find(String tenantId, String eventId);

    void save(InboundMessage message);
}

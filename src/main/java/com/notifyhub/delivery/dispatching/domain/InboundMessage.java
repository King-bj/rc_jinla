package com.notifyhub.delivery.dispatching.domain;

import java.util.List;

/** 创建阶段的聚合根。一次持有同一事件展开出的全部任务。 */
public final class InboundMessage {

    private final String tenantId;
    private final String eventId;
    private final List<DeliveryTask> tasks;

    public InboundMessage(String tenantId, String eventId, List<DeliveryTask> tasks) {
        this.tenantId = tenantId;
        this.eventId = eventId;
        this.tasks = List.copyOf(tasks);
    }

    public String tenantId() {
        return tenantId;
    }

    public String eventId() {
        return eventId;
    }

    public List<DeliveryTask> tasks() {
        return tasks;
    }

    public AggregateStatus aggregateStatus() {
        return AggregateStatus.from(tasks);
    }
}

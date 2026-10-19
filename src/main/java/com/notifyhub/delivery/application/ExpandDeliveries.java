package com.notifyhub.delivery.application;

import com.notifyhub.delivery.contract.NormalizedMessage;
import com.notifyhub.delivery.dispatching.domain.DeliveryRepository;
import com.notifyhub.delivery.dispatching.domain.DeliveryTask;
import com.notifyhub.delivery.dispatching.domain.InboundMessage;
import com.notifyhub.delivery.routing.domain.DeliveryPlan;
import com.notifyhub.delivery.routing.domain.RouteDestination;

import java.util.ArrayList;
import java.util.List;

/** 把一条消息按投递计划一次展开成全部任务。重复事件直接返回已有记录。 */
public final class ExpandDeliveries {

    private final DeliveryRepository repository;

    public ExpandDeliveries(DeliveryRepository repository) {
        this.repository = repository;
    }

    public InboundMessage expand(NormalizedMessage message, DeliveryPlan plan) {
        return repository.find(message.tenantId(), message.eventId())
                .orElseGet(() -> create(message, plan));
    }

    private InboundMessage create(NormalizedMessage message, DeliveryPlan plan) {
        List<DeliveryTask> tasks = new ArrayList<>();
        for (RouteDestination destination : plan.destinations()) {
            tasks.add(DeliveryTask.queued(
                    message.tenantId(),
                    message.eventId(),
                    destination.id(),
                    destination.configVersion()));
        }
        InboundMessage created = new InboundMessage(message.tenantId(), message.eventId(), tasks);
        repository.save(created);
        return created;
    }
}

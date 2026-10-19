package com.notifyhub.delivery.application;

import com.notifyhub.delivery.dispatching.domain.DeliveryTask;
import com.notifyhub.delivery.dispatching.domain.InboundMessage;
import com.notifyhub.delivery.dispatching.domain.TaskStatus;

/** 只把死信任务放回队列。其他状态保持原样。 */
public final class ReplayDelivery {

    public void replay(InboundMessage message, String destinationId) {
        DeliveryTask task = message.tasks().stream()
                .filter(item -> destinationId.equals(item.destinationId()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("destination not found"));
        if (task.status() != TaskStatus.DEAD) {
            throw new IllegalStateException("only DEAD tasks can be replayed");
        }
        task.replay();
    }
}

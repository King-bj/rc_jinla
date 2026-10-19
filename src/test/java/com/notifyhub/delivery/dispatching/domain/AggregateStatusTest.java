package com.notifyhub.delivery.dispatching.domain;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AggregateStatusTest {

    @Test
    void oneSuccessAndOneDeadIsPartial() {
        InboundMessage message = new InboundMessage("tenant", "event", List.of(
                new DeliveryTask(TaskStatus.SUCCEEDED),
                new DeliveryTask(TaskStatus.DEAD)));

        assertEquals(AggregateStatus.PARTIAL, message.aggregateStatus());
    }
}

package com.notifyhub.delivery.application;

import com.notifyhub.delivery.contract.NormalizedMessage;
import com.notifyhub.delivery.dispatching.domain.DeliveryTask;
import com.notifyhub.delivery.dispatching.domain.InMemoryDeliveryRepository;
import com.notifyhub.delivery.dispatching.domain.InboundMessage;
import com.notifyhub.delivery.dispatching.domain.TaskStatus;
import com.notifyhub.delivery.routing.domain.DeliveryPlan;
import com.notifyhub.delivery.routing.domain.RouteDestination;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class ExpandDeliveriesTest {

    @Test
    void duplicateEventDoesNotDuplicateTasks() {
        InMemoryDeliveryRepository repository = new InMemoryDeliveryRepository();
        ExpandDeliveries expand = new ExpandDeliveries(repository);
        NormalizedMessage message = new NormalizedMessage("tenant", "event");
        DeliveryPlan plan = new DeliveryPlan(List.of(
                new RouteDestination("crm", 1),
                new RouteDestination("inventory", 3)));

        InboundMessage first = expand.expand(message, plan);
        InboundMessage second = expand.expand(message, plan);

        assertSame(first, second);
        assertEquals(1, repository.size());
        assertEquals(2, second.tasks().size());
        assertEquals(TaskStatus.QUEUED, second.tasks().get(0).status());
        assertEquals("tenant:event:crm", second.tasks().get(0).idempotencyKey().value());
        assertEquals("tenant:event:inventory", second.tasks().get(1).idempotencyKey().value());
    }
}

package com.notifyhub.delivery.application;

import com.notifyhub.delivery.contract.NormalizedMessage;
import com.notifyhub.delivery.dispatching.domain.InMemoryDeliveryRepository;
import com.notifyhub.delivery.dispatching.domain.InboundMessage;
import com.notifyhub.delivery.routing.domain.DeliveryPlan;
import com.notifyhub.delivery.routing.domain.RouteDestination;
import com.notifyhub.delivery.routing.domain.RoutePlanner;
import com.notifyhub.delivery.routing.domain.RouteRule;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ConfigVersionPinTest {

    @Test
    void existingTaskKeepsConfigVersion() {
        RoutePlanner planner = new RoutePlanner();
        ExpandDeliveries expand = new ExpandDeliveries(new InMemoryDeliveryRepository());
        NormalizedMessage message = new NormalizedMessage("tenant", "event");
        DeliveryPlan createdWith = planner.plan(message, List.of(
                new RouteRule(true, "tenant", List.of(new RouteDestination("crm", 4)))));

        InboundMessage stored = expand.expand(message, createdWith);
        DeliveryPlan updated = planner.plan(message, List.of(
                new RouteRule(true, "tenant", List.of(new RouteDestination("crm", 9)))));

        assertEquals(9, updated.destinations().get(0).configVersion());
        assertEquals(4, stored.tasks().get(0).configVersion());
    }
}

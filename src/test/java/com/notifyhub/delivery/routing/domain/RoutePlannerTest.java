package com.notifyhub.delivery.routing.domain;

import com.notifyhub.delivery.contract.NormalizedMessage;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RoutePlannerTest {

    private final RoutePlanner planner = new RoutePlanner();
    private final NormalizedMessage message = new NormalizedMessage("tenant", "event");

    @Test
    void matchesSingleDestinationWithConfigVersion() {
        RouteRule rule = new RouteRule(true, "tenant", List.of(new RouteDestination("billing", 7)));

        DeliveryPlan plan = planner.plan(message, List.of(rule));

        assertEquals(1, plan.destinations().size());
        assertEquals("billing", plan.destinations().get(0).id());
        assertEquals(7, plan.destinations().get(0).configVersion());
    }

    @Test
    void fanOutDedupesDestination() {
        RouteRule first = new RouteRule(true, "tenant", List.of(new RouteDestination("crm", 1)));
        RouteRule second = new RouteRule(true, "tenant", List.of(
                new RouteDestination("crm", 2),
                new RouteDestination("inventory", 3)));

        DeliveryPlan plan = planner.plan(message, List.of(first, second));

        assertEquals(2, plan.destinations().size());
        assertEquals("crm", plan.destinations().get(0).id());
        assertEquals(1, plan.destinations().get(0).configVersion());
        assertEquals("inventory", plan.destinations().get(1).id());
        assertEquals(3, plan.destinations().get(1).configVersion());
    }

    @Test
    void unmatchedProducesNoTasks() {
        RouteRule disabled = new RouteRule(false, "tenant", List.of(new RouteDestination("crm", 1)));
        RouteRule otherTenant = new RouteRule(true, "other", List.of(new RouteDestination("crm", 1)));

        DeliveryPlan plan = planner.plan(message, List.of(disabled, otherTenant));

        assertTrue(plan.destinations().isEmpty());
    }
}

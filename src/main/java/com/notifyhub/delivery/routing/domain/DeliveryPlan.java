package com.notifyhub.delivery.routing.domain;

import java.util.List;

/** 一条消息展开前的目的地计划。未命中时目的地列表为空。 */
public final class DeliveryPlan {

    private final List<RouteDestination> destinations;

    public DeliveryPlan(List<RouteDestination> destinations) {
        this.destinations = List.copyOf(destinations);
    }

    public List<RouteDestination> destinations() {
        return destinations;
    }
}

package com.notifyhub.delivery.routing.domain;

import com.notifyhub.delivery.contract.NormalizedMessage;

import java.util.List;

/** 一条路由规则。只在启用且租户相同的时候命中。 */
public final class RouteRule {

    private final boolean enabled;
    private final String tenantId;
    private final List<RouteDestination> destinations;

    public RouteRule(boolean enabled, String tenantId, List<RouteDestination> destinations) {
        if (tenantId == null || tenantId.isBlank()) {
            throw new IllegalArgumentException("tenantId is required");
        }
        this.enabled = enabled;
        this.tenantId = tenantId;
        this.destinations = List.copyOf(destinations);
    }

    public boolean matches(NormalizedMessage message) {
        return enabled && tenantId.equals(message.tenantId());
    }

    public List<RouteDestination> destinations() {
        return destinations;
    }
}

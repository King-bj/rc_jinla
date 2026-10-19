package com.notifyhub.delivery.routing.domain;

import com.notifyhub.delivery.contract.NormalizedMessage;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 按启用规则生成投递计划。
 * 同一个目的地被多条规则命中时保留先出现的配置版本。
 */
public final class RoutePlanner {

    public DeliveryPlan plan(NormalizedMessage message, List<RouteRule> rules) {
        Map<String, RouteDestination> unique = new LinkedHashMap<>();
        for (RouteRule rule : rules) {
            if (!rule.matches(message)) {
                continue;
            }
            for (RouteDestination destination : rule.destinations()) {
                unique.putIfAbsent(destination.id(), destination);
            }
        }
        return new DeliveryPlan(new ArrayList<>(unique.values()));
    }
}

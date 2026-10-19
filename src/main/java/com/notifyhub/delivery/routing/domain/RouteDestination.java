package com.notifyhub.delivery.routing.domain;

/** 规则命中的一个目的地，以及创建任务时要钉住的配置版本。 */
public final class RouteDestination {

    private final String id;
    private final long configVersion;

    public RouteDestination(String id, long configVersion) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("destination id is required");
        }
        this.id = id;
        this.configVersion = configVersion;
    }

    public String id() {
        return id;
    }

    public long configVersion() {
        return configVersion;
    }
}

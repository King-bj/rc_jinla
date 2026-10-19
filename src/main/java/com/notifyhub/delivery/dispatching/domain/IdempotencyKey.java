package com.notifyhub.delivery.dispatching.domain;

import java.util.Objects;

/** 由租户、事件和目的地组成的稳定幂等键。 */
public final class IdempotencyKey {

    private final String value;

    private IdempotencyKey(String value) {
        this.value = value;
    }

    public static IdempotencyKey of(String tenantId, String eventId, String destinationId) {
        return new IdempotencyKey(tenantId + ":" + eventId + ":" + destinationId);
    }

    public String value() {
        return value;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof IdempotencyKey that)) {
            return false;
        }
        return value.equals(that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }
}

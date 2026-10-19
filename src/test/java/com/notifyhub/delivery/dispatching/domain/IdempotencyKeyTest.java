package com.notifyhub.delivery.dispatching.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class IdempotencyKeyTest {

    @Test
    void sameInputSameKey() {
        IdempotencyKey first = IdempotencyKey.of("tenant", "event", "dest");
        IdempotencyKey second = IdempotencyKey.of("tenant", "event", "dest");

        assertEquals(first, second);
        assertEquals("tenant:event:dest", first.value());
    }
}

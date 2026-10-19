package com.notifyhub.delivery.contract;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class NormalizedMessageTest {

    @Test
    void rejectsMissingIdentity() {
        assertThrows(IllegalArgumentException.class, () -> new NormalizedMessage(null, "event"));
        assertThrows(IllegalArgumentException.class, () -> new NormalizedMessage(" ", "event"));
        assertThrows(IllegalArgumentException.class, () -> new NormalizedMessage("tenant", null));
        assertThrows(IllegalArgumentException.class, () -> new NormalizedMessage("tenant", ""));
    }

    @Test
    void keepsTenantAndEvent() {
        NormalizedMessage message = new NormalizedMessage("tenant", "event");

        assertEquals("tenant", message.tenantId());
        assertEquals("event", message.eventId());
    }
}

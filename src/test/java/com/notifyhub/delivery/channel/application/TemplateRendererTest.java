package com.notifyhub.delivery.channel.application;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TemplateRendererTest {

    private final TemplateRenderer renderer = new TemplateRenderer();

    @Test
    void replacesPayloadAndIdempotencyKey() {
        String rendered = renderer.render(
                "user={{payload.userId}} key={{task.idempotencyKey}}",
                Map.of("userId", "1001"),
                "tenant:event:crm");

        assertEquals("user=1001 key=tenant:event:crm", rendered);
    }

    @Test
    void missingVariableRejected() {
        assertThrows(IllegalArgumentException.class, () -> renderer.render(
                "user={{payload.userId}}",
                Map.of(),
                "tenant:event:crm"));
    }
}

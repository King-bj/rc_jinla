package com.notifyhub.delivery.record;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DeliveryMetricsTest {

    @Test
    void eachOutcomeIncrementsItsCounterOnce() {
        DeliveryMetrics metrics = new DeliveryMetrics();

        metrics.recordSuccess();
        metrics.recordRetry();
        metrics.recordDead();
        metrics.recordCircuitOpen();

        assertEquals(1, metrics.successCount());
        assertEquals(1, metrics.retryCount());
        assertEquals(1, metrics.deadCount());
        assertEquals(1, metrics.circuitOpenCount());
    }
}

package com.notifyhub.delivery.record;

import java.util.concurrent.atomic.AtomicLong;

/** 投递结果计数：成功、重试、死信、熔断打开。 */
public final class DeliveryMetrics {

    private final AtomicLong success = new AtomicLong();
    private final AtomicLong retry = new AtomicLong();
    private final AtomicLong dead = new AtomicLong();
    private final AtomicLong circuitOpen = new AtomicLong();

    public void recordSuccess() {
        success.incrementAndGet();
    }

    public void recordRetry() {
        retry.incrementAndGet();
    }

    public void recordDead() {
        dead.incrementAndGet();
    }

    public void recordCircuitOpen() {
        circuitOpen.incrementAndGet();
    }

    public long successCount() {
        return success.get();
    }

    public long retryCount() {
        return retry.get();
    }

    public long deadCount() {
        return dead.get();
    }

    public long circuitOpenCount() {
        return circuitOpen.get();
    }
}

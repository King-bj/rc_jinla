package com.notifyhub.delivery.dispatching.domain;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DeliveryTaskClaimTest {

    @Test
    void onlyOneWorkerClaims() throws Exception {
        DeliveryTask task = DeliveryTask.queued("tenant", "event", "crm", 1);
        Instant now = Instant.parse("2026-10-19T09:00:00Z");
        Duration lease = Duration.ofSeconds(30);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(2);
        AtomicInteger wins = new AtomicInteger();

        Runnable claim = () -> {
            ready.countDown();
            try {
                ready.await();
                start.await();
                if (task.claim(now, lease)) {
                    wins.incrementAndGet();
                }
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            } finally {
                done.countDown();
            }
        };

        try {
            pool.submit(claim);
            pool.submit(claim);
            start.countDown();
            done.await();
        } finally {
            pool.shutdownNow();
        }

        assertEquals(1, wins.get());
        assertEquals(TaskStatus.SENDING, task.status());
        assertEquals(now.plus(lease), task.leaseUntil());
    }
}

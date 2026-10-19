package com.notifyhub.delivery.dispatching.domain;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class RetryScheduleTest {

    @Test
    void lastRetryableFailureBecomesDead() {
        List<Duration> intervals = List.of(Duration.ofSeconds(1), Duration.ofSeconds(5));
        Instant now = Instant.parse("2026-10-19T09:00:00Z");
        DeliveryTask task = DeliveryTask.queued("tenant", "event", "crm", 1);

        task.transitionTo(TaskStatus.SENDING);
        task.recordRetryableFailure(intervals, now);
        assertEquals(TaskStatus.RETRY_WAIT, task.status());
        assertEquals(now.plusSeconds(1), task.nextAttemptAt());

        task.transitionTo(TaskStatus.SENDING);
        task.recordRetryableFailure(intervals, now.plusSeconds(1));
        assertEquals(TaskStatus.RETRY_WAIT, task.status());
        assertEquals(now.plusSeconds(6), task.nextAttemptAt());

        task.transitionTo(TaskStatus.SENDING);
        task.recordRetryableFailure(intervals, now.plusSeconds(6));
        assertEquals(TaskStatus.DEAD, task.status());
        assertNull(task.nextAttemptAt());
    }
}

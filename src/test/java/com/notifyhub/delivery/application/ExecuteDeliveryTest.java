package com.notifyhub.delivery.application;

import com.notifyhub.delivery.channel.domain.AdapterType;
import com.notifyhub.delivery.channel.domain.ChannelAdapter;
import com.notifyhub.delivery.channel.domain.HttpOutcomeClassifier;
import com.notifyhub.delivery.channel.domain.RenderedRequest;
import com.notifyhub.delivery.channel.domain.SendOutcome;
import com.notifyhub.delivery.dispatching.domain.DeliveryTask;
import com.notifyhub.delivery.dispatching.domain.TaskStatus;
import com.notifyhub.delivery.record.domain.AttemptLog;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExecuteDeliveryTest {

    private static final Instant NOW = Instant.parse("2026-10-19T09:00:00Z");
    private static final List<Duration> INTERVALS = List.of(Duration.ofSeconds(1), Duration.ofSeconds(5));

    @Test
    void successRecordsAttempt() {
        ScriptedAdapter adapter = new ScriptedAdapter(SendOutcome.SUCCESS);
        AttemptLog attemptLog = new AttemptLog();
        ExecuteDelivery execute = new ExecuteDelivery(adapter, attemptLog, Duration.ofSeconds(30), INTERVALS);
        DeliveryTask task = DeliveryTask.queued("tenant", "event", "crm", 4);

        assertTrue(execute.execute(task, NOW));

        assertEquals(TaskStatus.SUCCEEDED, task.status());
        assertEquals(1, attemptLog.attempts().size());
        assertEquals("tenant:event:crm", attemptLog.attempts().get(0).idempotencyKey());
        assertEquals(SendOutcome.SUCCESS, attemptLog.attempts().get(0).outcome());
    }

    @Test
    void timeoutKeepsIdempotencyKey() {
        ScriptedAdapter adapter = new ScriptedAdapter(SendOutcome.RETRYABLE, SendOutcome.RETRYABLE);
        ExecuteDelivery execute = new ExecuteDelivery(adapter, new AttemptLog(), Duration.ofSeconds(30), INTERVALS);
        DeliveryTask task = DeliveryTask.queued("tenant", "event", "crm", 4);

        execute.execute(task, NOW);
        execute.execute(task, task.nextAttemptAt());

        assertEquals(TaskStatus.RETRY_WAIT, task.status());
        assertEquals(List.of("tenant:event:crm", "tenant:event:crm"), adapter.keys);
        assertEquals("tenant:event:crm", task.idempotencyKey().value());
    }

    @Test
    void terminalFailureSkipsBackoff() {
        HttpOutcomeClassifier classifier = new HttpOutcomeClassifier();
        SendOutcome outcome = classifier.classify(400, false, Set.of(200, 204));
        ScriptedAdapter adapter = new ScriptedAdapter(outcome);
        ExecuteDelivery execute = new ExecuteDelivery(adapter, new AttemptLog(), Duration.ofSeconds(30), INTERVALS);
        DeliveryTask task = DeliveryTask.queued("tenant", "event", "crm", 4);

        execute.execute(task, NOW);

        assertEquals(SendOutcome.TERMINAL, outcome);
        assertEquals(TaskStatus.DEAD, task.status());
        assertNull(task.nextAttemptAt());
    }

    private static final class ScriptedAdapter implements ChannelAdapter {
        private final List<SendOutcome> outcomes;
        private final List<String> keys = new ArrayList<>();
        private int index;

        private ScriptedAdapter(SendOutcome... outcomes) {
            this.outcomes = List.of(outcomes);
        }

        @Override
        public AdapterType type() {
            return AdapterType.HTTP;
        }

        @Override
        public SendOutcome send(RenderedRequest request) {
            keys.add(request.idempotencyKey());
            return outcomes.get(index++);
        }
    }
}

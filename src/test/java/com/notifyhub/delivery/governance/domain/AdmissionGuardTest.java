package com.notifyhub.delivery.governance.domain;

import com.notifyhub.delivery.application.ExecuteDelivery;
import com.notifyhub.delivery.channel.domain.AdapterType;
import com.notifyhub.delivery.channel.domain.ChannelAdapter;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdmissionGuardTest {

    private static final Instant NOW = Instant.parse("2026-10-19T09:00:00Z");
    private static final List<Duration> INTERVALS = List.of(Duration.ofSeconds(1));

    @Test
    void fullDestinationDoesNotBlockAnother() {
        InMemoryAdmissionGuard admission = new InMemoryAdmissionGuard();
        admission.limit("crm", 1);
        admission.limit("inventory", 1);
        assertEquals(Admission.ALLOW, admission.evaluate("crm"));

        CountingAdapter adapter = new CountingAdapter(SendOutcome.SUCCESS);
        ExecuteDelivery execute = new ExecuteDelivery(adapter, new AttemptLog(), Duration.ofSeconds(30), INTERVALS, admission);
        DeliveryTask blocked = DeliveryTask.queued("tenant", "event-a", "crm", 1);
        DeliveryTask other = DeliveryTask.queued("tenant", "event-b", "inventory", 1);

        assertFalse(execute.execute(blocked, NOW));
        assertTrue(execute.execute(other, NOW));

        assertEquals(TaskStatus.QUEUED, blocked.status());
        assertEquals(TaskStatus.SUCCEEDED, other.status());
        assertEquals(List.of("inventory"), adapter.destinations);
    }

    @Test
    void openCircuitSkipsNonCriticalAndDefersCritical() {
        InMemoryAdmissionGuard admission = new InMemoryAdmissionGuard();
        admission.critical("crm", false);
        admission.critical("billing", true);
        admission.openCircuit("crm");
        admission.openCircuit("billing");

        CountingAdapter adapter = new CountingAdapter(SendOutcome.SUCCESS);
        ExecuteDelivery execute = new ExecuteDelivery(adapter, new AttemptLog(), Duration.ofSeconds(30), INTERVALS, admission);
        DeliveryTask optional = DeliveryTask.queued("tenant", "event-a", "crm", 1);
        DeliveryTask required = DeliveryTask.queued("tenant", "event-b", "billing", 1);

        assertFalse(execute.execute(optional, NOW));
        assertFalse(execute.execute(required, NOW));

        assertEquals(TaskStatus.SKIPPED, optional.status());
        assertEquals(TaskStatus.RETRY_WAIT, required.status());
        assertEquals(NOW.plusSeconds(1), required.nextAttemptAt());
        assertTrue(adapter.destinations.isEmpty());
    }

    private static final class CountingAdapter implements ChannelAdapter {
        private final SendOutcome outcome;
        private final List<String> destinations = new ArrayList<>();

        private CountingAdapter(SendOutcome outcome) {
            this.outcome = outcome;
        }

        @Override
        public AdapterType type() {
            return AdapterType.HTTP;
        }

        @Override
        public SendOutcome send(RenderedRequest request) {
            destinations.add(request.target());
            return outcome;
        }
    }
}

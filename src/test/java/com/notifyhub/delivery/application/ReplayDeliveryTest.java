package com.notifyhub.delivery.application;

import com.notifyhub.delivery.dispatching.domain.DeliveryTask;
import com.notifyhub.delivery.dispatching.domain.InboundMessage;
import com.notifyhub.delivery.dispatching.domain.TaskStatus;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ReplayDeliveryTest {

    @Test
    void replayOnlyDeadTask() {
        DeliveryTask dead = DeliveryTask.queued("tenant", "event", "crm", 4);
        dead.transitionTo(TaskStatus.SENDING);
        dead.recordTerminalFailure();
        DeliveryTask succeeded = DeliveryTask.queued("tenant", "event", "billing", 4);
        succeeded.transitionTo(TaskStatus.SENDING);
        succeeded.markSucceeded();
        InboundMessage message = new InboundMessage("tenant", "event", List.of(dead, succeeded));
        ReplayDelivery replay = new ReplayDelivery();

        replay.replay(message, "crm");

        assertEquals(TaskStatus.QUEUED, dead.status());
        assertEquals(TaskStatus.SUCCEEDED, succeeded.status());
        assertThrows(IllegalStateException.class, () -> replay.replay(message, "billing"));
        assertEquals(TaskStatus.SUCCEEDED, succeeded.status());
    }
}

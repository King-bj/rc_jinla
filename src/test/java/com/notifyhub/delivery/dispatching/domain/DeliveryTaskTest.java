package com.notifyhub.delivery.dispatching.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DeliveryTaskTest {

    @Test
    void succeededCannotReturnToSending() {
        DeliveryTask task = new DeliveryTask(TaskStatus.SUCCEEDED);

        assertThrows(IllegalStateException.class, () -> task.transitionTo(TaskStatus.SENDING));
        assertEquals(TaskStatus.SUCCEEDED, task.status());
    }
}

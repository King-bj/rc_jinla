package com.notifyhub.delivery.dispatching.domain;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/** 发出阶段的任务聚合。只接受清单内的状态迁移。 */
public final class DeliveryTask {

    private static final Map<TaskStatus, Set<TaskStatus>> ALLOWED = allowed();

    private TaskStatus status;

    public DeliveryTask(TaskStatus status) {
        if (status == null) {
            throw new IllegalArgumentException("status is required");
        }
        this.status = status;
    }

    public TaskStatus status() {
        return status;
    }

    public void transitionTo(TaskStatus next) {
        Set<TaskStatus> allowed = ALLOWED.get(status);
        if (next == null || allowed == null || !allowed.contains(next)) {
            throw new IllegalStateException("Cannot transition from " + status + " to " + next);
        }
        this.status = next;
    }

    private static Map<TaskStatus, Set<TaskStatus>> allowed() {
        Map<TaskStatus, Set<TaskStatus>> transitions = new EnumMap<>(TaskStatus.class);
        transitions.put(TaskStatus.QUEUED, EnumSet.of(TaskStatus.SENDING));
        transitions.put(TaskStatus.SENDING, EnumSet.of(
                TaskStatus.SUCCEEDED, TaskStatus.RETRY_WAIT, TaskStatus.DEAD, TaskStatus.SKIPPED));
        transitions.put(TaskStatus.RETRY_WAIT, EnumSet.of(TaskStatus.SENDING));
        transitions.put(TaskStatus.SUCCEEDED, EnumSet.noneOf(TaskStatus.class));
        transitions.put(TaskStatus.DEAD, EnumSet.noneOf(TaskStatus.class));
        transitions.put(TaskStatus.SKIPPED, EnumSet.noneOf(TaskStatus.class));
        return transitions;
    }
}

package com.notifyhub.delivery.dispatching.domain;

import java.time.Duration;
import java.time.Instant;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** 发出阶段的任务聚合。只接受清单内的状态迁移。 */
public final class DeliveryTask {

    private static final Map<TaskStatus, Set<TaskStatus>> ALLOWED = allowed();

    private TaskStatus status;
    private String destinationId;
    private long configVersion;
    private IdempotencyKey idempotencyKey;
    private int failureCount;
    private Instant nextAttemptAt;

    public DeliveryTask(TaskStatus status) {
        if (status == null) {
            throw new IllegalArgumentException("status is required");
        }
        this.status = status;
    }

    public static DeliveryTask queued(String tenantId, String eventId, String destinationId, long configVersion) {
        DeliveryTask task = new DeliveryTask(TaskStatus.QUEUED);
        task.destinationId = destinationId;
        task.configVersion = configVersion;
        task.idempotencyKey = IdempotencyKey.of(tenantId, eventId, destinationId);
        return task;
    }

    public TaskStatus status() {
        return status;
    }

    public String destinationId() {
        return destinationId;
    }

    public long configVersion() {
        return configVersion;
    }

    public IdempotencyKey idempotencyKey() {
        return idempotencyKey;
    }

    public int failureCount() {
        return failureCount;
    }

    public Instant nextAttemptAt() {
        return nextAttemptAt;
    }

    /**
     * 记录一次可重试失败。间隔用完之前进入 RETRY_WAIT，用完之后进入 DEAD。
     * 调用时任务必须处于 SENDING。
     */
    public void recordRetryableFailure(List<Duration> intervals, Instant now) {
        int nextCount = failureCount + 1;
        if (nextCount > intervals.size()) {
            transitionTo(TaskStatus.DEAD);
            failureCount = nextCount;
            nextAttemptAt = null;
            return;
        }
        transitionTo(TaskStatus.RETRY_WAIT);
        failureCount = nextCount;
        nextAttemptAt = now.plus(intervals.get(nextCount - 1));
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

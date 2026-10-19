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
    private Instant leaseUntil;

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

    public Instant leaseUntil() {
        return leaseUntil;
    }

    /** 条件认领。已有未过期租约时返回 false，调用方不能再发送。 */
    public synchronized boolean claim(Instant now, Duration lease) {
        if (!canClaim(now)) {
            return false;
        }
        if (status != TaskStatus.SENDING) {
            transitionTo(TaskStatus.SENDING);
        }
        leaseUntil = now.plus(lease);
        return true;
    }

    public synchronized void markSucceeded() {
        transitionTo(TaskStatus.SUCCEEDED);
        nextAttemptAt = null;
        leaseUntil = null;
    }

    public synchronized void recordTerminalFailure() {
        transitionTo(TaskStatus.DEAD);
        nextAttemptAt = null;
        leaseUntil = null;
    }

    public synchronized void replay() {
        transitionTo(TaskStatus.QUEUED);
        nextAttemptAt = null;
        leaseUntil = null;
    }

    public synchronized void skip() {
        if (status != TaskStatus.SKIPPED) {
            transitionTo(TaskStatus.SKIPPED);
        }
        nextAttemptAt = null;
        leaseUntil = null;
    }

    public synchronized void defer(Instant when) {
        if (status != TaskStatus.RETRY_WAIT) {
            transitionTo(TaskStatus.RETRY_WAIT);
        }
        nextAttemptAt = when;
        leaseUntil = null;
    }

    /**
     * 记录一次可重试失败。间隔用完之前进入 RETRY_WAIT，用完之后进入 DEAD。
     * 调用时任务必须处于 SENDING。
     */
    public synchronized void recordRetryableFailure(List<Duration> intervals, Instant now) {
        int nextCount = failureCount + 1;
        if (nextCount > intervals.size()) {
            transitionTo(TaskStatus.DEAD);
            failureCount = nextCount;
            nextAttemptAt = null;
            leaseUntil = null;
            return;
        }
        transitionTo(TaskStatus.RETRY_WAIT);
        failureCount = nextCount;
        nextAttemptAt = now.plus(intervals.get(nextCount - 1));
        leaseUntil = null;
    }

    private boolean canClaim(Instant now) {
        if (status == TaskStatus.QUEUED) {
            return true;
        }
        if (status == TaskStatus.RETRY_WAIT) {
            return nextAttemptAt == null || !nextAttemptAt.isAfter(now);
        }
        if (status == TaskStatus.SENDING) {
            return leaseUntil == null || !leaseUntil.isAfter(now);
        }
        return false;
    }

    public synchronized void transitionTo(TaskStatus next) {
        Set<TaskStatus> allowed = ALLOWED.get(status);
        if (next == null || allowed == null || !allowed.contains(next)) {
            throw new IllegalStateException("Cannot transition from " + status + " to " + next);
        }
        this.status = next;
    }

    private static Map<TaskStatus, Set<TaskStatus>> allowed() {
        Map<TaskStatus, Set<TaskStatus>> transitions = new EnumMap<>(TaskStatus.class);
        transitions.put(TaskStatus.QUEUED, EnumSet.of(TaskStatus.SENDING, TaskStatus.SKIPPED, TaskStatus.RETRY_WAIT));
        transitions.put(TaskStatus.SENDING, EnumSet.of(
                TaskStatus.SUCCEEDED, TaskStatus.RETRY_WAIT, TaskStatus.DEAD, TaskStatus.SKIPPED));
        transitions.put(TaskStatus.RETRY_WAIT, EnumSet.of(TaskStatus.SENDING, TaskStatus.SKIPPED));
        transitions.put(TaskStatus.SUCCEEDED, EnumSet.noneOf(TaskStatus.class));
        transitions.put(TaskStatus.DEAD, EnumSet.of(TaskStatus.QUEUED));
        transitions.put(TaskStatus.SKIPPED, EnumSet.noneOf(TaskStatus.class));
        return transitions;
    }
}

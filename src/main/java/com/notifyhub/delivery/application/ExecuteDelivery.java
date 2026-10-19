package com.notifyhub.delivery.application;

import com.notifyhub.delivery.channel.domain.ChannelAdapter;
import com.notifyhub.delivery.channel.domain.RenderedRequest;
import com.notifyhub.delivery.channel.domain.SendOutcome;
import com.notifyhub.delivery.dispatching.domain.DeliveryTask;
import com.notifyhub.delivery.record.domain.Attempt;
import com.notifyhub.delivery.record.domain.AttemptLog;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/** 认领一条任务，调用替身或真实适配器，并记下这一次尝试。 */
public final class ExecuteDelivery {

    private final ChannelAdapter adapter;
    private final AttemptLog attemptLog;
    private final Duration lease;
    private final List<Duration> retryIntervals;

    public ExecuteDelivery(ChannelAdapter adapter, AttemptLog attemptLog, Duration lease, List<Duration> retryIntervals) {
        this.adapter = adapter;
        this.attemptLog = attemptLog;
        this.lease = lease;
        this.retryIntervals = List.copyOf(retryIntervals);
    }

    public boolean execute(DeliveryTask task, Instant now) {
        if (!task.claim(now, lease)) {
            return false;
        }
        String idempotencyKey = task.idempotencyKey().value();
        SendOutcome outcome = adapter.send(new RenderedRequest(task.destinationId(), "", idempotencyKey));
        attemptLog.append(new Attempt(idempotencyKey, outcome, now));
        switch (outcome) {
            case SUCCESS -> task.markSucceeded();
            case RETRYABLE -> task.recordRetryableFailure(retryIntervals, now);
            case TERMINAL -> task.recordTerminalFailure();
        }
        return true;
    }
}

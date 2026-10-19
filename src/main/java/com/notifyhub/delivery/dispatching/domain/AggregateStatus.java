package com.notifyhub.delivery.dispatching.domain;

import java.util.List;

/** 由子任务推导出的消息总状态。 */
public enum AggregateStatus {
    ALL_SUCCEEDED,
    PARTIAL,
    IN_PROGRESS,
    ALL_DEAD;

    public static AggregateStatus from(List<DeliveryTask> tasks) {
        boolean inProgress = false;
        boolean anySucceeded = false;
        boolean anyDead = false;
        for (DeliveryTask task : tasks) {
            switch (task.status()) {
                case QUEUED, SENDING, RETRY_WAIT -> inProgress = true;
                case SUCCEEDED -> anySucceeded = true;
                case DEAD -> anyDead = true;
                case SKIPPED -> {
                }
            }
        }
        if (inProgress) {
            return IN_PROGRESS;
        }
        if (anySucceeded && anyDead) {
            return PARTIAL;
        }
        if (anySucceeded) {
            return ALL_SUCCEEDED;
        }
        if (anyDead) {
            return ALL_DEAD;
        }
        return PARTIAL;
    }
}

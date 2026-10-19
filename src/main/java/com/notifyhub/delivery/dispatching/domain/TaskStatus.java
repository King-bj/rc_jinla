package com.notifyhub.delivery.dispatching.domain;

/** 单条投递任务的状态。 */
public enum TaskStatus {
    QUEUED,
    SENDING,
    RETRY_WAIT,
    SUCCEEDED,
    DEAD,
    SKIPPED
}

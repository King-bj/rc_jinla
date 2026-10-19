package com.notifyhub.delivery.dispatching.infrastructure;

/** 同一租户、事件、目的地的任务已经存在。 */
public final class DuplicateTaskException extends RuntimeException {

    public DuplicateTaskException(Throwable cause) {
        super("delivery task already exists", cause);
    }
}

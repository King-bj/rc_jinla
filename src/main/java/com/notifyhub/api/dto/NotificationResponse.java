package com.notifyhub.api.dto;

/** 通知提交响应，HTTP 202 返回。message 固定为 ACCEPTED 表示已受理。 */
public class NotificationResponse {

    private final Long taskId;
    private final String requestId;
    /** 当前任务状态，新提交时为 PENDING */
    private final String status;
    private final String message;

    public NotificationResponse(Long taskId, String requestId, String status, String message) {
        this.taskId = taskId;
        this.requestId = requestId;
        this.status = status;
        this.message = message;
    }

    public Long getTaskId() {
        return taskId;
    }

    public String getRequestId() {
        return requestId;
    }

    public String getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }
}

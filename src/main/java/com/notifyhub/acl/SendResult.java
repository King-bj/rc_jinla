package com.notifyhub.acl;

/** 渠道发送结果，供调度器判断成功或进入重试/死信流程。 */
public class SendResult {

    private final boolean success;
    private final int statusCode;
    private final String message;

    public SendResult(boolean success, int statusCode, String message) {
        this.success = success;
        this.statusCode = statusCode;
        this.message = message;
    }

    public static SendResult ok(int statusCode) {
        return new SendResult(true, statusCode, null);
    }

    public static SendResult fail(int statusCode, String message) {
        return new SendResult(false, statusCode, message);
    }

    public boolean isSuccess() {
        return success;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getMessage() {
        return message;
    }
}

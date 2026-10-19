package com.notifyhub.delivery.channel.domain;

/** 适配器对一次发送的判定，调度器据此决定成功、重试或死信。 */
public enum SendOutcome {
    SUCCESS,
    RETRYABLE,
    TERMINAL
}

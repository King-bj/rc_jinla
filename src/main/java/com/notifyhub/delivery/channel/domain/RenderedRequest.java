package com.notifyhub.delivery.channel.domain;

/** 渲染完成后交给通道适配器的报文。target 是 topic 或队列名。 */
public final class RenderedRequest {

    private final String target;
    private final String body;
    private final String idempotencyKey;

    public RenderedRequest(String target, String body, String idempotencyKey) {
        this.target = target;
        this.body = body;
        this.idempotencyKey = idempotencyKey;
    }

    public String target() {
        return target;
    }

    public String body() {
        return body;
    }

    public String idempotencyKey() {
        return idempotencyKey;
    }
}

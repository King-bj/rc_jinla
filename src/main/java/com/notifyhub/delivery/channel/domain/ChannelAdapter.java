package com.notifyhub.delivery.channel.domain;

/** 送出通道端口。适配器只负责传输，不决定重试次数。 */
public interface ChannelAdapter {

    AdapterType type();

    SendOutcome send(RenderedRequest request);
}

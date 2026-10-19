package com.notifyhub.delivery.channel.infrastructure.rocketmq;

import com.notifyhub.delivery.channel.domain.AdapterType;
import com.notifyhub.delivery.channel.domain.ChannelAdapter;
import com.notifyhub.delivery.channel.domain.RenderedRequest;
import com.notifyhub.delivery.channel.domain.SendOutcome;

/** RocketMQ 送出适配器。结果映射与 Kafka 适配器相同。 */
public final class RocketMqChannelAdapter implements ChannelAdapter {

    private final RocketMqProducerPort producer;

    public RocketMqChannelAdapter(RocketMqProducerPort producer) {
        this.producer = producer;
    }

    @Override
    public AdapterType type() {
        return AdapterType.ROCKETMQ;
    }

    @Override
    public SendOutcome send(RenderedRequest request) {
        try {
            producer.send(request.target(), request.idempotencyKey(), request.body());
            return SendOutcome.SUCCESS;
        } catch (RuntimeException ex) {
            return SendOutcome.RETRYABLE;
        }
    }
}

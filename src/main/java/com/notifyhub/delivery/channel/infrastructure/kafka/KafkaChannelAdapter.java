package com.notifyhub.delivery.channel.infrastructure.kafka;

import com.notifyhub.delivery.channel.domain.AdapterType;
import com.notifyhub.delivery.channel.domain.ChannelAdapter;
import com.notifyhub.delivery.channel.domain.RenderedRequest;
import com.notifyhub.delivery.channel.domain.SendOutcome;

/** Kafka 送出适配器。消息键使用幂等键，便于下游去重。 */
public final class KafkaChannelAdapter implements ChannelAdapter {

    private final KafkaProducerPort producer;

    public KafkaChannelAdapter(KafkaProducerPort producer) {
        this.producer = producer;
    }

    @Override
    public AdapterType type() {
        return AdapterType.KAFKA;
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

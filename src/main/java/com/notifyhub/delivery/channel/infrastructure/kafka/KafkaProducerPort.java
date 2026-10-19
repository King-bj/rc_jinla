package com.notifyhub.delivery.channel.infrastructure.kafka;

/** Kafka 发送端口。确认返回即成功，抛出异常由适配器映射为可重试。 */
public interface KafkaProducerPort {

    void send(String topic, String key, String body);
}

package com.notifyhub.delivery.channel.infrastructure.kafka;

import com.notifyhub.delivery.channel.domain.AdapterType;
import com.notifyhub.delivery.channel.domain.RenderedRequest;
import com.notifyhub.delivery.channel.domain.SendOutcome;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class KafkaChannelAdapterTest {

    private static final RenderedRequest REQUEST =
            new RenderedRequest("billing.events", "{\"id\":1}", "tenant:event:dest");

    @Test
    void confirmedSendIsSuccessAndUsesIdempotencyKey() {
        RecordingProducer producer = new RecordingProducer();
        KafkaChannelAdapter adapter = new KafkaChannelAdapter(producer);

        SendOutcome outcome = adapter.send(REQUEST);

        assertEquals(AdapterType.KAFKA, adapter.type());
        assertEquals(SendOutcome.SUCCESS, outcome);
        assertEquals("billing.events", producer.topic);
        assertEquals("tenant:event:dest", producer.key);
        assertEquals("{\"id\":1}", producer.body);
    }

    @Test
    void sendExceptionIsRetryable() {
        KafkaChannelAdapter adapter = new KafkaChannelAdapter((topic, key, body) -> {
            throw new RuntimeException("broker unavailable");
        });

        assertEquals(SendOutcome.RETRYABLE, adapter.send(REQUEST));
    }

    private static final class RecordingProducer implements KafkaProducerPort {
        private String topic;
        private String key;
        private String body;

        @Override
        public void send(String topic, String key, String body) {
            this.topic = topic;
            this.key = key;
            this.body = body;
        }
    }
}

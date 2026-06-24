package com.notifyhub.support;

import com.notifyhub.config.NotifyHubProperties;
import com.notifyhub.domain.NotificationMessage;
import com.notifyhub.domain.TargetSystem;
import com.notifyhub.domain.TaskStatus;
import com.notifyhub.entity.NotificationTaskEntity;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class TestData {

    private TestData() {
    }

    public static NotifyHubProperties properties() {
        NotifyHubProperties properties = new NotifyHubProperties();
        properties.setRetryIntervalsMinutes(List.of(1, 5, 30));

        NotifyHubProperties.ChannelConfig crm = new NotifyHubProperties.ChannelConfig();
        crm.setBaseUrl("http://crm.test");
        NotifyHubProperties.ChannelConfig ad = new NotifyHubProperties.ChannelConfig();
        ad.setBaseUrl("http://ad.test");
        NotifyHubProperties.ChannelConfig inventory = new NotifyHubProperties.ChannelConfig();
        inventory.setBaseUrl("http://inventory.test");

        Map<String, NotifyHubProperties.ChannelConfig> channels = new HashMap<>();
        channels.put("crm", crm);
        channels.put("ad", ad);
        channels.put("inventory", inventory);
        properties.setChannels(channels);
        return properties;
    }

    public static NotificationMessage message(TargetSystem targetSystem,
                                                String eventType,
                                                Map<String, Object> payload,
                                                Map<String, String> headers) {
        NotificationMessage message = new NotificationMessage();
        message.setRequestId("req-test");
        message.setTargetSystem(targetSystem);
        message.setEventType(eventType);
        message.setPriority(1);
        message.setPayload(payload);
        message.setHeaders(headers);
        return message;
    }

    public static NotificationTaskEntity task(TargetSystem targetSystem,
                                              String eventType,
                                              String payloadJson,
                                              int retryCount) {
        LocalDateTime now = LocalDateTime.of(2026, 6, 24, 12, 0);
        NotificationTaskEntity task = new NotificationTaskEntity();
        task.setId(1L);
        task.setRequestId("req-" + targetSystem.name().toLowerCase());
        task.setTargetSystem(targetSystem);
        task.setEventType(eventType);
        task.setPriority(1);
        task.setPayload(payloadJson);
        task.setStatus(TaskStatus.PENDING);
        task.setRetryCount(retryCount);
        task.setNextRetryTime(now);
        task.setCreatedAt(now);
        task.setUpdatedAt(now);
        return task;
    }
}

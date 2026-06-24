package com.notifyhub.dispatcher;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.notifyhub.acl.ChannelFactory;
import com.notifyhub.acl.ConverterFactory;
import com.notifyhub.acl.NotificationChannel;
import com.notifyhub.acl.NotificationConverter;
import com.notifyhub.acl.SendResult;
import com.notifyhub.acl.VendorHttpRequest;
import com.notifyhub.config.NotifyHubProperties;
import com.notifyhub.domain.NotificationMessage;
import com.notifyhub.domain.TargetSystem;
import com.notifyhub.domain.TaskStatus;
import com.notifyhub.entity.NotificationTaskEntity;
import com.notifyhub.repository.NotificationTaskRepository;
import com.notifyhub.support.TestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 投递调度核心场景测试：Mock Converter/Channel，对齐设计文档时序图。
 * 完整实现提交前 processTask 相关用例预期失败（TDD 红灯）。
 */
@ExtendWith(MockitoExtension.class)
class NotificationDispatcherTest {

    @Mock
    private NotificationTaskRepository repository;

    @Mock
    private ConverterFactory converterFactory;

    @Mock
    private ChannelFactory channelFactory;

    @Mock
    private NotificationConverter converter;

    @Mock
    private NotificationChannel channel;

    private NotificationDispatcher dispatcher;
    private NotifyHubProperties properties;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        properties = TestData.properties();
        when(channelFactory.getChannel(any())).thenReturn(channel);

        dispatcher = new NotificationDispatcher(
                repository,
                converterFactory,
                channelFactory,
                properties,
                objectMapper
        );
    }

    @Test
    void dispatch_pollsPendingTasks() {
        when(repository.findReadyTasks(eq(TaskStatus.PENDING), any(LocalDateTime.class), any(Pageable.class)))
                .thenReturn(List.of());

        dispatcher.dispatch();

        verify(repository).findReadyTasks(eq(TaskStatus.PENDING), any(LocalDateTime.class), any(Pageable.class));
    }

    @Test
    void processTask_sendSuccess_marksSuccess() {
        NotificationTaskEntity task = taskFor(TargetSystem.CRM, 0);
        stubConvertAndSend(SendResult.ok(200));

        dispatcher.processTask(task);

        assertEquals(TaskStatus.SUCCESS, task.getStatus());
        assertNull(task.getLastError());
        verify(repository).save(task);
    }

    @Test
    void processTask_sendFail_schedulesRetry() {
        NotificationTaskEntity task = taskFor(TargetSystem.CRM, 0);
        stubConvertAndSend(SendResult.fail(503, "CRM unavailable"));

        dispatcher.processTask(task);

        assertEquals(TaskStatus.PENDING, task.getStatus());
        assertEquals(1, task.getRetryCount());
        assertEquals("CRM unavailable", task.getLastError());
        assertNotNull(task.getNextRetryTime());
        verify(repository).save(task);
    }

    @Test
    void processTask_exhaustedRetries_marksDead() {
        NotificationTaskEntity task = taskFor(TargetSystem.INVENTORY, 3);
        stubConvertAndSend(SendResult.fail(500, "inventory down"));

        dispatcher.processTask(task);

        assertEquals(TaskStatus.DEAD, task.getStatus());
        assertEquals(4, task.getRetryCount());
        assertNull(task.getNextRetryTime());
        verify(repository).save(task);
    }

    @Test
    void processTask_invokesChannelSend() {
        NotificationTaskEntity task = taskFor(TargetSystem.AD, 0);
        VendorHttpRequest vendorRequest = new VendorHttpRequest(
                "http://ad.test/conversions",
                "POST",
                Map.of("Content-Type", "application/json"),
                "{\"conversion_event\":\"EVENT_AD\"}"
        );
        when(converterFactory.getConverter(TargetSystem.AD)).thenReturn(converter);
        when(converter.convert(any(NotificationMessage.class))).thenReturn(vendorRequest);
        when(channel.send(vendorRequest)).thenReturn(SendResult.ok(200));
        ArgumentCaptor<VendorHttpRequest> captor = ArgumentCaptor.forClass(VendorHttpRequest.class);

        dispatcher.processTask(task);

        verify(channel).send(captor.capture());
        assertEquals("http://ad.test/conversions", captor.getValue().getUrl());
    }

    @Test
    void processTask_notImplementedYet_throwsUnsupportedOperation() {
        NotificationTaskEntity task = taskFor(TargetSystem.CRM, 0);
        NotificationDispatcher skeleton = new NotificationDispatcher(
                repository,
                new ConverterFactory(),
                new ChannelFactory(),
                properties,
                objectMapper
        );

        assertThrows(UnsupportedOperationException.class, () -> skeleton.processTask(task));
    }

    private void stubConvertAndSend(SendResult sendResult) {
        VendorHttpRequest vendorRequest = new VendorHttpRequest(
                "http://crm.test/contacts/update",
                "POST",
                Map.of("Content-Type", "application/json"),
                "{}"
        );
        when(converterFactory.getConverter(any())).thenReturn(converter);
        when(converter.convert(any(NotificationMessage.class))).thenReturn(vendorRequest);
        when(channel.send(any())).thenReturn(sendResult);
    }

    private NotificationTaskEntity taskFor(TargetSystem targetSystem, int retryCount) {
        Map<String, Object> payload = Map.of("userId", "u1");
        try {
            String payloadJson = objectMapper.writeValueAsString(payload);
            return TestData.task(targetSystem, "EVENT_" + targetSystem.name(), payloadJson, retryCount);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}

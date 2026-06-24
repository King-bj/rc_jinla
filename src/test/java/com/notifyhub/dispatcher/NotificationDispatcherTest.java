package com.notifyhub.dispatcher;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 投递调度器单元测试。
 * <p>
 * Mock Repository / ConverterFactory / Channel，隔离外部 HTTP 依赖，
 * 验证设计文档时序图中的轮询、发送、重试与死信分支。
 */
@ExtendWith(MockitoExtension.class)
class NotificationDispatcherTest {

    @Mock
    private NotificationTaskRepository repository;

    @Mock
    private ConverterFactory converterFactory;

    @Mock
    private NotificationChannel channel;

    @Mock
    private NotificationConverter converter;

    private NotificationDispatcher dispatcher;
    private NotifyHubProperties properties;
    private ObjectMapper objectMapper;
    private ObjectProvider<NotificationDispatcher> selfProvider;
    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        properties = TestData.properties();

        dispatcher = new NotificationDispatcher(
                repository,
                converterFactory,
                channel,
                properties,
                objectMapper,
                selfProvider
        );
    }

    /** 时序图 loop：调度器定期拉取 PENDING/FAILED 且到达 nextRetryTime 的任务 */
    @Test
    void dispatch_pollsPendingTasks() {
        when(repository.findReadyTasks(anyList(), any(LocalDateTime.class), any(Pageable.class)))
                .thenReturn(List.of());

        dispatcher.dispatch();

        verify(repository).findReadyTasks(
                eq(List.of(TaskStatus.PENDING, TaskStatus.FAILED)),
                any(LocalDateTime.class),
                any(Pageable.class));
    }

    /** 时序图 alt Success：channel 返回 2xx 后任务标记 SUCCESS */
    @Test
    void processTask_sendSuccess_marksSuccess() {
        NotificationTaskEntity task = taskFor(TargetSystem.CRM, 0);
        stubConvertAndSend(SendResult.ok(200));

        dispatcher.processTask(task);

        assertEquals(TaskStatus.SUCCESS, task.getStatus());
        assertNull(task.getLastError());
        verify(repository).save(task);
    }

    /** 时序图 alt Failed：首次失败递增 retryCount，按退避间隔设置 nextRetryTime */
    @Test
    void processTask_sendFail_schedulesRetry() {
        NotificationTaskEntity task = taskFor(TargetSystem.CRM, 0);
        stubConvertAndSend(SendResult.fail(503, "CRM unavailable"));

        dispatcher.processTask(task);

        assertEquals(TaskStatus.FAILED, task.getStatus());
        assertEquals(1, task.getRetryCount());
        assertEquals("CRM unavailable", task.getLastError());
        assertNotNull(task.getNextRetryTime());
        verify(repository).save(task);
    }

    /** 毒消息：无效 payload JSON 计入重试，状态持久化为 FAILED */
    @Test
    void dispatch_invalidPayloadJson_schedulesRetry() {
        NotificationTaskEntity task = TestData.task(TargetSystem.CRM, "EVENT", "not-valid-json", 0);
        when(repository.findReadyTasks(anyList(), any(LocalDateTime.class), any(Pageable.class)))
                .thenReturn(List.of(task));

        dispatcher.dispatch();

        assertEquals(TaskStatus.FAILED, task.getStatus());
        assertEquals(1, task.getRetryCount());
        assertNotNull(task.getLastError());
        assertNotNull(task.getNextRetryTime());
        verify(repository).save(task);
    }

    /** 毒消息：重试耗尽后进入 DEAD */
    @Test
    void dispatch_invalidPayloadJson_marksDeadAfterExhaustedRetries() {
        NotificationTaskEntity task = TestData.task(TargetSystem.CRM, "EVENT", "not-valid-json", 3);
        when(repository.findReadyTasks(anyList(), any(LocalDateTime.class), any(Pageable.class)))
                .thenReturn(List.of(task));

        dispatcher.dispatch();

        assertEquals(TaskStatus.DEAD, task.getStatus());
        assertEquals(4, task.getRetryCount());
        assertNull(task.getNextRetryTime());
        verify(repository).save(task);
    }

    /** 时序图 retry exhausted：超过 retry-intervals 配置次数后进入 DEAD */
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

    /** 时序图 DS→CH→EXT：转换后的 VendorHttpRequest 被传入 channel.send */
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

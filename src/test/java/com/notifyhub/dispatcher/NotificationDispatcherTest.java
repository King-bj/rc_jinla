package com.notifyhub.dispatcher;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.notifyhub.acl.NotificationChannel;
import com.notifyhub.acl.SendResult;
import com.notifyhub.acl.VendorHttpRequest;
import com.notifyhub.acl.VendorTemplateRenderer;
import com.notifyhub.acl.VendorTemplates;
import com.notifyhub.config.NotifyHubProperties;
import com.notifyhub.domain.TaskStatus;
import com.notifyhub.entity.NotificationTaskEntity;
import com.notifyhub.entity.NotificationVendorEntity;
import com.notifyhub.repository.NotificationTaskRepository;
import com.notifyhub.repository.NotificationVendorRepository;
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
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 投递调度器单元测试。
 * <p>
 * 用真实模板渲染和假的供应商配置，验证轮询、发送、重试与死信。
 */
@ExtendWith(MockitoExtension.class)
class NotificationDispatcherTest {

    @Mock
    private NotificationTaskRepository repository;

    @Mock
    private NotificationVendorRepository vendorRepository;

    @Mock
    private NotificationChannel channel;

    @Mock
    private ObjectProvider<NotificationDispatcher> selfProvider;

    private NotificationDispatcher dispatcher;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        NotifyHubProperties properties = TestData.properties();
        dispatcher = new NotificationDispatcher(
                repository,
                vendorRepository,
                new VendorTemplateRenderer(objectMapper),
                channel,
                properties,
                objectMapper,
                selfProvider
        );
        lenient().when(selfProvider.getObject()).thenReturn(dispatcher);
    }

    /** 时序图 loop：调度器定期拉取 PENDING/FAILED 且到达 nextRetryTime 的任务 */
    @Test
    void dispatch_pollsPendingTasks() {
        when(repository.findReadyTasks(anyList(), any(LocalDateTime.class), any(Pageable.class)))
                .thenReturn(java.util.List.of());

        dispatcher.dispatch();

        verify(repository).findReadyTasks(
                eq(java.util.List.of(TaskStatus.PENDING, TaskStatus.FAILED)),
                any(LocalDateTime.class),
                any(Pageable.class));
    }

    /** 时序图 alt Success：channel 返回 2xx 后任务标记 SUCCESS */
    @Test
    void processTask_sendSuccess_marksSuccess() {
        NotificationTaskEntity task = taskFor("CRM", 0);
        stubVendorAndSend("CRM", VendorTemplates.crm("http://crm.test"), SendResult.ok(200));

        dispatcher.processTask(task);

        assertEquals(TaskStatus.SUCCESS, task.getStatus());
        assertNull(task.getLastError());
        verify(repository).save(task);
    }

    /** 时序图 alt Failed：首次失败递增 retryCount，按退避间隔设置 nextRetryTime */
    @Test
    void processTask_sendFail_schedulesRetry() {
        NotificationTaskEntity task = taskFor("CRM", 0);
        stubVendorAndSend("CRM", VendorTemplates.crm("http://crm.test"), SendResult.fail(503, "CRM unavailable"));

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
        NotificationTaskEntity task = TestData.task("CRM", "EVENT", "not-valid-json", 0);
        when(repository.findReadyTasks(anyList(), any(LocalDateTime.class), any(Pageable.class)))
                .thenReturn(java.util.List.of(task));

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
        NotificationTaskEntity task = TestData.task("CRM", "EVENT", "not-valid-json", 3);
        when(repository.findReadyTasks(anyList(), any(LocalDateTime.class), any(Pageable.class)))
                .thenReturn(java.util.List.of(task));

        dispatcher.dispatch();

        assertEquals(TaskStatus.DEAD, task.getStatus());
        assertEquals(4, task.getRetryCount());
        assertNull(task.getNextRetryTime());
        verify(repository).save(task);
    }

    /** 时序图 retry exhausted：超过 retry-intervals 配置次数后进入 DEAD */
    @Test
    void processTask_exhaustedRetries_marksDead() {
        NotificationTaskEntity task = taskFor("INVENTORY", 3);
        stubVendorAndSend("INVENTORY", VendorTemplates.inventory("http://inventory.test"),
                SendResult.fail(500, "inventory down"));

        dispatcher.processTask(task);

        assertEquals(TaskStatus.DEAD, task.getStatus());
        assertEquals(4, task.getRetryCount());
        assertNull(task.getNextRetryTime());
        verify(repository).save(task);
    }

    /** 新供应商只存在于配置里，下一次投递使用该 URL 和模板。 */
    @Test
    void processTask_newVendorConfig_sendsRenderedRequest() throws Exception {
        NotificationVendorEntity vendor = VendorTemplates.vendor(
                "BILLING",
                "账单",
                "http://billing.test/hooks/{{payload.userId}}",
                "{\"event\":\"{{eventType}}\",\"user\":{{payload.userId}}}");
        NotificationTaskEntity task = TestData.task("BILLING", "INVOICE", "{\"userId\":\"42\"}", 0);
        when(vendorRepository.findById("BILLING")).thenReturn(Optional.of(vendor));
        when(channel.send(any())).thenReturn(SendResult.ok(200));
        ArgumentCaptor<VendorHttpRequest> captor = ArgumentCaptor.forClass(VendorHttpRequest.class);

        dispatcher.processTask(task);

        verify(channel).send(captor.capture());
        assertEquals("http://billing.test/hooks/42", captor.getValue().getUrl());
        assertEquals("INVOICE", objectMapper.readTree(captor.getValue().getBody()).get("event").asText());
        assertEquals("42", objectMapper.readTree(captor.getValue().getBody()).get("user").asText());
    }

    /** 模板缺字段时按投递失败进入重试，而不是中断调度。 */
    @Test
    void dispatch_missingTemplateVariable_schedulesRetry() {
        NotificationVendorEntity vendor = VendorTemplates.vendor(
                "BILLING", "账单", "https://billing.test/hook", "{\"name\":{{payload.name}}}");
        NotificationTaskEntity task = TestData.task("BILLING", "EVENT", "{\"userId\":\"u1\"}", 0);
        when(vendorRepository.findById("BILLING")).thenReturn(Optional.of(vendor));
        when(repository.findReadyTasks(anyList(), any(LocalDateTime.class), any(Pageable.class)))
                .thenReturn(java.util.List.of(task));

        dispatcher.dispatch();

        assertEquals(TaskStatus.FAILED, task.getStatus());
        assertTrue(task.getLastError().contains("missing payload.name"));
        verify(repository).save(task);
    }

    private void stubVendorAndSend(String code, NotificationVendorEntity vendor, SendResult sendResult) {
        when(vendorRepository.findById(code)).thenReturn(Optional.of(vendor));
        when(channel.send(any())).thenReturn(sendResult);
    }

    private NotificationTaskEntity taskFor(String targetSystem, int retryCount) {
        try {
            String payloadJson = objectMapper.writeValueAsString(Map.of("userId", "u1"));
            return TestData.task(targetSystem, "EVENT_" + targetSystem, payloadJson, retryCount);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}

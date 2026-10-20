package com.notifyhub.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.notifyhub.api.dto.NotificationRequest;
import com.notifyhub.api.dto.NotificationResponse;
import com.notifyhub.domain.TaskStatus;
import com.notifyhub.entity.NotificationTaskEntity;
import com.notifyhub.entity.NotificationVendorEntity;
import com.notifyhub.repository.NotificationTaskRepository;
import com.notifyhub.repository.NotificationVendorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 通知接入服务单元测试。
 * <p>
 * Mock Repository，验证落库、requestId 幂等与 headers 序列化，不依赖真实数据库。
 */
@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationTaskRepository repository;

    @Mock
    private NotificationVendorRepository vendorRepository;

    private NotificationService service;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        service = new NotificationService(repository, vendorRepository, objectMapper);
    }

    /** 时序图 BS→API→DB：新请求写入 PENDING 任务并返回 ACCEPTED */
    @ParameterizedTest
    @ValueSource(strings = {"CRM", "AD", "INVENTORY"})
    void notify_newRequest_persistsPendingTask(String targetSystem) {
        NotificationRequest request = requestFor(targetSystem, "req-new-" + targetSystem);
        stubEnabled(targetSystem);
        when(repository.findByRequestId(request.getRequestId())).thenReturn(Optional.empty());
        when(repository.save(any())).thenAnswer(invocation -> {
            NotificationTaskEntity entity = invocation.getArgument(0);
            entity.setId(10L);
            return entity;
        });

        NotificationResponse response = service.notify(request);

        assertEquals(10L, response.getTaskId());
        assertEquals(request.getRequestId(), response.getRequestId());
        assertEquals(TaskStatus.PENDING.name(), response.getStatus());
        assertEquals("ACCEPTED", response.getMessage());

        ArgumentCaptor<NotificationTaskEntity> captor = ArgumentCaptor.forClass(NotificationTaskEntity.class);
        verify(repository).save(captor.capture());
        NotificationTaskEntity saved = captor.getValue();
        assertEquals(targetSystem, saved.getTargetSystem());
        assertEquals(TaskStatus.PENDING, saved.getStatus());
        assertEquals(0, saved.getRetryCount());
    }

    @Test
    void notify_unknownVendor_rejectsWithoutSave() {
        NotificationRequest request = requestFor("BILLING", "req-unknown");
        when(repository.findByRequestId("req-unknown")).thenReturn(Optional.empty());
        when(vendorRepository.findById("BILLING")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> service.notify(request));
        verify(repository, never()).save(any());
    }

    @Test
    void notify_disabledVendor_rejectsWithoutSave() {
        NotificationRequest request = requestFor("CRM", "req-off");
        NotificationVendorEntity vendor = enabled("CRM");
        vendor.setEnabled(false);
        when(repository.findByRequestId("req-off")).thenReturn(Optional.empty());
        when(vendorRepository.findById("CRM")).thenReturn(Optional.of(vendor));

        assertThrows(IllegalArgumentException.class, () -> service.notify(request));
        verify(repository, never()).save(any());
    }

    /** 幂等：相同 requestId 重复提交直接返回已有任务，不触发 save */
    @Test
    void notify_duplicateRequestId_returnsExistingWithoutSave() {
        NotificationRequest request = requestFor("CRM", "req-dup");
        NotificationTaskEntity existing = new NotificationTaskEntity();
        existing.setId(99L);
        existing.setRequestId("req-dup");
        existing.setStatus(TaskStatus.SUCCESS);
        when(repository.findByRequestId("req-dup")).thenReturn(Optional.of(existing));

        NotificationResponse response = service.notify(request);

        assertEquals(99L, response.getTaskId());
        assertEquals(TaskStatus.SUCCESS.name(), response.getStatus());
        verify(repository, never()).save(any());
    }

    /** 幂等竞态：唯一索引冲突时回读已有任务并返回 */
    @Test
    void notify_concurrentDuplicateRequestId_returnsExisting() {
        NotificationRequest request = requestFor("CRM", "req-race");
        stubEnabled("CRM");
        NotificationTaskEntity existing = new NotificationTaskEntity();
        existing.setId(42L);
        existing.setRequestId("req-race");
        existing.setStatus(TaskStatus.PENDING);
        when(repository.findByRequestId("req-race"))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(existing));
        when(repository.save(any())).thenThrow(new DataIntegrityViolationException("duplicate request_id"));

        NotificationResponse response = service.notify(request);

        assertEquals(42L, response.getTaskId());
        assertEquals("req-race", response.getRequestId());
        assertEquals(TaskStatus.PENDING.name(), response.getStatus());
    }

    /** 可选 headers 以 JSON 字符串持久化到任务表 */
    @Test
    void notify_withHeaders_persistsHeadersJson() {
        NotificationRequest request = requestFor("AD", "req-headers");
        stubEnabled("AD");
        request.setHeaders(Map.of("X-Trace-Id", "trace-1"));
        when(repository.findByRequestId(request.getRequestId())).thenReturn(Optional.empty());
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.notify(request);

        ArgumentCaptor<NotificationTaskEntity> captor = ArgumentCaptor.forClass(NotificationTaskEntity.class);
        verify(repository).save(captor.capture());
        assertEquals("{\"X-Trace-Id\":\"trace-1\"}", captor.getValue().getHeaders());
    }

    @Test
    void replay_deadTask_returnsToPending() {
        NotificationTaskEntity task = new NotificationTaskEntity();
        task.setId(7L);
        task.setStatus(TaskStatus.DEAD);
        task.setRetryCount(5);
        task.setLastError("down");
        when(repository.findById(7L)).thenReturn(Optional.of(task));

        service.replay(7L);

        assertEquals(TaskStatus.PENDING, task.getStatus());
        assertEquals(0, task.getRetryCount());
        assertNull(task.getLastError());
        assertNotNull(task.getNextRetryTime());
        verify(repository).save(task);
    }

    @Test
    void replay_nonDead_rejected() {
        NotificationTaskEntity task = new NotificationTaskEntity();
        task.setId(8L);
        task.setStatus(TaskStatus.FAILED);
        when(repository.findById(8L)).thenReturn(Optional.of(task));

        assertThrows(IllegalStateException.class, () -> service.replay(8L));
        verify(repository, never()).save(any());
    }

    private void stubEnabled(String code) {
        when(vendorRepository.findById(code)).thenReturn(Optional.of(enabled(code)));
    }

    private NotificationVendorEntity enabled(String code) {
        NotificationVendorEntity vendor = new NotificationVendorEntity();
        vendor.setCode(code);
        vendor.setEnabled(true);
        return vendor;
    }

    private NotificationRequest requestFor(String targetSystem, String requestId) {
        NotificationRequest request = new NotificationRequest();
        request.setRequestId(requestId);
        request.setTargetSystem(targetSystem);
        request.setEventType("TEST_EVENT");
        request.setPriority(5);
        request.setPayload(Map.of("userId", "1001"));
        return request;
    }
}

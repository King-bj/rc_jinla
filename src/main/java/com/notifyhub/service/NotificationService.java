package com.notifyhub.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.notifyhub.api.dto.NotificationRequest;
import com.notifyhub.api.dto.NotificationResponse;
import com.notifyhub.domain.TaskStatus;
import com.notifyhub.entity.NotificationTaskEntity;
import com.notifyhub.repository.NotificationTaskRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 通知接入服务：负责消息落库与幂等控制。
 * 采用 At Least Once 语义，接入后立即返回，实际投递由调度器异步完成。
 */
@Service
public class NotificationService {

    private final NotificationTaskRepository repository;
    private final ObjectMapper objectMapper;

    public NotificationService(NotificationTaskRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    /**
     * 提交通知请求。
     * 相同 requestId 重复提交时直接返回已有任务，不重复落库。
     */
    @Transactional
    public NotificationResponse notify(NotificationRequest request) {
        return repository.findByRequestId(request.getRequestId())
                .map(this::toResponse)
                .orElseGet(() -> createTask(request));
    }

    /** 首次提交：写入本地消息表，状态 PENDING，等待调度器拾取 */
    private NotificationResponse createTask(NotificationRequest request) {
        LocalDateTime now = LocalDateTime.now();
        NotificationTaskEntity task = new NotificationTaskEntity();
        task.setRequestId(request.getRequestId());
        task.setTargetSystem(request.getTargetSystem());
        task.setEventType(request.getEventType());
        task.setPriority(request.getPriority());
        task.setPayload(toJson(request.getPayload()));
        task.setHeaders(request.getHeaders() == null ? null : toJson(request.getHeaders()));
        task.setStatus(TaskStatus.PENDING);
        task.setRetryCount(0);
        task.setNextRetryTime(now);
        task.setCreatedAt(now);
        task.setUpdatedAt(now);

        NotificationTaskEntity saved = repository.save(task);
        return toResponse(saved);
    }

    private NotificationResponse toResponse(NotificationTaskEntity task) {
        return new NotificationResponse(
                task.getId(),
                task.getRequestId(),
                task.getStatus().name(),
                "ACCEPTED"
        );
    }

    private String toJson(Map<?, ?> map) {
        try {
            return objectMapper.writeValueAsString(map);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Invalid JSON field", e);
        }
    }
}

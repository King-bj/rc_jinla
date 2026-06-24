package com.notifyhub.dispatcher;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.notifyhub.acl.ConverterFactory;
import com.notifyhub.acl.NotificationChannel;
import com.notifyhub.acl.NotificationConverter;
import com.notifyhub.acl.SendResult;
import com.notifyhub.acl.VendorHttpRequest;
import com.notifyhub.config.NotifyHubProperties;
import com.notifyhub.domain.NotificationMessage;
import com.notifyhub.domain.TaskStatus;
import com.notifyhub.entity.NotificationTaskEntity;
import com.notifyhub.repository.NotificationTaskRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 通知投递调度器：轮询本地消息表，执行转换、发送、重试与死信处理。
 */
@Component
public class NotificationDispatcher {

    private static final Logger log = LoggerFactory.getLogger(NotificationDispatcher.class);
    private static final List<TaskStatus> READY_STATUSES = List.of(TaskStatus.PENDING, TaskStatus.FAILED);

    private final NotificationTaskRepository repository;
    private final ConverterFactory converterFactory;
    private final NotificationChannel channel;
    private final NotifyHubProperties properties;
    private final ObjectMapper objectMapper;
    private final ObjectProvider<NotificationDispatcher> selfProvider;

    public NotificationDispatcher(NotificationTaskRepository repository,
                                  ConverterFactory converterFactory,
                                  NotificationChannel channel,
                                  NotifyHubProperties properties,
                                  ObjectMapper objectMapper,
                                  ObjectProvider<NotificationDispatcher> selfProvider) {
        this.repository = repository;
        this.converterFactory = converterFactory;
        this.channel = channel;
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.selfProvider = selfProvider;
    }

    /** 定时拉取一批待投递任务并逐条处理 */
    @Scheduled(fixedDelayString = "${notify-hub.dispatcher.interval-ms:10000}")
    public void dispatch() {
        LocalDateTime now = LocalDateTime.now();
        int batchSize = properties.getDispatcher().getBatchSize();
        List<NotificationTaskEntity> tasks = repository.findReadyTasks(
                READY_STATUSES,
                now,
                PageRequest.of(0, batchSize)
        );

        for (NotificationTaskEntity task : tasks) {
            try {
                selfProvider.getObject().processTask(task);
            } catch (Exception e) {
                log.error("Unexpected error dispatching task id={} requestId={}",
                        task.getId(), task.getRequestId(), e);
                handleFailure(task, SendResult.fail(-1, e.getMessage()), LocalDateTime.now());
            }
        }
    }

    /**
     * 处理单条任务：领域消息 → 防腐层转换 → HTTP 发送 → 更新状态。
     */
    @Transactional
    public void processTask(NotificationTaskEntity task) {
        NotificationMessage message = toMessage(task);
        NotificationConverter converter = converterFactory.getConverter(task.getTargetSystem());

        VendorHttpRequest request = converter.convert(message);
        SendResult result = channel.send(request);
        LocalDateTime now = LocalDateTime.now();
        task.setUpdatedAt(now);

        if (result.isSuccess()) {
            task.setStatus(TaskStatus.SUCCESS);
            task.setLastError(null);
            repository.save(task);
            log.info("Notification delivered requestId={} target={}", task.getRequestId(), task.getTargetSystem());
            return;
        }

        handleFailure(task, result, now);
    }

    /**
     * 投递失败处理：递增重试次数，按配置的指数退避设置 nextRetryTime；
     * 超过重试上限则标记 DEAD。
     */
    private void handleFailure(NotificationTaskEntity task, SendResult result, LocalDateTime now) {
        int nextRetryCount = task.getRetryCount() + 1;
        task.setRetryCount(nextRetryCount);
        task.setLastError(truncate(result.getMessage(), 500));
        task.setStatus(TaskStatus.FAILED);

        List<Integer> intervals = properties.getRetryIntervalsMinutes();
        if (nextRetryCount > intervals.size()) {
            task.setStatus(TaskStatus.DEAD);
            task.setNextRetryTime(null);
            repository.save(task);
            log.error("Notification dead requestId={} target={} retries={}",
                    task.getRequestId(), task.getTargetSystem(), nextRetryCount);
            //TODO 整合推送邮箱/飞书等内部通讯软件
            return;
        }

        int delayMinutes = intervals.get(nextRetryCount - 1);
        task.setNextRetryTime(now.plusMinutes(delayMinutes));
        repository.save(task);
        log.warn("Notification failed requestId={} target={} retry={} nextRetryInMin={} error={}",
                task.getRequestId(), task.getTargetSystem(), nextRetryCount, delayMinutes, result.getMessage());
    }

    private NotificationMessage toMessage(NotificationTaskEntity task) {
        NotificationMessage message = new NotificationMessage();
        message.setRequestId(task.getRequestId());
        message.setTargetSystem(task.getTargetSystem());
        message.setEventType(task.getEventType());
        message.setPriority(task.getPriority());
        message.setPayload(readJsonMap(task.getPayload()));
        if (task.getHeaders() != null) {
            message.setHeaders(readJsonStringMap(task.getHeaders()));
        }
        return message;
    }

    private Map<String, Object> readJsonMap(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            throw new IllegalStateException("Invalid payload JSON for task", e);
        }
    }

    private Map<String, String> readJsonStringMap(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            throw new IllegalStateException("Invalid headers JSON for task", e);
        }
    }

    private String truncate(String value, int maxLen) {
        if (value == null) {
            return null;
        }
        return value.length() <= maxLen ? value : value.substring(0, maxLen);
    }
}

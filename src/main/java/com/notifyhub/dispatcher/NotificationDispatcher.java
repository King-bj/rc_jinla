package com.notifyhub.dispatcher;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.notifyhub.acl.ChannelFactory;
import com.notifyhub.acl.ConverterFactory;
import com.notifyhub.config.NotifyHubProperties;
import com.notifyhub.domain.TaskStatus;
import com.notifyhub.entity.NotificationTaskEntity;
import com.notifyhub.repository.NotificationTaskRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 通知投递调度器（骨架）：实现轮询拉取，投递逻辑待后续提交补全。
 */
@Component
public class NotificationDispatcher {

    private static final Logger log = LoggerFactory.getLogger(NotificationDispatcher.class);

    private final NotificationTaskRepository repository;
    private final ConverterFactory converterFactory;
    private final ChannelFactory channelFactory;
    private final NotifyHubProperties properties;
    private final ObjectMapper objectMapper;

    public NotificationDispatcher(NotificationTaskRepository repository,
                                  ConverterFactory converterFactory,
                                  ChannelFactory channelFactory,
                                  NotifyHubProperties properties,
                                  ObjectMapper objectMapper) {
        this.repository = repository;
        this.converterFactory = converterFactory;
        this.channelFactory = channelFactory;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Scheduled(fixedDelayString = "${notify-hub.dispatcher.interval-ms:10000}")
    public void dispatch() {
        LocalDateTime now = LocalDateTime.now();
        int batchSize = properties.getDispatcher().getBatchSize();
        List<NotificationTaskEntity> tasks = repository.findReadyTasks(
                TaskStatus.PENDING,
                now,
                PageRequest.of(0, batchSize)
        );

        for (NotificationTaskEntity task : tasks) {
            try {
                processTask(task);
            } catch (Exception e) {
                log.error("Unexpected error dispatching task id={} requestId={}",
                        task.getId(), task.getRequestId(), e);
            }
        }
    }

    @Transactional
    public void processTask(NotificationTaskEntity task) {
        throw new UnsupportedOperationException("Dispatcher not implemented yet");
    }
}

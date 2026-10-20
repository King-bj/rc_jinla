package com.notifyhub.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.notifyhub.api.dto.NotificationRequest;
import com.notifyhub.api.dto.NotificationResponse;
import com.notifyhub.domain.TaskStatus;
import com.notifyhub.entity.NotificationTaskEntity;
import com.notifyhub.entity.NotificationVendorEntity;
import com.notifyhub.repository.NotificationTaskRepository;
import com.notifyhub.repository.NotificationVendorRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 通知接入服务：负责消息落库与幂等控制。
 * 采用 At Least Once 语义，接入后立即返回，实际投递由调度器异步完成。
 */
@Service
public class NotificationService {

    private static final int PAGE_SIZE = 20;

    private final NotificationTaskRepository repository;
    private final NotificationVendorRepository vendorRepository;
    private final ObjectMapper objectMapper;

    public NotificationService(NotificationTaskRepository repository,
                               NotificationVendorRepository vendorRepository,
                               ObjectMapper objectMapper) {
        this.repository = repository;
        this.vendorRepository = vendorRepository;
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

    public Page<NotificationTaskEntity> search(String status, String vendor, String requestId, int page) {
        TaskStatus parsed = blank(status) ? null : TaskStatus.valueOf(status);
        String vendorCode = blankToNull(vendor);
        String request = blankToNull(requestId);
        Specification<NotificationTaskEntity> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (parsed != null) {
                predicates.add(cb.equal(root.get("status"), parsed));
            }
            if (vendorCode != null) {
                predicates.add(cb.equal(root.get("targetSystem"), vendorCode));
            }
            if (request != null) {
                predicates.add(cb.equal(root.get("requestId"), request));
            }
            if (predicates.isEmpty()) {
                return cb.conjunction();
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
        int index = Math.max(page, 0);
        return repository.findAll(spec, PageRequest.of(index, PAGE_SIZE, Sort.by(Sort.Direction.DESC, "createdAt")));
    }

    public NotificationTaskEntity get(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("task not found"));
    }

    /** 只把死信放回队列。其他状态拒绝。 */
    @Transactional
    public void replay(Long id) {
        NotificationTaskEntity task = get(id);
        if (task.getStatus() != TaskStatus.DEAD) {
            throw new IllegalStateException("only DEAD tasks can be replayed");
        }
        LocalDateTime now = LocalDateTime.now();
        task.setStatus(TaskStatus.PENDING);
        task.setRetryCount(0);
        task.setNextRetryTime(now);
        task.setLastError(null);
        task.setUpdatedAt(now);
        repository.save(task);
    }

    /** 首次提交：写入本地消息表，状态 PENDING，等待调度器拾取 */
    private NotificationResponse createTask(NotificationRequest request) {
        NotificationVendorEntity vendor = vendorRepository.findById(request.getTargetSystem())
                .orElseThrow(() -> new IllegalArgumentException("Unknown vendor: " + request.getTargetSystem()));
        if (!vendor.isEnabled()) {
            throw new IllegalArgumentException("Vendor is disabled: " + request.getTargetSystem());
        }
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

        try {
            NotificationTaskEntity saved = repository.save(task);
            return toResponse(saved);
        } catch (DataIntegrityViolationException e) {
            return repository.findByRequestId(request.getRequestId())
                    .map(this::toResponse)
                    .orElseThrow(() -> e);
        }
    }

    private NotificationResponse toResponse(NotificationTaskEntity task) {
        return new NotificationResponse(
                task.getId(),
                task.getRequestId(),
                task.getStatus().name(),
                "ACCEPTED"
        );
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private static String blankToNull(String value) {
        return blank(value) ? null : value;
    }

    private String toJson(Map<?, ?> map) {
        try {
            return objectMapper.writeValueAsString(map);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Invalid JSON field", e);
        }
    }
}

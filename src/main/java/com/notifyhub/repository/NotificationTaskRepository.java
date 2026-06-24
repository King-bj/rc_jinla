package com.notifyhub.repository;

import com.notifyhub.domain.TaskStatus;
import com.notifyhub.entity.NotificationTaskEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 通知任务数据访问。
 */
public interface NotificationTaskRepository extends JpaRepository<NotificationTaskEntity, Long> {

    /** 按幂等键查询，用于接入层去重 */
    Optional<NotificationTaskEntity> findByRequestId(String requestId);

    /**
     * 查询到达投递时间的待处理任务（含首次 PENDING 与重试等待中的 FAILED）。
     * 按优先级降序、创建时间升序，保证高优任务先出队。
     */
    @Query("SELECT t FROM NotificationTaskEntity t " +
            "WHERE t.status IN :statuses " +
            "AND (t.nextRetryTime IS NULL OR t.nextRetryTime <= :now) " +
            "ORDER BY t.priority DESC, t.createdAt ASC")
    List<NotificationTaskEntity> findReadyTasks(
            @Param("statuses") List<TaskStatus> statuses,
            @Param("now") LocalDateTime now,
            Pageable pageable);
}

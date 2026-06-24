package com.notifyhub.domain;

/**
 * 通知任务生命周期状态。
 * <p>
 * PENDING → SUCCESS（投递成功）<br>
 * PENDING → FAILED → PENDING（重试等待）→ DEAD（超过最大重试次数）
 */
public enum TaskStatus {
    /** 待投递或等待下次重试 */
    PENDING,
    /** 已成功送达第三方 */
    SUCCESS,
    /** 最近一次投递失败（重试调度前短暂状态） */
    FAILED,
    /** 重试耗尽，进入死信，需人工补偿 */
    DEAD
}

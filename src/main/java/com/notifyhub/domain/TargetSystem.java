package com.notifyhub.domain;

/**
 * 外部通知目标系统，对应防腐层中不同的 Converter / 渠道配置。
 */
public enum TargetSystem {
    /** 客户关系管理系统 */
    CRM,
    /** 广告投放系统 */
    AD,
    /** 库存系统 */
    INVENTORY
}

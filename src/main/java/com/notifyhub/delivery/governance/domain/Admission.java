package com.notifyhub.delivery.governance.domain;

/** 执行前的准入结果。 */
public enum Admission {
    ALLOW,
    BULKHEAD_FULL,
    DEGRADE_SKIP,
    DEGRADE_RETRY
}

package com.notifyhub.delivery.governance.domain;

/** 按目的地决定能否发送。实现可以是内存，也可以以后换成 Redis。 */
public interface AdmissionGuard {

    Admission evaluate(String destinationId);

    void release(String destinationId);
}

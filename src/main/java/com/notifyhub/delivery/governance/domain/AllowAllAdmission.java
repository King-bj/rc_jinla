package com.notifyhub.delivery.governance.domain;

/** 不限额、不熔断。已有执行测试继续直接发送。 */
public final class AllowAllAdmission implements AdmissionGuard {

    @Override
    public Admission evaluate(String destinationId) {
        return Admission.ALLOW;
    }

    @Override
    public void release(String destinationId) {
    }
}

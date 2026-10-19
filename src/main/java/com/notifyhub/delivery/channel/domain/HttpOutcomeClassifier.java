package com.notifyhub.delivery.channel.domain;

import java.util.Set;

/** 按目的地配置的成功码解释 HTTP 结果。不发起网络调用。 */
public final class HttpOutcomeClassifier {

    public SendOutcome classify(int statusCode, boolean timedOut, Set<Integer> successCodes) {
        if (timedOut) {
            return SendOutcome.RETRYABLE;
        }
        if (successCodes.contains(statusCode)) {
            return SendOutcome.SUCCESS;
        }
        if (statusCode >= 500) {
            return SendOutcome.RETRYABLE;
        }
        if (statusCode >= 400) {
            return SendOutcome.TERMINAL;
        }
        return SendOutcome.TERMINAL;
    }
}

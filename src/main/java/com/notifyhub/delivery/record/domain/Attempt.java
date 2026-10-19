package com.notifyhub.delivery.record.domain;

import com.notifyhub.delivery.channel.domain.SendOutcome;

import java.time.Instant;

/** 一次已经发出的尝试。 */
public final class Attempt {

    private final String idempotencyKey;
    private final SendOutcome outcome;
    private final Instant at;

    public Attempt(String idempotencyKey, SendOutcome outcome, Instant at) {
        this.idempotencyKey = idempotencyKey;
        this.outcome = outcome;
        this.at = at;
    }

    public String idempotencyKey() {
        return idempotencyKey;
    }

    public SendOutcome outcome() {
        return outcome;
    }

    public Instant at() {
        return at;
    }
}

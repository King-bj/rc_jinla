package com.notifyhub.delivery.record.domain;

import java.util.ArrayList;
import java.util.List;

/** 按发生顺序保存尝试记录。 */
public final class AttemptLog {

    private final List<Attempt> attempts = new ArrayList<>();

    public void append(Attempt attempt) {
        attempts.add(attempt);
    }

    public List<Attempt> attempts() {
        return List.copyOf(attempts);
    }
}

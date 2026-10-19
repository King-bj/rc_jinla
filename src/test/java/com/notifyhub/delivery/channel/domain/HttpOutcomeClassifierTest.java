package com.notifyhub.delivery.channel.domain;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HttpOutcomeClassifierTest {

    private final HttpOutcomeClassifier classifier = new HttpOutcomeClassifier();
    private final Set<Integer> successCodes = Set.of(200, 204);

    @Test
    void classifiesSuccessServerErrorClientErrorAndTimeout() {
        assertEquals(SendOutcome.SUCCESS, classifier.classify(204, false, successCodes));
        assertEquals(SendOutcome.RETRYABLE, classifier.classify(500, false, successCodes));
        assertEquals(SendOutcome.TERMINAL, classifier.classify(400, false, successCodes));
        assertEquals(SendOutcome.RETRYABLE, classifier.classify(400, true, successCodes));
    }
}

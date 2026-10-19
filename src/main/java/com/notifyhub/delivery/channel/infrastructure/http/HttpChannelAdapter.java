package com.notifyhub.delivery.channel.infrastructure.http;

import com.notifyhub.delivery.channel.domain.AdapterType;
import com.notifyhub.delivery.channel.domain.ChannelAdapter;
import com.notifyhub.delivery.channel.domain.HttpOutcomeClassifier;
import com.notifyhub.delivery.channel.domain.RenderedRequest;
import com.notifyhub.delivery.channel.domain.SendOutcome;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.Set;

/** 把渲染后的报文发到 HTTP 目的地，并用成功码配置解释响应。 */
public final class HttpChannelAdapter implements ChannelAdapter {

    public static final String IDEMPOTENCY_HEADER = "Idempotency-Key";

    private final HttpClient client;
    private final HttpOutcomeClassifier classifier;
    private final Set<Integer> successCodes;

    public HttpChannelAdapter(HttpClient client, HttpOutcomeClassifier classifier, Set<Integer> successCodes) {
        this.client = client;
        this.classifier = classifier;
        this.successCodes = Set.copyOf(successCodes);
    }

    @Override
    public AdapterType type() {
        return AdapterType.HTTP;
    }

    @Override
    public SendOutcome send(RenderedRequest request) {
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(request.target()))
                .timeout(Duration.ofSeconds(3))
                .header("Content-Type", "application/json")
                .header(IDEMPOTENCY_HEADER, request.idempotencyKey())
                .POST(HttpRequest.BodyPublishers.ofString(request.body()))
                .build();
        try {
            HttpResponse<String> response = client.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            return classifier.classify(response.statusCode(), false, successCodes);
        } catch (HttpTimeoutException ex) {
            return classifier.classify(0, true, successCodes);
        } catch (IOException ex) {
            return classifier.classify(0, true, successCodes);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return classifier.classify(0, true, successCodes);
        }
    }
}

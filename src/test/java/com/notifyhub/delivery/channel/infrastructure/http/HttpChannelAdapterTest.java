package com.notifyhub.delivery.channel.infrastructure.http;

import com.notifyhub.delivery.channel.application.TemplateRenderer;
import com.notifyhub.delivery.channel.domain.HttpOutcomeClassifier;
import com.notifyhub.delivery.channel.domain.RenderedRequest;
import com.notifyhub.delivery.channel.domain.SendOutcome;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HttpChannelAdapterTest {

    @Test
    void sendsRenderedBodyAndClassifiesStatus() throws Exception {
        AtomicReference<String> body = new AtomicReference<>();
        AtomicReference<String> idempotencyKey = new AtomicReference<>();
        AtomicInteger status = new AtomicInteger(204);
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/hooks/crm", exchange -> {
            body.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            idempotencyKey.set(exchange.getRequestHeaders().getFirst(HttpChannelAdapter.IDEMPOTENCY_HEADER));
            byte[] response = "ok".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(status.get(), response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();
        try {
            String rendered = new TemplateRenderer().render(
                    "user={{payload.userId}}",
                    Map.of("userId", "1001"),
                    "tenant:event:crm");
            HttpChannelAdapter adapter = new HttpChannelAdapter(
                    HttpClient.newHttpClient(),
                    new HttpOutcomeClassifier(),
                    Set.of(200, 204));
            String url = "http://127.0.0.1:" + server.getAddress().getPort() + "/hooks/crm";
            RenderedRequest request = new RenderedRequest(url, rendered, "tenant:event:crm");

            assertEquals(SendOutcome.SUCCESS, adapter.send(request));
            assertEquals("user=1001", body.get());
            assertEquals("tenant:event:crm", idempotencyKey.get());

            status.set(500);
            assertEquals(SendOutcome.RETRYABLE, adapter.send(request));
            status.set(400);
            assertEquals(SendOutcome.TERMINAL, adapter.send(request));
        } finally {
            server.stop(0);
        }
    }
}

package com.notifyhub.acl;

import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * 基于 JDK HttpClient 的通用 HTTP 通知渠道。
 * 2xx 视为成功，其余状态码或网络异常均视为失败并触发重试。
 */
@Component
public class HttpNotificationChannel implements NotificationChannel {

    private final HttpClient httpClient;

    public HttpNotificationChannel(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    @Override
    public SendResult send(VendorHttpRequest request) {
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(request.getUrl()))
                    .timeout(Duration.ofSeconds(30))
                    .method(request.getMethod(), HttpRequest.BodyPublishers.ofString(request.getBody()));

            request.getHeaders().forEach(builder::header);

            HttpResponse<String> response = httpClient.send(
                    builder.build(),
                    HttpResponse.BodyHandlers.ofString()
            );

            int code = response.statusCode();
            if (code >= 200 && code < 300) {
                return SendResult.ok(code);
            }
            return SendResult.fail(code, response.body());
        } catch (Exception e) {
            return SendResult.fail(-1, e.getMessage());
        }
    }
}

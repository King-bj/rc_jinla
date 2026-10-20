package com.notifyhub.acl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.notifyhub.domain.NotificationMessage;
import com.notifyhub.entity.NotificationVendorEntity;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 按供应商模板组装 HTTP 请求。
 * <p>
 * {@code {{eventType}}}、{@code {{requestId}}} 按 JSON 字符串内容插入（模板自己带引号）。
 * {@code {{payload}}} 插入整段 JSON。{@code {{payload.字段}}} 插入该字段的 JSON 值。
 */
@Component
public class VendorTemplateRenderer {

    private static final Pattern TOKEN = Pattern.compile("\\{\\{\\s*([^{}]+?)\\s*\\}\\}");

    private final ObjectMapper objectMapper;

    public VendorTemplateRenderer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public VendorHttpRequest render(NotificationVendorEntity vendor, NotificationMessage message) {
        String url = render(vendor.getUrlTemplate(), message, false).trim();
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            throw new IllegalArgumentException("url must be http or https");
        }
        String body = render(vendor.getBodyTemplate(), message, true);
        if (!isJson(body)) {
            throw new IllegalArgumentException("body is not valid JSON");
        }
        Map<String, String> headers = renderHeaders(vendor.getHeadersTemplate(), message);
        if (message.getHeaders() != null) {
            headers.putAll(message.getHeaders());
        }
        return new VendorHttpRequest(url, vendor.getMethod(), headers, body);
    }

    private Map<String, String> renderHeaders(String template, NotificationMessage message) {
        String source = template == null || template.isBlank() ? VendorTemplates.DEFAULT_HEADERS : template;
        String rendered = render(source, message, true);
        JsonNode node;
        try {
            node = objectMapper.readTree(rendered);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("headers are not valid JSON");
        }
        if (!node.isObject()) {
            throw new IllegalArgumentException("headers must be a JSON object");
        }
        Map<String, String> headers = new LinkedHashMap<>();
        node.fields().forEachRemaining(entry -> {
            if (!entry.getValue().isTextual()) {
                throw new IllegalArgumentException("header value must be a string: " + entry.getKey());
            }
            headers.put(entry.getKey(), entry.getValue().asText());
        });
        return headers;
    }

    private String render(String template, NotificationMessage message, boolean json) {
        if (template == null) {
            throw new IllegalArgumentException("template is required");
        }
        Matcher matcher = TOKEN.matcher(template);
        StringBuilder rendered = new StringBuilder();
        while (matcher.find()) {
            matcher.appendReplacement(rendered, Matcher.quoteReplacement(resolve(matcher.group(1).trim(), message, json)));
        }
        matcher.appendTail(rendered);
        if (rendered.indexOf("{{") >= 0) {
            throw new IllegalArgumentException("incomplete template");
        }
        return rendered.toString();
    }

    private String resolve(String expression, NotificationMessage message, boolean json) {
        if ("eventType".equals(expression)) {
            return text(message.getEventType(), "eventType", json);
        }
        if ("requestId".equals(expression)) {
            return text(message.getRequestId(), "requestId", json);
        }
        if ("payload".equals(expression)) {
            if (message.getPayload() == null) {
                throw new IllegalArgumentException("missing payload");
            }
            return writeJson(message.getPayload());
        }
        if (expression.startsWith("payload.")) {
            String field = expression.substring("payload.".length());
            if (field.isBlank() || field.indexOf('.') >= 0) {
                throw new IllegalArgumentException("unknown template variable " + expression);
            }
            Map<String, Object> payload = message.getPayload();
            if (payload == null || !payload.containsKey(field) || payload.get(field) == null) {
                throw new IllegalArgumentException("missing payload." + field);
            }
            Object value = payload.get(field);
            return json ? writeJson(value) : raw(value);
        }
        throw new IllegalArgumentException("unknown template variable " + expression);
    }

    private String text(String value, String name, boolean json) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("missing " + name);
        }
        return json ? jsonStringContent(value) : value;
    }

    private String jsonStringContent(String value) {
        String quoted = writeJson(value);
        return quoted.substring(1, quoted.length() - 1);
    }

    private String raw(Object value) {
        if (value instanceof String text) {
            return text;
        }
        if (value instanceof Number || value instanceof Boolean) {
            return String.valueOf(value);
        }
        return writeJson(value);
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("failed to write JSON", ex);
        }
    }

    private boolean isJson(String value) {
        try {
            objectMapper.readTree(value);
            return true;
        } catch (JsonProcessingException ex) {
            return false;
        }
    }
}

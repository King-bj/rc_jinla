package com.notifyhub.delivery.channel.application;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** 替换 payload 字段和任务幂等键。任一变量缺失时失败，不返回半截报文。 */
public final class TemplateRenderer {

    private static final Pattern TOKEN = Pattern.compile("\\{\\{\\s*([^{}]+?)\\s*\\}\\}");

    public String render(String template, Map<String, Object> payload, String idempotencyKey) {
        Matcher matcher = TOKEN.matcher(template);
        StringBuilder rendered = new StringBuilder();
        while (matcher.find()) {
            matcher.appendReplacement(rendered, Matcher.quoteReplacement(resolve(matcher.group(1).trim(), payload, idempotencyKey)));
        }
        matcher.appendTail(rendered);
        if (rendered.indexOf("{{") >= 0) {
            throw new IllegalArgumentException("incomplete template");
        }
        return rendered.toString();
    }

    private static String resolve(String expression, Map<String, Object> payload, String idempotencyKey) {
        if ("task.idempotencyKey".equals(expression)) {
            if (idempotencyKey == null || idempotencyKey.isBlank()) {
                throw new IllegalArgumentException("task.idempotencyKey is required");
            }
            return idempotencyKey;
        }
        if (expression.startsWith("payload.")) {
            String field = expression.substring("payload.".length());
            if (field.isBlank() || !payload.containsKey(field) || payload.get(field) == null) {
                throw new IllegalArgumentException("missing payload." + field);
            }
            return String.valueOf(payload.get(field));
        }
        throw new IllegalArgumentException("unknown template variable " + expression);
    }
}

package com.notifyhub.acl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.notifyhub.domain.NotificationMessage;
import com.notifyhub.entity.NotificationVendorEntity;
import com.notifyhub.support.TestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 种子模板应复现原来三个 Converter 的 URL 和 body，新供应商只靠配置。
 */
class VendorTemplateRendererTest {

    private ObjectMapper objectMapper;
    private VendorTemplateRenderer renderer;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        renderer = new VendorTemplateRenderer(objectMapper);
    }

    static Stream<Arguments> seedCases() {
        return Stream.of(
                Arguments.of(VendorTemplates.crm("http://crm.test"), "PAYMENT_SUCCESS",
                        Map.of("userId", "1001", "status", "PAID"),
                        "http://crm.test/contacts/update", "event", "contact"),
                Arguments.of(VendorTemplates.ad("http://ad.test"), "USER_REGISTERED",
                        Map.of("campaignId", "ad-88", "clickId", "clk-1"),
                        "http://ad.test/conversions", "conversion_event", "data"),
                Arguments.of(VendorTemplates.inventory("http://inventory.test"), "STOCK_DEDUCT",
                        Map.of("sku", "SKU-001", "quantity", (Object) 2),
                        "http://inventory.test/stock/adjust", "event_type", "items")
        );
    }

    @ParameterizedTest
    @MethodSource("seedCases")
    void seedTemplates_matchFormerConverters(NotificationVendorEntity vendor,
                                             String eventType,
                                             Map<String, Object> payload,
                                             String expectedUrl,
                                             String eventField,
                                             String payloadField) throws Exception {
        NotificationMessage message = TestData.message(vendor.getCode(), eventType, payload, null);

        VendorHttpRequest request = renderer.render(vendor, message);

        assertEquals("POST", request.getMethod());
        assertEquals(expectedUrl, request.getUrl());
        assertEquals("application/json", request.getHeaders().get("Content-Type"));
        JsonNode body = objectMapper.readTree(request.getBody());
        assertEquals(eventType, body.get(eventField).asText());
        assertEquals(objectMapper.valueToTree(payload), body.get(payloadField));
    }

    @Test
    void callerHeaders_overrideTemplateHeaders() {
        NotificationVendorEntity vendor = VendorTemplates.crm("http://crm.test");
        NotificationMessage message = TestData.message("CRM", "E", Map.of("userId", "1"),
                Map.of("X-Api-Key", "secret", "Content-Type", "application/custom"));

        VendorHttpRequest request = renderer.render(vendor, message);

        assertEquals("secret", request.getHeaders().get("X-Api-Key"));
        assertEquals("application/custom", request.getHeaders().get("Content-Type"));
    }

    @Test
    void newVendor_usesConfiguredUrlAndFields() throws Exception {
        NotificationVendorEntity vendor = VendorTemplates.vendor(
                "BILLING",
                "账单",
                "http://billing.test/hooks/{{payload.userId}}",
                "{\"event\":\"{{eventType}}\",\"user\":{{payload.userId}},\"qty\":{{payload.qty}},\"extra\":{{payload.extra}}}");
        Map<String, Object> payload = new java.util.HashMap<>();
        payload.put("userId", "42");
        payload.put("qty", 2);
        payload.put("extra", Map.of("ok", true));
        NotificationMessage message = TestData.message("BILLING", "INVOICE", payload, null);

        VendorHttpRequest request = renderer.render(vendor, message);

        assertEquals("http://billing.test/hooks/42", request.getUrl());
        JsonNode body = objectMapper.readTree(request.getBody());
        assertEquals("INVOICE", body.get("event").asText());
        assertEquals("42", body.get("user").asText());
        assertEquals(2, body.get("qty").asInt());
        assertTrue(body.get("extra").get("ok").asBoolean());
    }

    @Test
    void missingField_fails() {
        NotificationVendorEntity vendor = VendorTemplates.vendor(
                "BILLING", "账单", "https://billing.test/hook", "{\"name\":{{payload.name}}}");
        NotificationMessage message = TestData.message("BILLING", "E", Map.of("userId", "1"), null);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> renderer.render(vendor, message));
        assertTrue(ex.getMessage().contains("missing payload.name"));
    }

    @Test
    void nonHttpUrl_fails() {
        NotificationVendorEntity vendor = VendorTemplates.vendor(
                "BILLING", "账单", "ftp://billing.test/hook", "{{payload}}");
        NotificationMessage message = TestData.message("BILLING", "E", Map.of("a", 1), null);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> renderer.render(vendor, message));
        assertTrue(ex.getMessage().contains("http"));
    }
}

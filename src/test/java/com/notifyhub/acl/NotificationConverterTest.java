package com.notifyhub.acl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.notifyhub.acl.ad.AdConverter;
import com.notifyhub.acl.crm.CrmConverter;
import com.notifyhub.acl.inventory.InventoryConverter;
import com.notifyhub.config.NotifyHubProperties;
import com.notifyhub.domain.NotificationMessage;
import com.notifyhub.domain.TargetSystem;
import com.notifyhub.support.TestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 防腐层 Converter 参数化测试。
 * <p>
 * 验证 CRM / AD / INVENTORY 三渠道在 URL、Body 字段名、payload 映射上的差异，
 * 以及业务自定义 Header 的透传合并。
 */
class NotificationConverterTest {

    private ObjectMapper objectMapper;
    private NotifyHubProperties properties;
    private CrmConverter crmConverter;
    private AdConverter adConverter;
    private InventoryConverter inventoryConverter;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        properties = TestData.properties();
        crmConverter = new CrmConverter(objectMapper, properties);
        adConverter = new AdConverter(objectMapper, properties);
        inventoryConverter = new InventoryConverter(objectMapper, properties);
    }

    /** 各渠道转换用例：事件类型、payload、目标 URL 与 Body 字段名 */
    static Stream<Arguments> channelConversionCases() {
        return Stream.of(
                Arguments.of(
                        TargetSystem.CRM,
                        "PAYMENT_SUCCESS",
                        Map.of("userId", "1001", "status", "PAID"),
                        "http://crm.test/contacts/update",
                        "event",
                        "contact"
                ),
                Arguments.of(
                        TargetSystem.AD,
                        "USER_REGISTERED",
                        Map.of("campaignId", "ad-88", "clickId", "clk-1"),
                        "http://ad.test/conversions",
                        "conversion_event",
                        "data"
                ),
                Arguments.of(
                        TargetSystem.INVENTORY,
                        "STOCK_DEDUCT",
                        Map.of("sku", "SKU-001", "quantity", 2),
                        "http://inventory.test/stock/adjust",
                        "event_type",
                        "items"
                )
        );
    }

    /** 同一领域消息在不同渠道产出不同的 HTTP 请求结构与路径 */
    @ParameterizedTest
    @MethodSource("channelConversionCases")
    void convert_buildsChannelSpecificRequest(TargetSystem targetSystem,
                                              String eventType,
                                              Map<String, Object> payload,
                                              String expectedUrl,
                                              String eventField,
                                              String payloadField) throws Exception {
        NotificationMessage message = TestData.message(targetSystem, eventType, payload, null);
        NotificationConverter converter = converterFor(targetSystem);

        VendorHttpRequest request = converter.convert(message);

        assertEquals("POST", request.getMethod());
        assertEquals(expectedUrl, request.getUrl());
        assertEquals("application/json", request.getHeaders().get("Content-Type"));

        JsonNode body = objectMapper.readTree(request.getBody());
        assertEquals(eventType, body.get(eventField).asText());
        assertEquals(objectMapper.valueToTree(payload), body.get(payloadField));
    }

    /** 业务 headers 与默认 Content-Type 合并，不覆盖彼此 */
    @ParameterizedTest
    @MethodSource("channelConversionCases")
    void convert_mergesCustomHeaders(TargetSystem targetSystem,
                                     String eventType,
                                     Map<String, Object> payload,
                                     String expectedUrl,
                                     String eventField,
                                     String payloadField) {
        Map<String, String> customHeaders = Map.of("X-Api-Key", "secret-" + targetSystem.name());
        NotificationMessage message = TestData.message(targetSystem, eventType, payload, customHeaders);

        VendorHttpRequest request = converterFor(targetSystem).convert(message);

        assertEquals("secret-" + targetSystem.name(), request.getHeaders().get("X-Api-Key"));
        assertTrue(request.getUrl().startsWith("http://"));
    }

    private NotificationConverter converterFor(TargetSystem targetSystem) {
        return switch (targetSystem) {
            case CRM -> crmConverter;
            case AD -> adConverter;
            case INVENTORY -> inventoryConverter;
        };
    }
}

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

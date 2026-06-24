package com.notifyhub.acl;

import com.notifyhub.acl.ad.AdConverter;
import com.notifyhub.acl.crm.CrmConverter;
import com.notifyhub.acl.inventory.InventoryConverter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.notifyhub.domain.TargetSystem;
import com.notifyhub.support.TestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class ConverterFactoryTest {

    private ConverterFactory factory;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();
        var properties = TestData.properties();
        factory = new ConverterFactory(
                new CrmConverter(objectMapper, properties),
                new AdConverter(objectMapper, properties),
                new InventoryConverter(objectMapper, properties)
        );
    }

    @Test
    void getConverter_returnsConverterPerChannel() {
        assertEquals("/contacts/update",
                factory.getConverter(TargetSystem.CRM)
                        .convert(TestData.message(TargetSystem.CRM, "E", Map.of("k", "v"), null))
                        .getUrl()
                        .replace("http://crm.test", ""));

        assertEquals("/conversions",
                factory.getConverter(TargetSystem.AD)
                        .convert(TestData.message(TargetSystem.AD, "E", Map.of("k", "v"), null))
                        .getUrl()
                        .replace("http://ad.test", ""));

        assertEquals("/stock/adjust",
                factory.getConverter(TargetSystem.INVENTORY)
                        .convert(TestData.message(TargetSystem.INVENTORY, "E", Map.of("k", "v"), null))
                        .getUrl()
                        .replace("http://inventory.test", ""));
    }

    @Test
    void getConverter_sameInstanceForSameChannel() {
        NotificationConverter first = factory.getConverter(TargetSystem.CRM);
        NotificationConverter second = factory.getConverter(TargetSystem.CRM);
        assertSame(first, second);
    }
}

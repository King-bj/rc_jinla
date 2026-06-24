package com.notifyhub.acl;

import com.notifyhub.acl.ad.AdConverter;
import com.notifyhub.acl.crm.CrmConverter;
import com.notifyhub.acl.inventory.InventoryConverter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.notifyhub.domain.TargetSystem;
import com.notifyhub.support.TestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * ConverterFactory 路由测试：按 targetSystem 返回对应渠道 Converter。
 */
class ConverterFactoryTest {

    private ConverterFactory factory;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();
        var properties = TestData.properties();
        factory = new ConverterFactory(List.of(
                new CrmConverter(objectMapper, properties),
                new AdConverter(objectMapper, properties),
                new InventoryConverter(objectMapper, properties)
        ));
    }

    /** CRM→/contacts/update，AD→/conversions，INVENTORY→/stock/adjust */
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

    /** 同一渠道多次获取应返回同一 Converter 实例（Spring 单例） */
    @Test
    void getConverter_sameInstanceForSameChannel() {
        NotificationConverter first = factory.getConverter(TargetSystem.CRM);
        NotificationConverter second = factory.getConverter(TargetSystem.CRM);
        assertSame(first, second);
    }
}

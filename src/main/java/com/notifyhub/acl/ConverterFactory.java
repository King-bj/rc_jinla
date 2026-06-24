package com.notifyhub.acl;

import com.notifyhub.acl.ad.AdConverter;
import com.notifyhub.acl.crm.CrmConverter;
import com.notifyhub.acl.inventory.InventoryConverter;
import com.notifyhub.domain.TargetSystem;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.Map;

/**
 * 按目标系统路由到对应的 Converter 实现。
 */
@Component
public class ConverterFactory {

    private final Map<TargetSystem, NotificationConverter> converters;

    public ConverterFactory(CrmConverter crmConverter,
                            AdConverter adConverter,
                            InventoryConverter inventoryConverter) {
        converters = new EnumMap<>(TargetSystem.class);
        converters.put(TargetSystem.CRM, crmConverter);
        converters.put(TargetSystem.AD, adConverter);
        converters.put(TargetSystem.INVENTORY, inventoryConverter);
    }

    public NotificationConverter getConverter(TargetSystem targetSystem) {
        NotificationConverter converter = converters.get(targetSystem);
        if (converter == null) {
            throw new IllegalArgumentException("Unsupported target system: " + targetSystem);
        }
        return converter;
    }
}

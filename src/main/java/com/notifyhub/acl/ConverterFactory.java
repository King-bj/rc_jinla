package com.notifyhub.acl;

import com.notifyhub.domain.TargetSystem;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * 按目标系统路由到对应的 Converter 实现。
 */
@Component
public class ConverterFactory {

    private final Map<TargetSystem, NotificationConverter> converters;

    public ConverterFactory(List<NotificationConverter> converters) {
        this.converters = new EnumMap<>(TargetSystem.class);
        for (NotificationConverter converter : converters) {
            TargetSystem target = converter.supports();
            if (this.converters.putIfAbsent(target, converter) != null) {
                throw new IllegalStateException("Duplicate converter for: " + target);
            }
        }
    }

    public NotificationConverter getConverter(TargetSystem targetSystem) {
        NotificationConverter converter = converters.get(targetSystem);
        if (converter == null) {
            throw new IllegalArgumentException("Unsupported target system: " + targetSystem);
        }
        return converter;
    }
}

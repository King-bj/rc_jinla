package com.notifyhub.acl;

import com.notifyhub.domain.TargetSystem;
import org.springframework.stereotype.Component;

/**
 * 转换器工厂（骨架占位），完整实现见后续 ACL 提交。
 */
@Component
public class ConverterFactory {

    public NotificationConverter getConverter(TargetSystem targetSystem) {
        throw new UnsupportedOperationException("ConverterFactory not implemented yet");
    }
}

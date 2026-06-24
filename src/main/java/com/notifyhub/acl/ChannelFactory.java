package com.notifyhub.acl;

import com.notifyhub.domain.TargetSystem;
import org.springframework.stereotype.Component;

/**
 * 渠道工厂（骨架占位），完整实现见后续 ACL 提交。
 */
@Component
public class ChannelFactory {

    public NotificationChannel getChannel(TargetSystem targetSystem) {
        throw new UnsupportedOperationException("ChannelFactory not implemented yet");
    }
}

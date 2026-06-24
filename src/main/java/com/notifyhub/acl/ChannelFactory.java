package com.notifyhub.acl;

import com.notifyhub.domain.TargetSystem;
import org.springframework.stereotype.Component;

/**
 * 按目标系统获取发送渠道。
 * MVP 阶段所有外部系统均通过 HTTP 渠道发送。
 */
@Component
public class ChannelFactory {

    private final HttpNotificationChannel httpChannel;

    public ChannelFactory(HttpNotificationChannel httpChannel) {
        this.httpChannel = httpChannel;
    }

    public NotificationChannel getChannel(TargetSystem targetSystem) {
        return httpChannel;
    }
}

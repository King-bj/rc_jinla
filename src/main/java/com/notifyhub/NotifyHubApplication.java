package com.notifyhub;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 通知中枢应用入口。
 * 启用定时调度，驱动 {@link com.notifyhub.dispatcher.NotificationDispatcher} 轮询投递。
 */
@SpringBootApplication
@EnableScheduling
public class NotifyHubApplication {

    public static void main(String[] args) {
        SpringApplication.run(NotifyHubApplication.class, args);
    }
}

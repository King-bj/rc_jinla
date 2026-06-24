package com.notifyhub.acl;

/**
 * 通知渠道抽象：负责将转换后的请求发送至外部系统。
 */
public interface NotificationChannel {

    SendResult send(VendorHttpRequest request);
}

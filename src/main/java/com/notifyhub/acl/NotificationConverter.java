package com.notifyhub.acl;

import com.notifyhub.domain.NotificationMessage;

/**
 * 防腐层：将统一领域消息转换为供应商 HTTP 请求。
 */
public interface NotificationConverter {

    VendorHttpRequest convert(NotificationMessage message);
}

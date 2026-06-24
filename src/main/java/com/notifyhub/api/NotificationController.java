package com.notifyhub.api;

import com.notifyhub.api.dto.NotificationRequest;
import com.notifyhub.api.dto.NotificationResponse;
import com.notifyhub.service.NotificationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** 通知接入 API，业务系统统一调用入口。 */
@RestController
@RequestMapping("/api/v1")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    /**
     * 提交通知。快速返回 202 Accepted，不等待第三方响应。
     */
    @PostMapping("/notifications")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public NotificationResponse submit(@Valid @RequestBody NotificationRequest request) {
        return notificationService.notify(request);
    }
}

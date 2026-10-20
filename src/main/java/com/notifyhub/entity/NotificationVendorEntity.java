package com.notifyhub.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * 供应商出口配置。地址和报文模板存在这里，发送时读取，改完不用重启。
 */
@Entity
@Table(name = "notification_vendor")
public class NotificationVendorEntity {

    @Id
    @Column(length = 64)
    private String code;

    @Column(nullable = false, length = 128)
    private String name;

    @Column(nullable = false)
    private boolean enabled;

    @Column(nullable = false, length = 16)
    private String method;

    @Column(name = "url_template", nullable = false, length = 1024)
    private String urlTemplate;

    @Column(name = "headers_template", columnDefinition = "TEXT")
    private String headersTemplate;

    @Column(name = "body_template", nullable = false, columnDefinition = "TEXT")
    private String bodyTemplate;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getUrlTemplate() {
        return urlTemplate;
    }

    public void setUrlTemplate(String urlTemplate) {
        this.urlTemplate = urlTemplate;
    }

    public String getHeadersTemplate() {
        return headersTemplate;
    }

    public void setHeadersTemplate(String headersTemplate) {
        this.headersTemplate = headersTemplate;
    }

    public String getBodyTemplate() {
        return bodyTemplate;
    }

    public void setBodyTemplate(String bodyTemplate) {
        this.bodyTemplate = bodyTemplate;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}

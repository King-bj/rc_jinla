package com.notifyhub.acl;

import com.notifyhub.entity.NotificationVendorEntity;

import java.time.LocalDateTime;

/** 与原 CRM / AD / INVENTORY Converter 等价的种子模板。 */
public final class VendorTemplates {

    public static final String DEFAULT_HEADERS = "{\"Content-Type\":\"application/json\"}";

    private VendorTemplates() {
    }

    public static NotificationVendorEntity crm(String baseUrl) {
        return vendor("CRM", "客户关系管理系统", baseUrl + "/contacts/update",
                "{\"event\":\"{{eventType}}\",\"contact\":{{payload}}}");
    }

    public static NotificationVendorEntity ad(String baseUrl) {
        return vendor("AD", "广告投放系统", baseUrl + "/conversions",
                "{\"conversion_event\":\"{{eventType}}\",\"data\":{{payload}}}");
    }

    public static NotificationVendorEntity inventory(String baseUrl) {
        return vendor("INVENTORY", "库存系统", baseUrl + "/stock/adjust",
                "{\"event_type\":\"{{eventType}}\",\"items\":{{payload}}}");
    }

    public static NotificationVendorEntity vendor(String code, String name, String url, String body) {
        NotificationVendorEntity vendor = new NotificationVendorEntity();
        vendor.setCode(code);
        vendor.setName(name);
        vendor.setEnabled(true);
        vendor.setMethod("POST");
        vendor.setUrlTemplate(url);
        vendor.setHeadersTemplate(DEFAULT_HEADERS);
        vendor.setBodyTemplate(body);
        vendor.setUpdatedAt(LocalDateTime.now());
        return vendor;
    }
}

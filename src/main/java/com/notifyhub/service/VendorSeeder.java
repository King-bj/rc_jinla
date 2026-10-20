package com.notifyhub.service;

import com.notifyhub.acl.VendorTemplates;
import com.notifyhub.config.NotifyHubProperties;
import com.notifyhub.entity.NotificationVendorEntity;
import com.notifyhub.repository.NotificationVendorRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 供应商表为空时，用 yml 里的三个地址写入与原 Converter 等价的模板。
 * 表里已有数据后不再读取 yml。
 * 仍指向 localhost:18081/18082/18083 的地址改写到当前渠道根地址，路径保持不变。
 */
@Component
public class VendorSeeder implements ApplicationRunner {

    private static final List<LegacyRoot> LEGACY_ROOTS = List.of(
            new LegacyRoot("http://localhost:18081", "crm"),
            new LegacyRoot("http://localhost:18082", "ad"),
            new LegacyRoot("http://localhost:18083", "inventory")
    );

    private final NotificationVendorRepository repository;
    private final NotifyHubProperties properties;

    public VendorSeeder(NotificationVendorRepository repository, NotifyHubProperties properties) {
        this.repository = repository;
        this.properties = properties;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        seedIfEmpty();
        retargetLegacyUrls();
    }

    public void seedIfEmpty() {
        if (repository.count() > 0) {
            return;
        }
        repository.save(VendorTemplates.crm(baseUrl("crm")));
        repository.save(VendorTemplates.ad(baseUrl("ad")));
        repository.save(VendorTemplates.inventory(baseUrl("inventory")));
    }

    /** 只改写三个旧模拟端口，其它地址原样保留。 */
    public void retargetLegacyUrls() {
        for (NotificationVendorEntity vendor : repository.findAll()) {
            String updated = rewrite(vendor.getUrlTemplate());
            if (updated == null || updated.equals(vendor.getUrlTemplate())) {
                continue;
            }
            vendor.setUrlTemplate(updated);
            vendor.setUpdatedAt(LocalDateTime.now());
            repository.save(vendor);
        }
    }

    private String rewrite(String url) {
        if (url == null) {
            return null;
        }
        for (LegacyRoot legacy : LEGACY_ROOTS) {
            if (url.equals(legacy.prefix) || url.startsWith(legacy.prefix + "/")) {
                return join(baseUrl(legacy.channelKey), url.substring(legacy.prefix.length()));
            }
        }
        return null;
    }

    private static String join(String base, String suffix) {
        if (suffix.isEmpty()) {
            return stripTrailingSlash(base);
        }
        return stripTrailingSlash(base) + suffix;
    }

    private static String stripTrailingSlash(String base) {
        if (base.endsWith("/")) {
            return base.substring(0, base.length() - 1);
        }
        return base;
    }

    private String baseUrl(String key) {
        NotifyHubProperties.ChannelConfig config = properties.getChannels().get(key);
        if (config == null || config.getBaseUrl() == null || config.getBaseUrl().isBlank()) {
            throw new IllegalStateException("Missing channel config for: " + key);
        }
        return config.getBaseUrl();
    }

    private record LegacyRoot(String prefix, String channelKey) {
    }
}

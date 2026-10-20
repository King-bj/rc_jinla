package com.notifyhub.service;

import com.notifyhub.acl.VendorTemplates;
import com.notifyhub.config.NotifyHubProperties;
import com.notifyhub.entity.NotificationVendorEntity;
import com.notifyhub.repository.NotificationVendorRepository;
import com.notifyhub.support.TestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VendorSeederTest {

    @Mock
    private NotificationVendorRepository repository;

    private VendorSeeder seeder;

    @BeforeEach
    void setUp() {
        NotifyHubProperties properties = TestData.properties();
        seeder = new VendorSeeder(repository, properties);
    }

    @Test
    void seedIfEmpty_writesThreeVendorsFromYaml() {
        when(repository.count()).thenReturn(0L);

        seeder.seedIfEmpty();

        ArgumentCaptor<NotificationVendorEntity> captor = ArgumentCaptor.forClass(NotificationVendorEntity.class);
        verify(repository, org.mockito.Mockito.times(3)).save(captor.capture());
        List<String> codes = captor.getAllValues().stream().map(NotificationVendorEntity::getCode).toList();
        assertEquals(List.of("CRM", "AD", "INVENTORY"), codes);
        assertEquals("http://crm.test/contacts/update", captor.getAllValues().get(0).getUrlTemplate());
        assertEquals("{\"event\":\"{{eventType}}\",\"contact\":{{payload}}}", captor.getAllValues().get(0).getBodyTemplate());
    }

    @Test
    void seedIfEmpty_skipsWhenVendorsExist() {
        when(repository.count()).thenReturn(1L);

        seeder.seedIfEmpty();

        verify(repository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void retargetLegacyUrls_rewritesOldPortsAndLeavesOthers() {
        NotificationVendorEntity legacy = VendorTemplates.crm("http://localhost:18081");
        NotificationVendorEntity custom = VendorTemplates.vendor("BILLING", "账单", "https://billing.example/hook", "{}");
        NotificationVendorEntity lookalike = VendorTemplates.vendor("OTHER", "其他", "http://localhost:180810/x", "{}");
        when(repository.findAll()).thenReturn(List.of(legacy, custom, lookalike));

        seeder.retargetLegacyUrls();

        ArgumentCaptor<NotificationVendorEntity> captor = ArgumentCaptor.forClass(NotificationVendorEntity.class);
        verify(repository).save(captor.capture());
        assertEquals("CRM", captor.getValue().getCode());
        assertEquals("http://crm.test/contacts/update", captor.getValue().getUrlTemplate());
    }
}

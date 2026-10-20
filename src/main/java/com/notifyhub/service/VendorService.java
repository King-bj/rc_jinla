package com.notifyhub.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.notifyhub.acl.VendorHttpRequest;
import com.notifyhub.acl.VendorTemplateRenderer;
import com.notifyhub.domain.NotificationMessage;
import com.notifyhub.entity.NotificationVendorEntity;
import com.notifyhub.repository.NotificationVendorRepository;
import com.notifyhub.web.VendorForm;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/** 供应商配置的增改和报文预览。不发送 HTTP。 */
@Service
public class VendorService {

    private static final Pattern CODE = Pattern.compile("[A-Za-z0-9_-]{1,64}");
    private static final Set<String> METHODS = Set.of("POST", "PUT", "PATCH");

    private final NotificationVendorRepository repository;
    private final VendorTemplateRenderer renderer;
    private final ObjectMapper objectMapper;

    public VendorService(NotificationVendorRepository repository,
                         VendorTemplateRenderer renderer,
                         ObjectMapper objectMapper) {
        this.repository = repository;
        this.renderer = renderer;
        this.objectMapper = objectMapper;
    }

    public List<NotificationVendorEntity> list() {
        return repository.findAll(Sort.by("code"));
    }

    public NotificationVendorEntity get(String code) {
        return repository.findById(code)
                .orElseThrow(() -> new IllegalArgumentException("Unknown vendor: " + code));
    }

    public boolean exists(String code) {
        return code != null && repository.existsById(code);
    }

    @Transactional
    public void create(VendorForm form) {
        validate(form);
        if (repository.existsById(form.getCode())) {
            throw new IllegalArgumentException("Vendor already exists: " + form.getCode());
        }
        repository.save(toEntity(form, new NotificationVendorEntity()));
    }

    @Transactional
    public void update(String code, VendorForm form) {
        NotificationVendorEntity existing = get(code);
        form.setCode(code);
        validate(form);
        repository.save(toEntity(form, existing));
    }

    public VendorHttpRequest preview(VendorForm form) {
        validate(form);
        NotificationVendorEntity vendor = toEntity(form, new NotificationVendorEntity());
        return renderer.render(vendor, sampleMessage(form));
    }

    public static VendorForm toForm(NotificationVendorEntity vendor) {
        VendorForm form = new VendorForm();
        form.setCode(vendor.getCode());
        form.setName(vendor.getName());
        form.setEnabled(vendor.isEnabled());
        form.setMethod(vendor.getMethod());
        form.setUrlTemplate(vendor.getUrlTemplate());
        form.setHeadersTemplate(vendor.getHeadersTemplate());
        form.setBodyTemplate(vendor.getBodyTemplate());
        return form;
    }

    private NotificationVendorEntity toEntity(VendorForm form, NotificationVendorEntity vendor) {
        vendor.setCode(form.getCode());
        vendor.setName(form.getName().trim());
        vendor.setEnabled(form.isEnabled());
        vendor.setMethod(form.getMethod());
        vendor.setUrlTemplate(form.getUrlTemplate().trim());
        vendor.setHeadersTemplate(blankToNull(form.getHeadersTemplate()));
        vendor.setBodyTemplate(form.getBodyTemplate());
        vendor.setUpdatedAt(LocalDateTime.now());
        return vendor;
    }

    private void validate(VendorForm form) {
        if (form.getCode() == null || !CODE.matcher(form.getCode()).matches()) {
            throw new IllegalArgumentException("vendor code must match [A-Za-z0-9_-]{1,64}");
        }
        if (form.getName() == null || form.getName().isBlank() || form.getName().trim().length() > 128) {
            throw new IllegalArgumentException("vendor name is required");
        }
        if (form.getMethod() == null || !METHODS.contains(form.getMethod())) {
            throw new IllegalArgumentException("method must be POST, PUT or PATCH");
        }
        if (form.getUrlTemplate() == null || form.getUrlTemplate().isBlank()) {
            throw new IllegalArgumentException("url template is required");
        }
        if (form.getBodyTemplate() == null || form.getBodyTemplate().isBlank()) {
            throw new IllegalArgumentException("body template is required");
        }
    }

    private NotificationMessage sampleMessage(VendorForm form) {
        if (form.getSamplePayload() == null || form.getSamplePayload().isBlank()) {
            throw new IllegalArgumentException("sample payload is required");
        }
        NotificationMessage message = new NotificationMessage();
        message.setRequestId(form.getSampleRequestId());
        message.setTargetSystem(form.getCode());
        message.setEventType(form.getSampleEventType());
        try {
            message.setPayload(objectMapper.readValue(form.getSamplePayload(), new TypeReference<>() {}));
        } catch (Exception ex) {
            throw new IllegalArgumentException("sample payload is not valid JSON");
        }
        return message;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}

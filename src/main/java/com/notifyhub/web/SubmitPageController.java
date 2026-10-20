package com.notifyhub.web;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.notifyhub.api.dto.NotificationRequest;
import com.notifyhub.api.dto.NotificationResponse;
import com.notifyhub.entity.NotificationVendorEntity;
import com.notifyhub.service.NotificationService;
import com.notifyhub.service.VendorService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/** 手工提交通知，走和对外接口相同的接入服务。 */
@Controller
public class SubmitPageController {

    private final NotificationService notificationService;
    private final VendorService vendorService;
    private final ObjectMapper objectMapper;

    public SubmitPageController(NotificationService notificationService,
                                VendorService vendorService,
                                ObjectMapper objectMapper) {
        this.notificationService = notificationService;
        this.vendorService = vendorService;
        this.objectMapper = objectMapper;
    }

    @GetMapping("/notifications/new")
    public String form(Model model) {
        model.addAttribute("form", sampleForm());
        model.addAttribute("vendors", enabledVendors());
        return "notifications/submit";
    }

    @PostMapping("/notifications/new")
    public String submit(@ModelAttribute SubmitForm form, Model model) {
        model.addAttribute("form", form);
        model.addAttribute("vendors", enabledVendors());
        try {
            NotificationRequest request = toRequest(form);
            NotificationResponse response = notificationService.notify(request);
            model.addAttribute("result", response);
        } catch (RuntimeException ex) {
            model.addAttribute("error", ex.getMessage());
        }
        return "notifications/submit";
    }

    private static SubmitForm sampleForm() {
        SubmitForm form = new SubmitForm();
        form.setRequestId("req-" + UUID.randomUUID().toString().substring(0, 8));
        form.setEventType("PAYMENT_SUCCESS");
        form.setPriority(1);
        form.setPayloadJson("{\"userId\":\"1001\",\"status\":\"PAID\"}");
        return form;
    }

    private List<NotificationVendorEntity> enabledVendors() {
        return vendorService.list().stream().filter(NotificationVendorEntity::isEnabled).toList();
    }

    private NotificationRequest toRequest(SubmitForm form) {
        NotificationRequest request = new NotificationRequest();
        request.setRequestId(form.getRequestId());
        request.setTargetSystem(form.getTargetSystem());
        request.setEventType(form.getEventType());
        request.setPriority(form.getPriority());
        request.setPayload(readMap(form.getPayloadJson(), "payload"));
        if (form.getHeadersJson() != null && !form.getHeadersJson().isBlank()) {
            request.setHeaders(readStringMap(form.getHeadersJson()));
        }
        return request;
    }

    private Map<String, Object> readMap(String json, String field) {
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception ex) {
            throw new IllegalArgumentException(field + " is not valid JSON");
        }
    }

    private Map<String, String> readStringMap(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception ex) {
            throw new IllegalArgumentException("headers is not valid JSON");
        }
    }
}

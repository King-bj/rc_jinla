package com.notifyhub.web;

import com.notifyhub.acl.VendorHttpRequest;
import com.notifyhub.api.dto.NotificationResponse;
import com.notifyhub.domain.TaskStatus;
import com.notifyhub.entity.NotificationTaskEntity;
import com.notifyhub.entity.NotificationVendorEntity;
import com.notifyhub.service.NotificationService;
import com.notifyhub.service.VendorService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({VendorPageController.class, TaskPageController.class, SubmitPageController.class})
class OpsPageTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private VendorService vendorService;

    @MockBean
    private NotificationService notificationService;

    @Test
    void vendorList_renders() throws Exception {
        NotificationVendorEntity vendor = new NotificationVendorEntity();
        vendor.setCode("CRM");
        vendor.setName("客户关系管理系统");
        vendor.setEnabled(true);
        vendor.setMethod("POST");
        vendor.setUrlTemplate("http://crm.test/contacts/update");
        when(vendorService.list()).thenReturn(List.of(vendor));

        mockMvc.perform(get("/vendors"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("CRM")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("供应商")));
    }

    @Test
    void vendorPreview_showsRenderedBody() throws Exception {
        when(vendorService.exists("BILLING")).thenReturn(false);
        when(vendorService.preview(any())).thenReturn(new VendorHttpRequest(
                "https://billing.test/hook",
                "POST",
                Map.of("Content-Type", "application/json"),
                "{\"event\":\"PAY\"}"));

        mockMvc.perform(post("/vendors/preview")
                        .param("code", "BILLING")
                        .param("name", "账单")
                        .param("enabled", "true")
                        .param("method", "POST")
                        .param("urlTemplate", "https://billing.test/hook")
                        .param("bodyTemplate", "{\"event\":\"{{eventType}}\"}")
                        .param("sampleEventType", "PAY")
                        .param("sampleRequestId", "req-1")
                        .param("samplePayload", "{\"a\":1}"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("预览结果")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("https://billing.test/hook")));
    }

    @Test
    void taskList_andReplayForm() throws Exception {
        NotificationTaskEntity task = task(9L, TaskStatus.DEAD);
        when(notificationService.search(isNull(), isNull(), isNull(), eq(0)))
                .thenReturn(new PageImpl<>(List.of(task)));
        when(notificationService.get(9L)).thenReturn(task);

        when(vendorService.list()).thenReturn(List.of());

        mockMvc.perform(get("/tasks"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("req-9")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("死信")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("最近错误")));

        mockMvc.perform(get("/tasks/9"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("重放")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("死信")));

        mockMvc.perform(post("/tasks/9/replay"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/tasks/9"))
                .andExpect(flash().attribute("message", "已放回队列"));
        verify(notificationService).replay(9L);
    }

    @Test
    void replay_nonDead_showsError() throws Exception {
        doThrow(new IllegalStateException("only DEAD tasks can be replayed"))
                .when(notificationService).replay(3L);

        mockMvc.perform(post("/tasks/3/replay"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("error", "only DEAD tasks can be replayed"));
    }

    @Test
    void submitPage_rendersVendors() throws Exception {
        NotificationVendorEntity vendor = new NotificationVendorEntity();
        vendor.setCode("AD");
        vendor.setName("广告投放系统");
        vendor.setEnabled(true);
        when(vendorService.list()).thenReturn(List.of(vendor));
        when(notificationService.notify(any())).thenReturn(new NotificationResponse(1L, "req-1", "PENDING", "ACCEPTED"));

        mockMvc.perform(get("/notifications/new"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("手工提交")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("AD")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("PAYMENT_SUCCESS")));

        mockMvc.perform(post("/notifications/new")
                        .param("requestId", "req-1")
                        .param("targetSystem", "AD")
                        .param("eventType", "CLICK")
                        .param("payloadJson", "{\"clickId\":\"c1\"}"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("已受理")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("/tasks/1")));
    }

    private NotificationTaskEntity task(Long id, TaskStatus status) {
        LocalDateTime now = LocalDateTime.of(2026, 10, 19, 12, 0);
        NotificationTaskEntity task = new NotificationTaskEntity();
        task.setId(id);
        task.setRequestId("req-" + id);
        task.setTargetSystem("CRM");
        task.setEventType("PAY");
        task.setStatus(status);
        task.setRetryCount(5);
        task.setPayload("{\"userId\":\"1\"}");
        task.setCreatedAt(now);
        task.setUpdatedAt(now);
        return task;
    }
}

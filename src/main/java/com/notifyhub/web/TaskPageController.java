package com.notifyhub.web;

import com.notifyhub.domain.TaskStatus;
import com.notifyhub.entity.NotificationTaskEntity;
import com.notifyhub.service.NotificationService;
import com.notifyhub.service.VendorService;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** 任务列表、详情和死信重放。 */
@Controller
public class TaskPageController {

    private final NotificationService notificationService;
    private final VendorService vendorService;

    public TaskPageController(NotificationService notificationService, VendorService vendorService) {
        this.notificationService = notificationService;
        this.vendorService = vendorService;
    }

    @GetMapping("/")
    public String home() {
        return "redirect:/tasks";
    }

    @GetMapping("/tasks")
    public String list(@RequestParam(required = false) String status,
                       @RequestParam(required = false) String vendor,
                       @RequestParam(required = false) String requestId,
                       @RequestParam(defaultValue = "0") int page,
                       Model model) {
        model.addAttribute("status", status);
        model.addAttribute("vendor", vendor);
        model.addAttribute("requestId", requestId);
        model.addAttribute("vendors", vendorService.list());
        try {
            model.addAttribute("tasks", notificationService.search(status, vendor, requestId, page));
        } catch (IllegalArgumentException ex) {
            model.addAttribute("error", ex.getMessage());
            model.addAttribute("tasks", Page.empty());
        }
        return "tasks/list";
    }

    @GetMapping("/tasks/{id}")
    public String detail(@PathVariable Long id, Model model) {
        NotificationTaskEntity task = notificationService.get(id);
        model.addAttribute("task", task);
        if (task.getStatus() == TaskStatus.PENDING || task.getStatus() == TaskStatus.FAILED) {
            model.addAttribute("refreshSeconds", 3);
        }
        return "tasks/detail";
    }

    @PostMapping("/tasks/{id}/replay")
    public String replay(@PathVariable Long id, RedirectAttributes redirect) {
        try {
            notificationService.replay(id);
            redirect.addFlashAttribute("message", "已放回队列");
        } catch (IllegalStateException ex) {
            redirect.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/tasks/" + id;
    }
}

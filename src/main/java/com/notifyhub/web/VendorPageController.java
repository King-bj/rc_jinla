package com.notifyhub.web;

import com.notifyhub.acl.VendorHttpRequest;
import com.notifyhub.service.VendorService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

/** 供应商配置页：列表、新建、修改、报文预览。 */
@Controller
public class VendorPageController {

    private final VendorService vendorService;

    public VendorPageController(VendorService vendorService) {
        this.vendorService = vendorService;
    }

    @GetMapping("/vendors")
    public String list(Model model) {
        model.addAttribute("vendors", vendorService.list());
        return "vendors/list";
    }

    @GetMapping("/vendors/new")
    public String createForm(Model model) {
        model.addAttribute("form", new VendorForm());
        model.addAttribute("creating", true);
        return "vendors/form";
    }

    @PostMapping("/vendors")
    public String create(@ModelAttribute VendorForm form, Model model) {
        try {
            vendorService.create(form);
            return "redirect:/vendors/" + form.getCode();
        } catch (IllegalArgumentException ex) {
            return formWithError(form, true, ex.getMessage(), model);
        }
    }

    @GetMapping("/vendors/{code}")
    public String editForm(@PathVariable String code, Model model) {
        model.addAttribute("form", VendorService.toForm(vendorService.get(code)));
        model.addAttribute("creating", false);
        return "vendors/form";
    }

    @PostMapping("/vendors/{code}")
    public String update(@PathVariable String code, @ModelAttribute VendorForm form, Model model) {
        try {
            vendorService.update(code, form);
            return "redirect:/vendors/" + code;
        } catch (IllegalArgumentException ex) {
            form.setCode(code);
            return formWithError(form, false, ex.getMessage(), model);
        }
    }

    @PostMapping("/vendors/preview")
    public String preview(@ModelAttribute VendorForm form, Model model) {
        model.addAttribute("form", form);
        model.addAttribute("creating", !vendorService.exists(form.getCode()));
        try {
            VendorHttpRequest rendered = vendorService.preview(form);
            model.addAttribute("preview", rendered);
        } catch (RuntimeException ex) {
            model.addAttribute("error", ex.getMessage());
        }
        return "vendors/form";
    }

    private String formWithError(VendorForm form, boolean creating, String error, Model model) {
        model.addAttribute("form", form);
        model.addAttribute("creating", creating);
        model.addAttribute("error", error);
        return "vendors/form";
    }
}

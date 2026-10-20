package com.notifyhub.web;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** 本机模拟供应商。任意 /mock/** 的写请求都返回成功，供手工提交走通投递。 */
@RestController
public class LocalMockController {

    @RequestMapping(path = "/mock/**", method = {RequestMethod.POST, RequestMethod.PUT, RequestMethod.PATCH})
    public Map<String, Boolean> accept() {
        return Map.of("accepted", true);
    }
}

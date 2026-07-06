package com.demo.order.controller;

import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 演示 @RefreshScope 配置热刷新
 * 在 Nacos 创建 order-service.yaml 配置后，改值无需重启
 */
@RefreshScope
@RestController
public class ConfigDemoController {

    @org.springframework.beans.factory.annotation.Value("${order.timeout:3000}")
    private int timeout;

    @org.springframework.beans.factory.annotation.Value("${order.feature.send-sms:true}")
    private boolean sendSms;

    @GetMapping("/config/demo")
    public Map<String, Object> demo() {
        return Map.of(
                "timeout", timeout + "ms",
                "sendSms", sendSms,
                "tip", "去 Nacos 控制台修改 order-service.yaml，刷新本页看值变化"
        );
    }
}

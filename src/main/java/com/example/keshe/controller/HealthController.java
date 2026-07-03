package com.example.keshe.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.RuntimeMXBean;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 健康检查和系统信息接口。
 * 用于阿里云 SLB/ALB 健康探测和运维监控。
 */
@RestController
public class HealthController {

    private final Environment environment;
    private final Instant startTime = Instant.now();

    @Value("${server.port:8080}")
    private String serverPort;

    public HealthController(Environment environment) {
        this.environment = environment;
    }

    /**
     * 健康检查端点 — 阿里云 SLB/ALB 可配置此地址作为健康探测
     * GET /health
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("status", "UP");
        result.put("service", "smart-home-backend");
        result.put("version", "1.0.0");
        result.put("timestamp", Instant.now().toString());

        // 运行时间
        RuntimeMXBean runtime = ManagementFactory.getRuntimeMXBean();
        long uptimeMs = runtime.getUptime();
        Duration uptime = Duration.ofMillis(uptimeMs);
        result.put("uptime", String.format("%dh %dm %ds",
                uptime.toHours(), uptime.toMinutes() % 60, uptime.toSeconds() % 60));

        // 内存使用
        MemoryMXBean memory = ManagementFactory.getMemoryMXBean();
        long heapUsed = memory.getHeapMemoryUsage().getUsed() / (1024 * 1024);
        long heapMax = memory.getHeapMemoryUsage().getMax() / (1024 * 1024);
        result.put("memory", heapUsed + "MB / " + heapMax + "MB");

        // 当前激活的 profile
        String[] profiles = environment.getActiveProfiles();
        result.put("activeProfiles", profiles.length > 0 ? Arrays.asList(profiles) : "default");

        return ResponseEntity.ok(result);
    }

    /**
     * 详细环境信息（仅开发和生产环境可见，云端建议通过安全组限制访问）
     * GET /info
     */
    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> info() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("service", "Smart Home Backend");
        result.put("version", "1.0.0");
        result.put("description", "智能家居后台管理系统 — 支持 Yeelight 局域网控制 + 涂鸦云平台");
        result.put("port", serverPort);

        String[] profiles = environment.getActiveProfiles();
        result.put("activeProfiles", profiles.length > 0 ? Arrays.asList(profiles) : "default");

        // 功能特性
        Map<String, Boolean> features = new LinkedHashMap<>();
        features.put("yeelight_lan", true);
        features.put("tuya_cloud", true);
        features.put("websocket", true);
        result.put("features", features);

        result.put("startTime", startTime.toString());

        return ResponseEntity.ok(result);
    }
}

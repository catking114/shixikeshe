package com.example.keshe.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final DeviceWebSocketHandler deviceWebSocketHandler;

    @Value("${app.cors.allowed-origins:*}")
    private String allowedOrigins;

    public WebSocketConfig(DeviceWebSocketHandler deviceWebSocketHandler) {
        this.deviceWebSocketHandler = deviceWebSocketHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // allowedOrigins 从配置文件读取，部署到阿里云时修改 app.cors.allowed-origins 即可
        registry.addHandler(deviceWebSocketHandler, "/ws/device")
                .setAllowedOrigins(allowedOrigins.split(","));
    }
}

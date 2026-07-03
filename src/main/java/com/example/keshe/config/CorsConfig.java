package com.example.keshe.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

/**
 * 全局 CORS 跨域配置。
 * 
 * 当前端和后端部署在不同域名时（如前端部署在阿里云 OSS/CDN，后端部署在 ECS），
 * 需要配置 CORS 允许跨域请求。配置项通过 application.properties 管理，
 * 部署到阿里云时只需修改配置文件中的 app.cors.* 即可。
 */
@Configuration
public class CorsConfig {

    @Value("${app.cors.allowed-origins:*}")
    private String allowedOrigins;

    @Value("${app.cors.allowed-methods:GET,POST,PUT,DELETE,OPTIONS}")
    private String allowedMethods;

    @Value("${app.cors.allowed-headers:*}")
    private String allowedHeaders;

    @Value("${app.cors.allow-credentials:false}")
    private boolean allowCredentials;

    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();

        // 解析逗号分隔的 origins
        for (String origin : allowedOrigins.split(",")) {
            config.addAllowedOrigin(origin.trim());
        }
        for (String method : allowedMethods.split(",")) {
            config.addAllowedMethod(method.trim());
        }
        for (String header : allowedHeaders.split(",")) {
            config.addAllowedHeader(header.trim());
        }

        config.setAllowCredentials(allowCredentials);
        config.setMaxAge(3600L); // 预检请求缓存 1 小时

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        source.registerCorsConfiguration("/ws/**", config);

        return new CorsFilter(source);
    }
}

package com.sky.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.server.standard.ServerEndpointExporter;

/**
 * WebSocket配置类
 * 用于启用WebSocket支持
 */
@Configuration
public class WebSocketConfig {
    
    /**
     * 注入ServerEndpointExporter
     * 这个Bean会自动注册使用@ServerEndpoint注解声明的WebSocket endpoint
     */
    @Bean
    public ServerEndpointExporter serverEndpointExporter() {
        return new ServerEndpointExporter();
    }
}

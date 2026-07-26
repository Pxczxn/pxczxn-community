package com.mars.admin.websocket;

import com.mars.system.config.PxczxnCorsProperties;
import com.mars.websocket.WebSocketHandshakeInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * WebSocket配置
 * 注册业务WebSocket处理器，容器配置由 mars-websocket 模块提供
 */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final MessageWebSocketHandler messageWebSocketHandler;
    private final SshWebSocketHandler sshWebSocketHandler;
    private final WebSocketHandshakeInterceptor handshakeInterceptor;
    private final PxczxnCorsProperties corsProperties;

    public WebSocketConfig(MessageWebSocketHandler messageWebSocketHandler,
                          SshWebSocketHandler sshWebSocketHandler,
                          WebSocketHandshakeInterceptor handshakeInterceptor,
                          PxczxnCorsProperties corsProperties) {
        this.messageWebSocketHandler = messageWebSocketHandler;
        this.sshWebSocketHandler = sshWebSocketHandler;
        this.handshakeInterceptor = handshakeInterceptor;
        this.corsProperties = corsProperties;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // 消息 WebSocket
        registry.addHandler(messageWebSocketHandler, "/ws/message")
                .addInterceptors(handshakeInterceptor)
                .setAllowedOrigins(corsProperties.validatedAllowedOriginsArray());

        // SSH 终端 WebSocket
        registry.addHandler(sshWebSocketHandler, "/ws/ssh")
                .addInterceptors(handshakeInterceptor)
                .setAllowedOrigins(corsProperties.validatedAllowedOriginsArray());
    }
}

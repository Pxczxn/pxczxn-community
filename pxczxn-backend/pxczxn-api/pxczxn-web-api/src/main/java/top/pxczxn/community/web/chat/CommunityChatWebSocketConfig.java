package top.pxczxn.community.web.chat;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import top.pxczxn.platform.system.config.PxczxnCorsProperties;
import top.pxczxn.platform.websocket.CommunityWebSocketHandshakeInterceptor;

@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class CommunityChatWebSocketConfig implements WebSocketConfigurer {

    private final CommunityChatWebSocketHandler handler;
    private final CommunityWebSocketHandshakeInterceptor handshakeInterceptor;
    private final PxczxnCorsProperties corsProperties;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(handler, "/ws/community-chat")
                .addInterceptors(handshakeInterceptor)
                .setAllowedOrigins(corsProperties.validatedAllowedOriginsArray());
    }
}

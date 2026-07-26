package com.mars.websocket;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

/**
 * Authenticates WebSocket handshakes with a short-lived, single-use ticket.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketHandshakeInterceptor implements HandshakeInterceptor {

    private final WebSocketTicketService ticketService;

    @Override
    public boolean beforeHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Map<String, Object> attributes
    ) {
        try {
            if (request instanceof ServletServerHttpRequest servletRequest) {
                String ticket = servletRequest.getServletRequest().getParameter("ticket");
                Long userId = ticketService.consume(ticket);
                if (userId != null) {
                    attributes.put("userId", userId);
                    log.info("WebSocket handshake accepted for userId={}", userId);
                    return true;
                }
            }
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            log.warn("WebSocket handshake rejected: missing, expired, or reused ticket");
            return false;
        } catch (Exception exception) {
            response.setStatusCode(HttpStatus.SERVICE_UNAVAILABLE);
            log.error("WebSocket handshake authentication failed", exception);
            return false;
        }
    }

    @Override
    public void afterHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Exception exception
    ) {
        // No authentication data is retained after the one-time ticket is consumed.
    }
}

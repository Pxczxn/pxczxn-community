package top.pxczxn.community.web.chat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import top.pxczxn.community.chat.application.CommunityChatMessageView;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Community-only socket channel. Messages are persisted through HTTP; this channel only delivers
 * post-commit events and heartbeat responses.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CommunityChatWebSocketHandler extends TextWebSocketHandler {

    private static final Map<Long, Set<WebSocketSession>> ONLINE_SESSIONS =
            new ConcurrentHashMap<>();

    private final ObjectMapper objectMapper;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        Long userId = userId(session);
        if (userId == null) {
            return;
        }
        ONLINE_SESSIONS.computeIfAbsent(userId, ignored -> ConcurrentHashMap.newKeySet())
                .add(session);
        send(session, event("connected", null));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        try {
            JsonNode payload = objectMapper.readTree(message.getPayload());
            String type = payload.path("type").asText();
            if ("ping".equals(type)) {
                send(session, event("pong", null));
                return;
            }
            send(session, event("error", "Messages must be sent through the HTTP API"));
        } catch (IOException exception) {
            send(session, event("error", "Invalid WebSocket payload"));
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        remove(session);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.debug("Community chat WebSocket transport error", exception);
        remove(session);
    }

    public void sendChatEvent(Long userId, CommunityChatMessageView message) {
        try {
            ObjectNode payload = objectMapper.valueToTree(message);
            payload.put("type", "chat");
            payload.put("time", System.currentTimeMillis());
            sendToUser(userId, objectMapper.writeValueAsString(payload));
        } catch (IOException exception) {
            log.warn("Unable to serialize community chat event for userId={}", userId, exception);
        }
    }

    private void sendToUser(Long userId, String payload) {
        Set<WebSocketSession> sessions = ONLINE_SESSIONS.get(userId);
        if (sessions == null) {
            return;
        }
        sessions.removeIf(session -> !session.isOpen());
        sessions.forEach(session -> send(session, payload));
    }

    private String event(String type, String content) {
        ObjectNode event = objectMapper.createObjectNode().put("type", type);
        if (content != null) {
            event.put("content", content);
        }
        try {
            return objectMapper.writeValueAsString(event);
        } catch (IOException exception) {
            return "{\"type\":\"error\"}";
        }
    }

    private void send(WebSocketSession session, String payload) {
        try {
            synchronized (session) {
                if (session.isOpen()) {
                    session.sendMessage(new TextMessage(payload));
                }
            }
        } catch (IOException exception) {
            log.debug("Unable to deliver community chat WebSocket event", exception);
        }
    }

    private void remove(WebSocketSession session) {
        Long userId = userId(session);
        if (userId == null) {
            return;
        }
        Set<WebSocketSession> sessions = ONLINE_SESSIONS.get(userId);
        if (sessions == null) {
            return;
        }
        sessions.remove(session);
        if (sessions.isEmpty()) {
            ONLINE_SESSIONS.remove(userId, sessions);
        }
    }

    private static Long userId(WebSocketSession session) {
        return (Long) session.getAttributes().get("userId");
    }
}

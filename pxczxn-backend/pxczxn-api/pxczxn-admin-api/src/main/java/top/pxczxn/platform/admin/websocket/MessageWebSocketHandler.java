package top.pxczxn.platform.admin.websocket;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket消息处理器
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MessageWebSocketHandler extends TextWebSocketHandler {

    private final ObjectMapper objectMapper;

    /**
     * 在线用户会话。一个账号允许多个浏览器标签和设备同时在线。
     */
    private static final Map<Long, Set<WebSocketSession>> ONLINE_SESSIONS =
            new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        Long userId = getUserId(session);
        if (userId != null) {
            ONLINE_SESSIONS.computeIfAbsent(
                    userId,
                    ignored -> ConcurrentHashMap.newKeySet()).add(session);
            log.info("WebSocket连接建立，用户ID: {}，当前在线: {}", userId, ONLINE_SESSIONS.size());

            // 发送连接成功消息
            sendMessage(session, createMessage("connected", "连接成功"));
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        try {
            Long userId = getUserId(session);
            String payload = message.getPayload();
            log.debug("收到消息，用户: {}，内容: {}", userId, payload);

            JsonNode jsonNode = objectMapper.readTree(payload);
            String type = jsonNode.has("type") ? jsonNode.get("type").asText() : "";

            switch (type) {
                case "ping" -> sendMessage(session, createMessage("pong", "pong"));
                default -> sendMessage(
                        session,
                        createMessage("error", "不支持通过WebSocket直接发送业务消息"));
            }
        } catch (Exception e) {
            log.error("处理WebSocket消息失败", e);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Long userId = getUserId(session);
        if (userId != null) {
            removeSession(userId, session);
            log.info("WebSocket连接关闭，用户ID: {}，当前在线: {}", userId, ONLINE_SESSIONS.size());
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.error("WebSocket传输错误", exception);
        Long userId = getUserId(session);
        if (userId != null) {
            removeSession(userId, session);
        }
    }

    /**
     * 发送消息给指定用户
     */
    public void sendToUser(Long userId, String message) {
        Set<WebSocketSession> sessions = ONLINE_SESSIONS.get(userId);
        if (sessions == null) {
            return;
        }
        sessions.removeIf(session -> !session.isOpen());
        sessions.forEach(session -> sendMessage(session, message));
    }

    /**
     * 安全序列化并投递业务事件。
     */
    public void sendEvent(Long userId, String type, Object payload) {
        try {
            ObjectNode event = objectMapper.valueToTree(payload);
            event.put("type", type);
            event.put("time", System.currentTimeMillis());
            sendToUser(userId, objectMapper.writeValueAsString(event));
        } catch (Exception exception) {
            log.error("序列化WebSocket业务事件失败, type={}", type, exception);
        }
    }

    /**
     * 广播消息给所有在线用户
     */
    public void broadcastMessage(String message) {
        ONLINE_SESSIONS.keySet().forEach(userId -> sendToUser(userId, message));
    }

    /**
     * 发送系统通知
     */
    public void sendNotice(Long userId, String title, String content) {
        try {
            Map<String, Object> notice = Map.of(
                    "type", "notice",
                    "title", title,
                    "content", content,
                    "time", System.currentTimeMillis()
            );
            String message = objectMapper.writeValueAsString(notice);

            if (userId == null || userId == 0) {
                // 广播给所有用户
                broadcastMessage(message);
            } else {
                // 发送给指定用户
                sendToUser(userId, message);
            }
        } catch (Exception e) {
            log.error("发送通知失败", e);
        }
    }

    /**
     * 发送未读消息数量
     */
    public void sendUnreadCount(Long userId, int noticeCount, int chatCount) {
        try {
            Map<String, Object> data = Map.of(
                    "type", "unread",
                    "noticeCount", noticeCount,
                    "chatCount", chatCount
            );
            sendToUser(userId, objectMapper.writeValueAsString(data));
        } catch (Exception e) {
            log.error("发送未读数量失败", e);
        }
    }

    /**
     * 获取在线用户数
     */
    public int getOnlineCount() {
        return ONLINE_SESSIONS.size();
    }

    /**
     * 检查用户是否在线
     */
    public boolean isOnline(Long userId) {
        Set<WebSocketSession> sessions = ONLINE_SESSIONS.get(userId);
        return sessions != null && sessions.stream().anyMatch(WebSocketSession::isOpen);
    }

    /**
     * 获取用户ID
     */
    private Long getUserId(WebSocketSession session) {
        return (Long) session.getAttributes().get("userId");
    }

    /**
     * 发送消息
     */
    private void sendMessage(WebSocketSession session, String message) {
        try {
            synchronized (session) {
                if (session.isOpen()) {
                    session.sendMessage(new TextMessage(message));
                }
            }
        } catch (IOException e) {
            log.error("发送WebSocket消息失败", e);
        }
    }

    /**
     * 创建消息
     */
    private String createMessage(String type, String content) {
        try {
            return objectMapper.writeValueAsString(Map.of("type", type, "content", content));
        } catch (Exception e) {
            return "{\"type\":\"error\",\"content\":\"消息序列化失败\"}";
        }
    }

    private void removeSession(Long userId, WebSocketSession session) {
        Set<WebSocketSession> sessions = ONLINE_SESSIONS.get(userId);
        if (sessions == null) {
            return;
        }
        sessions.removeIf(existing ->
                existing == session || existing.getId().equals(session.getId()));
        if (sessions.isEmpty()) {
            ONLINE_SESSIONS.remove(userId, sessions);
        }
    }
}

package com.mars.websocket;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@ConfigurationProperties(prefix = "pxczxn.websocket.ticket")
public class WebSocketTicketProperties {

    private Duration ttl = Duration.ofSeconds(30);
    private boolean requireRedis;

    public Duration getTtl() {
        return ttl;
    }

    public void setTtl(Duration ttl) {
        if (ttl == null || ttl.isNegative() || ttl.isZero()
                || ttl.compareTo(Duration.ofMinutes(2)) > 0) {
            throw new IllegalArgumentException(
                    "WebSocket ticket TTL must be between 1 second and 2 minutes");
        }
        this.ttl = ttl;
    }

    public boolean isRequireRedis() {
        return requireRedis;
    }

    public void setRequireRedis(boolean requireRedis) {
        this.requireRedis = requireRedis;
    }
}

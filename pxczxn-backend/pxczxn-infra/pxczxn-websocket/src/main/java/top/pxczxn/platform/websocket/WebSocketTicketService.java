package top.pxczxn.platform.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Issues opaque, short-lived, single-use WebSocket authentication tickets.
 */
@Slf4j
@Service
public class WebSocketTicketService {

    private static final String REDIS_KEY_PREFIX = "pxczxn:websocket:ticket:";
    private static final int TICKET_BYTES = 32;

    private final StringRedisTemplate redisTemplate;
    private final WebSocketTicketProperties properties;
    private final SecureRandom secureRandom;
    private final Map<String, LocalTicket> localTickets = new ConcurrentHashMap<>();
    private volatile boolean redisAvailable = true;

    @Autowired
    public WebSocketTicketService(
            StringRedisTemplate redisTemplate,
            WebSocketTicketProperties properties
    ) {
        this(redisTemplate, properties, new SecureRandom());
    }

    WebSocketTicketService(
            StringRedisTemplate redisTemplate,
            WebSocketTicketProperties properties,
            SecureRandom secureRandom
    ) {
        this.redisTemplate = redisTemplate;
        this.properties = properties;
        this.secureRandom = secureRandom;
    }

    public IssuedTicket issue(Long userId) {
        byte[] randomBytes = new byte[TICKET_BYTES];
        secureRandom.nextBytes(randomBytes);
        String ticket = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
        String storageKey = storageKey(ticket);

        if (redisAvailable) {
            try {
                redisTemplate.opsForValue().set(
                        storageKey,
                        userId.toString(),
                        properties.getTtl()
                );
                return new IssuedTicket(ticket, properties.getTtl().toSeconds());
            } catch (RuntimeException exception) {
                handleRedisFailure(exception);
            }
        }

        localTickets.put(
                storageKey,
                new LocalTicket(userId, Instant.now().plus(properties.getTtl()))
        );
        removeExpiredLocalTickets();
        return new IssuedTicket(ticket, properties.getTtl().toSeconds());
    }

    public Long consume(String ticket) {
        if (!StringUtils.hasText(ticket)) {
            return null;
        }
        String storageKey = storageKey(ticket.trim());

        if (redisAvailable) {
            try {
                String userId = redisTemplate.opsForValue().getAndDelete(storageKey);
                return userId == null ? null : Long.valueOf(userId);
            } catch (RuntimeException exception) {
                handleRedisFailure(exception);
            }
        }

        LocalTicket localTicket = localTickets.remove(storageKey);
        if (localTicket == null || !localTicket.expiresAt().isAfter(Instant.now())) {
            return null;
        }
        return localTicket.userId();
    }

    private void handleRedisFailure(RuntimeException exception) {
        if (properties.isRequireRedis()) {
            throw new IllegalStateException(
                    "Redis is required for WebSocket tickets in this environment",
                    exception
            );
        }
        if (redisAvailable) {
            redisAvailable = false;
            log.warn(
                    "Redis is unavailable; WebSocket tickets are using the single-instance memory fallback"
            );
        }
    }

    private void removeExpiredLocalTickets() {
        Instant now = Instant.now();
        localTickets.entrySet().removeIf(entry -> !entry.getValue().expiresAt().isAfter(now));
    }

    private String storageKey(String ticket) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return REDIS_KEY_PREFIX
                    + HexFormat.of().formatHex(digest.digest(ticket.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    public record IssuedTicket(String ticket, long expiresInSeconds) {
    }

    private record LocalTicket(Long userId, Instant expiresAt) {
    }
}

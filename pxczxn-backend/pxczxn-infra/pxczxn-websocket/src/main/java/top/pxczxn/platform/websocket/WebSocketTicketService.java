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
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Issues opaque, short-lived, single-use WebSocket authentication tickets.
 */
@Slf4j
@Service
public class WebSocketTicketService {

    private static final String REDIS_KEY_PREFIX = "pxczxn:websocket:ticket:";
    private static final int TICKET_BYTES = 32;
    private static final Set<String> AUDIENCES = Set.of("ADMIN", "COMMUNITY");

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
        return issue("ADMIN", userId);
    }

    public IssuedTicket issue(String audience, Long userId) {
        if (!AUDIENCES.contains(audience) || userId == null || userId <= 0) {
            throw new IllegalArgumentException("WebSocket ticket audience and user ID are required");
        }
        byte[] randomBytes = new byte[TICKET_BYTES];
        secureRandom.nextBytes(randomBytes);
        String ticket = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
        String storageKey = storageKey(ticket);
        LocalTicket localTicket = new LocalTicket(
                audience,
                userId,
                Instant.now().plus(properties.getTtl())
        );

        if (!properties.isRequireRedis()) {
            localTickets.put(storageKey, localTicket);
            removeExpiredLocalTickets();
        }

        if (redisAvailable) {
            try {
                redisTemplate.opsForValue().set(
                        storageKey,
                        audience + ":" + userId,
                        properties.getTtl()
                );
                return new IssuedTicket(ticket, properties.getTtl().toSeconds());
            } catch (RuntimeException exception) {
                handleRedisFailure(exception);
            }
        }

        localTickets.put(storageKey, localTicket);
        removeExpiredLocalTickets();
        return new IssuedTicket(ticket, properties.getTtl().toSeconds());
    }

    public Long consume(String ticket) {
        return consume("ADMIN", ticket);
    }

    public Long consume(String audience, String ticket) {
        if (!AUDIENCES.contains(audience)) {
            return null;
        }
        TicketPrincipal principal = consumePrincipal(ticket);
        return principal != null && audience.equals(principal.audience())
                ? principal.userId()
                : null;
    }

    public TicketPrincipal consumePrincipal(String ticket) {
        if (!StringUtils.hasText(ticket)) {
            return null;
        }
        String storageKey = storageKey(ticket.trim());

        if (redisAvailable) {
            try {
                String stored = redisTemplate.opsForValue().getAndDelete(storageKey);
                localTickets.remove(storageKey);
                return parsePrincipal(stored);
            } catch (RuntimeException exception) {
                handleRedisFailure(exception);
            }
        }

        LocalTicket localTicket = localTickets.remove(storageKey);
        if (localTicket == null || !localTicket.expiresAt().isAfter(Instant.now())) {
            return null;
        }
        return new TicketPrincipal(localTicket.audience(), localTicket.userId());
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

    private static TicketPrincipal parsePrincipal(String stored) {
        if (!StringUtils.hasText(stored)) {
            return null;
        }
        int separator = stored.indexOf(':');
        if (separator < 1 || separator == stored.length() - 1) {
            return null;
        }
        String audience = stored.substring(0, separator);
        if (!AUDIENCES.contains(audience)) {
            return null;
        }
        try {
            return new TicketPrincipal(
                    audience,
                    Long.valueOf(stored.substring(separator + 1))
            );
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    public record TicketPrincipal(String audience, Long userId) {
    }

    private record LocalTicket(String audience, Long userId, Instant expiresAt) {
    }
}

package top.pxczxn.platform.websocket;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class WebSocketTicketServiceTest {

    @Test
    void memoryFallbackTicketsAreOpaqueAndSingleUse() {
        StringRedisTemplate redisTemplate = unavailableRedis();
        WebSocketTicketProperties properties = properties(false);
        WebSocketTicketService service = new WebSocketTicketService(redisTemplate, properties);

        WebSocketTicketService.IssuedTicket issued = service.issue(42L);

        assertThat(issued.ticket())
                .hasSize(43)
                .matches("[A-Za-z0-9_-]+")
                .isNotEqualTo("42");
        assertThat(issued.expiresInSeconds()).isEqualTo(30);
        assertThat(service.consume(issued.ticket())).isEqualTo(42L);
        assertThat(service.consume(issued.ticket())).isNull();
    }

    @Test
    void productionModeRefusesToIssueTicketsWithoutRedis() {
        WebSocketTicketService service = new WebSocketTicketService(
                unavailableRedis(),
                properties(true)
        );

        assertThatThrownBy(() -> service.issue(42L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Redis is required");
    }

    @SuppressWarnings("unchecked")
    private StringRedisTemplate unavailableRedis() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        doThrow(new IllegalStateException("offline"))
                .when(valueOperations)
                .set(any(String.class), any(String.class), any(Duration.class));
        return redisTemplate;
    }

    private WebSocketTicketProperties properties(boolean requireRedis) {
        WebSocketTicketProperties properties = new WebSocketTicketProperties();
        properties.setTtl(Duration.ofSeconds(30));
        properties.setRequireRedis(requireRedis);
        return properties;
    }
}

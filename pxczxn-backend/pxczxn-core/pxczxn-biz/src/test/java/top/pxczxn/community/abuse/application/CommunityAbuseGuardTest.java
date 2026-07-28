package top.pxczxn.community.abuse.application;

import org.junit.jupiter.api.Test;
import top.pxczxn.community.abuse.model.CommunityAbuseEvent;
import top.pxczxn.community.abuse.model.CommunityAbuseWindow;
import top.pxczxn.community.abuse.persistence.CommunityAbuseEventMapper;
import top.pxczxn.community.abuse.persistence.CommunityAbuseWindowMapper;
import top.pxczxn.platform.common.exception.BusinessException;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CommunityAbuseGuardTest {

    @Test
    void allowsAtThresholdAndRejectsOverThresholdWithAnAuditTrail() {
        CommunityAbuseWindowMapper windows = mock(CommunityAbuseWindowMapper.class);
        CommunityAbuseEventMapper events = mock(CommunityAbuseEventMapper.class);
        when(windows.selectOne(any())).thenReturn(
                window(3, LocalDateTime.now(ZoneOffset.UTC)),
                window(4, LocalDateTime.now(ZoneOffset.UTC))
        );
        CommunityAbuseGuard guard = new CommunityAbuseGuard(windows, events);

        guard.check("USER:7", "COMMENT_CREATE", 3, 60);

        assertThatThrownBy(() -> guard.check("USER:7", "COMMENT_CREATE", 3, 60))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo(429);
        verify(events, times(2)).insert(any(CommunityAbuseEvent.class));
        verify(windows, times(2)).recordAttempt(any(), any(), any(), any(Integer.class));
        verify(windows).update(any(), any());
    }

    @Test
    void resetsAnExpiredWindowAndRecordsTheResetAttempt() {
        CommunityAbuseWindowMapper windows = mock(CommunityAbuseWindowMapper.class);
        CommunityAbuseEventMapper events = mock(CommunityAbuseEventMapper.class);
        when(windows.selectOne(any())).thenReturn(window(1, LocalDateTime.now(ZoneOffset.UTC)));
        CommunityAbuseGuard guard = new CommunityAbuseGuard(windows, events);

        guard.check("USER:7", "COMMENT_CREATE", 1, 60);

        org.mockito.ArgumentCaptor<CommunityAbuseEvent> captured =
                org.mockito.ArgumentCaptor.forClass(CommunityAbuseEvent.class);
        verify(events).insert(captured.capture());
        assertThat(captured.getValue().getDecision()).isEqualTo("ALLOW");
        assertThat(captured.getValue().getAttemptCount()).isEqualTo(1);
        verify(windows).recordAttempt(any(), any(), any(), any(Integer.class));
    }

    @Test
    void keepsActionTypesSeparateAndAuditsTheActionMetadata() {
        CommunityAbuseWindowMapper windows = mock(CommunityAbuseWindowMapper.class);
        CommunityAbuseEventMapper events = mock(CommunityAbuseEventMapper.class);
        when(windows.selectOne(any())).thenReturn(
                window(1, LocalDateTime.now(ZoneOffset.UTC)),
                window(1, LocalDateTime.now(ZoneOffset.UTC))
        );
        CommunityAbuseGuard guard = new CommunityAbuseGuard(windows, events);

        guard.check("USER:7", "LIKE_CREATE", 2, 60);
        guard.check("USER:7", "REPORT_CREATE", 2, 300);

        org.mockito.ArgumentCaptor<CommunityAbuseEvent> captured =
                org.mockito.ArgumentCaptor.forClass(CommunityAbuseEvent.class);
        verify(events, times(2)).insert(captured.capture());
        assertThat(captured.getAllValues())
                .extracting(CommunityAbuseEvent::getActionType)
                .containsExactly("LIKE_CREATE", "REPORT_CREATE");
        assertThat(captured.getAllValues().get(1).getWindowSeconds()).isEqualTo(300);
        verify(windows, times(2)).recordAttempt(any(), any(), any(), any(Integer.class));
    }

    private static CommunityAbuseWindow window(
            int attempts,
            LocalDateTime startedAt
    ) {
        CommunityAbuseWindow row = new CommunityAbuseWindow();
        row.setAttemptCount(attempts);
        row.setRejectedCount(0);
        row.setWindowStartedAt(startedAt);
        return row;
    }
}

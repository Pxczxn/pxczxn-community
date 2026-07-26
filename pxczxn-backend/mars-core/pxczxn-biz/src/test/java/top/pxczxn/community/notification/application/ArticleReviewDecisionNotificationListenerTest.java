package top.pxczxn.community.notification.application;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import top.pxczxn.community.notification.model.CommunityNotification;
import top.pxczxn.community.notification.model.CommunityNotificationRecipient;
import top.pxczxn.community.notification.persistence.CommunityNotificationMapper;
import top.pxczxn.community.notification.persistence.CommunityNotificationRecipientMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ArticleReviewDecisionNotificationListenerTest {

    private CommunityNotificationMapper notificationMapper;
    private CommunityNotificationRecipientMapper recipientMapper;
    private ArticleReviewDecisionNotificationService notificationService;
    private ArticleReviewDecisionNotificationListener listener;

    @BeforeEach
    void setUp() {
        notificationMapper = mock(CommunityNotificationMapper.class);
        recipientMapper = mock(CommunityNotificationRecipientMapper.class);
        notificationService = new ArticleReviewDecisionNotificationService(
                notificationMapper,
                recipientMapper
        );
        listener = new ArticleReviewDecisionNotificationListener(
                notificationService
        );
        when(notificationMapper.selectCount(any())).thenReturn(0L);
        when(notificationMapper.insert(any())).thenReturn(1);
        when(recipientMapper.insert(any())).thenReturn(1);
    }

    @Test
    void createsReviewNotificationAndUnreadRecipient() {
        notificationService.create(event());

        ArgumentCaptor<CommunityNotification> notification =
                ArgumentCaptor.forClass(CommunityNotification.class);
        verify(notificationMapper).insert(notification.capture());
        assertThat(notification.getValue().getNotificationType())
                .isEqualTo("REVIEW");
        assertThat(notification.getValue().getCategory())
                .isEqualTo("REVIEW");
        assertThat(notification.getValue().getImportance())
                .isEqualTo("HIGH");
        assertThat(notification.getValue().getAggregateCount())
                .isEqualTo(1);
        assertThat(notification.getValue().getDeduplicationKey())
                .isEqualTo("article-review:600:REVISION_REQUIRED");
        assertThat(notification.getValue().getContent())
                .contains("请补充来源");

        ArgumentCaptor<CommunityNotificationRecipient> recipient =
                ArgumentCaptor.forClass(
                        CommunityNotificationRecipient.class
                );
        verify(recipientMapper).insert(recipient.capture());
        assertThat(recipient.getValue().getRecipientUserId()).isEqualTo(100L);
        assertThat(recipient.getValue().getStatus()).isEqualTo("UNREAD");
    }

    @Test
    void duplicateDecisionNotificationIsIgnored() {
        when(notificationMapper.selectCount(any())).thenReturn(1L);

        notificationService.create(event());

        verify(notificationMapper, never()).insert(any());
        verify(recipientMapper, never()).insert(any());
    }

    @Test
    void listenerContainsNotificationFailureAfterMainDecisionCommit() {
        ArticleReviewDecisionNotificationService failingService =
                mock(ArticleReviewDecisionNotificationService.class);
        org.mockito.Mockito.doThrow(new IllegalStateException("offline"))
                .when(failingService)
                .create(any());
        ArticleReviewDecisionNotificationListener safeListener =
                new ArticleReviewDecisionNotificationListener(failingService);

        org.assertj.core.api.Assertions.assertThatCode(
                () -> safeListener.onDecision(event())
        ).doesNotThrowAnyException();
    }

    private static ArticleReviewDecisionNotificationEvent event() {
        return new ArticleReviewDecisionNotificationEvent(
                600L,
                300L,
                100L,
                "文章",
                "REVISION_REQUIRED",
                "请补充来源"
        );
    }
}

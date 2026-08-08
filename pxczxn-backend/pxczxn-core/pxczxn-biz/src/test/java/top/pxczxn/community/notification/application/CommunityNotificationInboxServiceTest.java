package top.pxczxn.community.notification.application;

import top.pxczxn.platform.common.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import top.pxczxn.community.article.persistence.ArticleMapper;
import top.pxczxn.community.blog.persistence.BlogMapper;
import top.pxczxn.community.notification.model.CommunityNotification;
import top.pxczxn.community.notification.model.CommunityNotificationRecipient;
import top.pxczxn.community.notification.persistence.CommunityNotificationMapper;
import top.pxczxn.community.notification.persistence.CommunityNotificationRecipientMapper;
import top.pxczxn.community.shared.auth.CommunityAuth;
import top.pxczxn.community.social.application.AccessibleContentTarget;
import top.pxczxn.community.social.application.CommunityContentAccessService;
import top.pxczxn.community.social.application.LikeTargetType;
import top.pxczxn.community.social.persistence.CommunityCommentMapper;
import top.pxczxn.community.social.persistence.CommunityMomentMapper;
import top.pxczxn.community.user.model.CommunityUser;
import top.pxczxn.community.user.persistence.CommunityUserMapper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CommunityNotificationInboxServiceTest {

    private CommunityNotificationMapper notificationMapper;
    private CommunityNotificationRecipientMapper recipientMapper;
    private CommunityUserMapper userMapper;
    private CommunityContentAccessService accessService;
    private CommunityNotificationInboxService service;

    @BeforeEach
    void setUp() {
        notificationMapper = mock(CommunityNotificationMapper.class);
        recipientMapper =
                mock(CommunityNotificationRecipientMapper.class);
        userMapper = mock(CommunityUserMapper.class);
        accessService = mock(CommunityContentAccessService.class);
        CommunityAuth communityAuth = mock(CommunityAuth.class);
        when(communityAuth.getLoginUserId()).thenReturn(10L);
        service = new CommunityNotificationInboxService(
                notificationMapper,
                recipientMapper,
                userMapper,
                mock(BlogMapper.class),
                mock(ArticleMapper.class),
                mock(CommunityCommentMapper.class),
                mock(CommunityMomentMapper.class),
                accessService,
                communityAuth
        );
    }

    @Test
    void pageReturnsVisibleTargetAndSenderWithoutLeakingPayload() {
        CommunityNotificationRecipient recipient = recipient("UNREAD");
        CommunityNotification notification = notification();
        CommunityUser sender = new CommunityUser();
        sender.setId(20L);
        sender.setUsername("sender");
        sender.setDisplayName("发送者");
        sender.setStatus("NORMAL");
        when(recipientMapper.selectInbox(
                10L, "INTERACTION", "UNREAD", 0, 20
        )).thenReturn(List.of(recipient));
        when(recipientMapper.countInbox(
                10L, "INTERACTION", "UNREAD"
        )).thenReturn(1L);
        when(notificationMapper.selectBatchIds(any()))
                .thenReturn(List.of(notification));
        when(userMapper.selectBatchIds(any()))
                .thenReturn(List.of(sender));
        when(accessService.findAccessible(
                LikeTargetType.ARTICLE, 30L
        )).thenReturn(new AccessibleContentTarget(
                LikeTargetType.ARTICLE,
                30L,
                10L,
                40L,
                "文章",
                "摘要",
                null,
                "/blog/30/article",
                1,
                2,
                3
        ));

        NotificationPageView page = service.page(
                "interaction", "unread", 1, 20
        );

        assertThat(page.total()).isEqualTo(1);
        assertThat(page.records()).singleElement().satisfies(view -> {
            assertThat(view.targetAvailable()).isTrue();
            assertThat(view.canonicalPath())
                    .isEqualTo("/blog/30/article");
            assertThat(view.sender().username()).isEqualTo("sender");
            assertThat(view.aggregateCount()).isEqualTo(2);
        });
    }

    @Test
    void unavailableTargetHidesSenderSummaryPathAndIds() {
        CommunityNotificationRecipient recipient = recipient("UNREAD");
        CommunityNotification notification = notification();
        CommunityUser sender = new CommunityUser();
        sender.setId(20L);
        sender.setStatus("NORMAL");
        when(recipientMapper.selectInbox(
                10L, null, null, 0, 20
        )).thenReturn(List.of(recipient));
        when(recipientMapper.countInbox(
                10L, null, null
        )).thenReturn(1L);
        when(notificationMapper.selectBatchIds(any()))
                .thenReturn(List.of(notification));
        when(userMapper.selectBatchIds(any()))
                .thenReturn(List.of(sender));
        when(accessService.findAccessible(
                LikeTargetType.ARTICLE, 30L
        )).thenReturn(null);

        NotificationView view = service.page(
                null, null, 1, 20
        ).records().getFirst();

        assertThat(view.targetAvailable()).isFalse();
        assertThat(view.targetType()).isNull();
        assertThat(view.targetId()).isNull();
        assertThat(view.canonicalPath()).isNull();
        assertThat(view.sender()).isNull();
        assertThat(view.title()).isEqualTo("通知内容已不可用");
    }

    @Test
    void unknownTargetTypeDegradesToUnavailableInsteadOfThrowing() {
        CommunityNotificationRecipient recipient = recipient("UNREAD");
        CommunityNotification notification = notification();
        notification.setTargetType("WHATEVER");
        when(recipientMapper.selectInbox(
                10L, "INTERACTION", "UNREAD", 0, 20
        )).thenReturn(List.of(recipient));
        when(recipientMapper.countInbox(
                10L, "INTERACTION", "UNREAD"
        )).thenReturn(1L);
        when(notificationMapper.selectBatchIds(any()))
                .thenReturn(List.of(notification));
        when(userMapper.selectBatchIds(any()))
                .thenReturn(List.of());

        NotificationView view = service.page(
                "interaction", "unread", 1, 20
        ).records().getFirst();

        assertThat(view.targetAvailable()).isFalse();
        assertThat(view.title()).isEqualTo("通知内容已不可用");
    }

    @Test
    void unreadCountIncludesStableZeroValuedCategories() {
        CommunityNotificationRecipient recipient1 = recipient("UNREAD");
        recipient1.setNotificationId(1L);
        CommunityNotificationRecipient recipient2 = recipient("UNREAD");
        recipient2.setNotificationId(2L);
        CommunityNotificationRecipient recipient3 = recipient("UNREAD");
        recipient3.setNotificationId(3L);

        CommunityNotification notification1 = notification();
        notification1.setId(1L);
        notification1.setCategory("COMMENT");
        notification1.setTargetType("ARTICLE");
        notification1.setTargetId(100L);

        CommunityNotification notification2 = notification();
        notification2.setId(2L);
        notification2.setCategory("COMMENT");
        notification2.setTargetType("ARTICLE");
        notification2.setTargetId(101L);

        CommunityNotification notification3 = notification();
        notification3.setId(3L);
        notification3.setCategory("INTERACTION");
        notification3.setTargetType("ARTICLE");
        notification3.setTargetId(200L);

        when(recipientMapper.selectInbox(10L, null, "UNREAD", 0, 1000))
                .thenReturn(List.of(recipient1, recipient2, recipient3));
        when(notificationMapper.selectBatchIds(any()))
                .thenReturn(List.of(notification1, notification2, notification3));

        // 模拟内容可用
        when(accessService.findAccessible(
                LikeTargetType.ARTICLE, 100L
        )).thenReturn(new AccessibleContentTarget(
                LikeTargetType.ARTICLE, 100L, 10L, 50L,
                "Article", "Summary", null, "/article/100", 1, 2, 3
        ));
        when(accessService.findAccessible(
                LikeTargetType.ARTICLE, 101L
        )).thenReturn(new AccessibleContentTarget(
                LikeTargetType.ARTICLE, 101L, 10L, 51L,
                "Article", "Summary", null, "/article/101", 1, 2, 3
        ));
        when(accessService.findAccessible(
                LikeTargetType.ARTICLE, 200L
        )).thenReturn(new AccessibleContentTarget(
                LikeTargetType.ARTICLE, 200L, 10L, 60L,
                "Article", "Summary", null, "/article/200", 1, 2, 3
        ));

        UnreadNotificationCountView count = service.unreadCount();

        assertThat(count.total()).isEqualTo(3);
        assertThat(count.categories().get("COMMENT")).isEqualTo(2);
        assertThat(count.categories().get("INTERACTION")).isEqualTo(1);
        assertThat(count.categories().get("SYSTEM")).isZero();
    }

    @Test
    void readIsIdempotentAndReturnsRemainingUnreadCount() {
        CommunityNotificationRecipient recipient = recipient("READ");
        recipient.setReadAt(LocalDateTime.of(2026, 7, 26, 1, 0));
        when(recipientMapper.findRelation(1L, 10L))
                .thenReturn(recipient);
        when(recipientMapper.countInbox(10L, null, "UNREAD"))
                .thenReturn(0L);
        when(recipientMapper.countUnreadByCategory(10L))
                .thenReturn(List.of());

        NotificationReadView result = service.read(1L);

        assertThat(result.idempotentReplay()).isTrue();
        assertThat(result.unreadCount()).isZero();
        assertThat(result.status()).isEqualTo("READ");
    }

    @Test
    void invalidFiltersAreRejectedBeforeQueryingInbox() {
        assertThatThrownBy(() ->
                service.page("SECRET", null, 1, 20))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo(400);
        assertThatThrownBy(() ->
                service.page(null, "ARCHIVED", 1, 20))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo(400);
    }

    private static CommunityNotificationRecipient recipient(
            String status
    ) {
        CommunityNotificationRecipient recipient =
                new CommunityNotificationRecipient();
        recipient.setId(2L);
        recipient.setNotificationId(1L);
        recipient.setRecipientUserId(10L);
        recipient.setStatus(status);
        return recipient;
    }

    private static CommunityNotification notification() {
        CommunityNotification notification = new CommunityNotification();
        notification.setId(1L);
        notification.setNotificationType("LIKE");
        notification.setCategory("INTERACTION");
        notification.setImportance("NORMAL");
        notification.setSenderUserId(20L);
        notification.setTitle("有人点赞了你的内容");
        notification.setContent("有人点赞了你发布的内容。");
        notification.setTargetType("ARTICLE");
        notification.setTargetId(30L);
        notification.setAggregateCount(2);
        notification.setCreatedAt(LocalDateTime.of(2026, 7, 26, 0, 0));
        notification.setLastActivityAt(
                LocalDateTime.of(2026, 7, 26, 0, 5)
        );
        return notification;
    }
}

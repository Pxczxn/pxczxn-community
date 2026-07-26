package top.pxczxn.community.notification.application;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import top.pxczxn.community.article.persistence.ArticleMapper;
import top.pxczxn.community.blog.model.Blog;
import top.pxczxn.community.blog.persistence.BlogMapper;
import top.pxczxn.community.notification.model.CommunityNotification;
import top.pxczxn.community.notification.model.CommunityNotificationRecipient;
import top.pxczxn.community.notification.persistence.CommunityNotificationMapper;
import top.pxczxn.community.notification.persistence.CommunityNotificationRecipientMapper;
import top.pxczxn.community.social.model.CommunityFollow;
import top.pxczxn.community.social.model.CommunityMoment;
import top.pxczxn.community.social.persistence.CommunityFollowMapper;
import top.pxczxn.community.social.persistence.CommunityMomentMapper;
import top.pxczxn.community.user.model.CommunityUser;
import top.pxczxn.community.user.persistence.CommunityUserMapper;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CommunityNotificationDispatchServiceTest {

    private CommunityNotificationMapper notificationMapper;
    private CommunityNotificationRecipientMapper recipientMapper;
    private CommunityFollowMapper followMapper;
    private CommunityUserMapper userMapper;
    private BlogMapper blogMapper;
    private ArticleMapper articleMapper;
    private CommunityMomentMapper momentMapper;
    private CommunityNotificationDispatchService service;

    @BeforeEach
    void setUp() {
        notificationMapper = mock(CommunityNotificationMapper.class);
        recipientMapper =
                mock(CommunityNotificationRecipientMapper.class);
        followMapper = mock(CommunityFollowMapper.class);
        userMapper = mock(CommunityUserMapper.class);
        blogMapper = mock(BlogMapper.class);
        articleMapper = mock(ArticleMapper.class);
        momentMapper = mock(CommunityMomentMapper.class);
        service = new CommunityNotificationDispatchService(
                notificationMapper,
                recipientMapper,
                followMapper,
                userMapper,
                blogMapper,
                articleMapper,
                momentMapper
        );
    }

    @Test
    void directEventCreatesUnreadRecipientAndSafeAggregateFields() {
        when(userMapper.selectById(2L)).thenReturn(activeUser(2L));
        when(notificationMapper.findByDeduplicationKey(anyString()))
                .thenReturn(null);
        when(notificationMapper.insert(any())).thenReturn(1);
        when(recipientMapper.findRelation(anyLong(), anyLong()))
                .thenReturn(null);
        when(recipientMapper.insert(any())).thenReturn(1);

        service.createDirect(event(1L, 2L));

        ArgumentCaptor<CommunityNotification> notification =
                ArgumentCaptor.forClass(CommunityNotification.class);
        ArgumentCaptor<CommunityNotificationRecipient> recipient =
                ArgumentCaptor.forClass(
                        CommunityNotificationRecipient.class
                );
        verify(notificationMapper).insert(notification.capture());
        verify(recipientMapper).insert(recipient.capture());
        assertThat(notification.getValue().getCategory())
                .isEqualTo("INTERACTION");
        assertThat(notification.getValue().getAggregateCount())
                .isEqualTo(1);
        assertThat(notification.getValue().getDeduplicationKey())
                .startsWith("like:ARTICLE:99:2:");
        assertThat(recipient.getValue().getStatus())
                .isEqualTo("UNREAD");
        assertThat(recipient.getValue().getRecipientUserId())
                .isEqualTo(2L);
    }

    @Test
    void selfNotificationIsSuppressedBeforeDatabaseAccess() {
        service.createDirect(event(2L, 2L));

        verify(userMapper, never()).selectById(anyLong());
        verify(notificationMapper, never()).insert(any());
        verify(recipientMapper, never()).insert(any());
    }

    @Test
    void repeatedEventAggregatesAndReopensReadRecipient() {
        CommunityNotification existing = new CommunityNotification();
        existing.setId(55L);
        CommunityNotificationRecipient recipient =
                new CommunityNotificationRecipient();
        recipient.setId(66L);
        recipient.setStatus("READ");
        when(userMapper.selectById(2L)).thenReturn(activeUser(2L));
        when(notificationMapper.findByDeduplicationKey(anyString()))
                .thenReturn(existing);
        when(notificationMapper.update(any(), any())).thenReturn(1);
        when(recipientMapper.findRelation(55L, 2L))
                .thenReturn(recipient);
        when(recipientMapper.update(any(), any())).thenReturn(1);

        service.createDirect(event(1L, 2L));

        verify(notificationMapper).update(any(), any());
        verify(recipientMapper).update(any(), any());
        verify(notificationMapper, never()).insert(any());
        verify(recipientMapper, never()).insert(any());
    }

    @Test
    void followerLevelsSuppressMutedAndNonImportantUpdates() {
        Blog blog = new Blog();
        blog.setId(100L);
        blog.setStatus("ACTIVE");
        CommunityMoment moment = new CommunityMoment();
        moment.setId(200L);
        moment.setStatus("PUBLISHED");
        moment.setVisibility("PUBLIC");
        when(blogMapper.selectById(100L)).thenReturn(blog);
        when(momentMapper.selectById(200L)).thenReturn(moment);
        when(followMapper.selectList(any())).thenReturn(List.of(
                follow(10L, "ALL", 0),
                follow(11L, "IMPORTANT", 0),
                follow(12L, "MUTED", 1),
                follow(13L, "IMPORTANT", 1)
        ));
        when(userMapper.selectById(anyLong()))
                .thenAnswer(invocation ->
                        activeUser(invocation.getArgument(0)));
        when(notificationMapper.findByDeduplicationKey(anyString()))
                .thenReturn(null);
        when(notificationMapper.insert(any())).thenReturn(1);
        when(recipientMapper.findRelation(anyLong(), anyLong()))
                .thenReturn(null);
        when(recipientMapper.insert(any())).thenReturn(1);

        service.createMomentPublished(
                new MomentPublishedNotificationEvent(
                        200L, 100L, 1L, "TEXT", "PUBLIC"
                )
        );

        ArgumentCaptor<CommunityNotification> notifications =
                ArgumentCaptor.forClass(CommunityNotification.class);
        verify(notificationMapper, times(2))
                .insert(notifications.capture());
        assertThat(notifications.getAllValues())
                .extracting(CommunityNotification::getImportance)
                .containsExactlyInAnyOrder("NORMAL", "HIGH");
        verify(recipientMapper, times(2)).insert(any());
    }

    private static CommunityNotificationEvent event(
            Long sender,
            Long recipient
    ) {
        return new CommunityNotificationEvent(
                "LIKE",
                "INTERACTION",
                sender,
                recipient,
                "ARTICLE",
                99L,
                "有人点赞了你的内容",
                "有人点赞了你发布的内容。",
                "like:ARTICLE:99",
                "NORMAL"
        );
    }

    private static CommunityUser activeUser(Long id) {
        CommunityUser user = new CommunityUser();
        user.setId(id);
        user.setStatus("NORMAL");
        return user;
    }

    private static CommunityFollow follow(
            Long userId,
            String level,
            int special
    ) {
        CommunityFollow follow = new CommunityFollow();
        follow.setFollowerUserId(userId);
        follow.setNotificationLevel(level);
        follow.setSpecialFollow(special);
        return follow;
    }
}

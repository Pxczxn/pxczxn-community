package top.pxczxn.community.admin.application;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import top.pxczxn.platform.common.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import top.pxczxn.community.article.persistence.ArticleMapper;
import top.pxczxn.community.blog.persistence.BlogMapper;
import top.pxczxn.community.social.model.CommunityComment;
import top.pxczxn.community.social.model.CommunityCommentModerationEvent;
import top.pxczxn.community.social.model.CommunityContentLike;
import top.pxczxn.community.social.model.CommunityMoment;
import top.pxczxn.community.social.model.CommunityMomentModerationEvent;
import top.pxczxn.community.social.persistence.CommunityCommentMapper;
import top.pxczxn.community.social.persistence.CommunityCommentModerationEventMapper;
import top.pxczxn.community.social.persistence.CommunityContentLikeMapper;
import top.pxczxn.community.social.persistence.CommunityFollowMapper;
import top.pxczxn.community.social.persistence.CommunityMomentMapper;
import top.pxczxn.community.social.persistence.CommunityMomentModerationEventMapper;
import top.pxczxn.community.social.persistence.FavoriteItemMapper;
import top.pxczxn.community.user.model.CommunityUser;
import top.pxczxn.community.user.persistence.CommunityUserMapper;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminInteractionGovernanceServiceTest {

    private CommunityCommentMapper commentMapper;
    private CommunityCommentModerationEventMapper commentEventMapper;
    private CommunityMomentMapper momentMapper;
    private CommunityMomentModerationEventMapper momentEventMapper;
    private CommunityContentLikeMapper likeMapper;
    private FavoriteItemMapper favoriteItemMapper;
    private CommunityFollowMapper followMapper;
    private CommunityUserMapper userMapper;
    private BlogMapper blogMapper;
    private ArticleMapper articleMapper;
    private AdminInteractionGovernanceService service;

    @BeforeEach
    void setUp() {
        commentMapper = mock(CommunityCommentMapper.class);
        commentEventMapper =
                mock(CommunityCommentModerationEventMapper.class);
        momentMapper = mock(CommunityMomentMapper.class);
        momentEventMapper =
                mock(CommunityMomentModerationEventMapper.class);
        likeMapper = mock(CommunityContentLikeMapper.class);
        favoriteItemMapper = mock(FavoriteItemMapper.class);
        followMapper = mock(CommunityFollowMapper.class);
        userMapper = mock(CommunityUserMapper.class);
        blogMapper = mock(BlogMapper.class);
        articleMapper = mock(ArticleMapper.class);
        service = new AdminInteractionGovernanceService(
                commentMapper,
                commentEventMapper,
                momentMapper,
                momentEventMapper,
                likeMapper,
                favoriteItemMapper,
                followMapper,
                userMapper,
                blogMapper,
                articleMapper
        );
    }

    @Test
    void approvePendingCommentPublishesAndIncrementsTargetCount() {
        CommunityComment pending = comment(
                100L, "PENDING_REVIEW", 2, null
        );
        CommunityComment updated = comment(
                100L, "PUBLISHED", 3, null
        );
        when(commentMapper.selectById(100L))
                .thenReturn(pending, updated);
        when(commentMapper.update(any(), any())).thenReturn(1);
        when(commentEventMapper.insert(any())).thenReturn(1);
        when(articleMapper.update(any(), any())).thenReturn(1);

        AdminGovernanceResultView result = service.moderateComment(
                100L, 7L, "APPROVE", 2, "人工复核通过"
        );

        assertThat(result.status()).isEqualTo("PUBLISHED");
        assertThat(result.lockVersion()).isEqualTo(3);
        assertThat(result.affectedIds()).containsExactly(100L);
        ArgumentCaptor<CommunityCommentModerationEvent> event =
                ArgumentCaptor.forClass(
                        CommunityCommentModerationEvent.class
                );
        verify(commentEventMapper).insert(event.capture());
        assertThat(event.getValue().getAction())
                .isEqualTo("PLATFORM_APPROVED");
        assertThat(event.getValue().getActorType()).isEqualTo("ADMIN");
        assertThat(event.getValue().getActorAdminId()).isEqualTo(7L);
        verify(articleMapper).update(any(), any());
    }

    @Test
    void takingDownRootCommentCascadesAcrossPublishedThread() {
        CommunityComment root = comment(
                100L, "PUBLISHED", 4, null
        );
        CommunityComment reply = comment(
                101L, "PUBLISHED", 1, 100L
        );
        CommunityComment updated = comment(
                100L, "TAKEN_DOWN", 5, null
        );
        when(commentMapper.selectById(100L))
                .thenReturn(root, updated);
        when(commentMapper.findPublishedThread(100L))
                .thenReturn(List.of(root, reply));
        when(commentMapper.update(any(), any())).thenReturn(1);
        when(commentEventMapper.insert(any())).thenReturn(1);
        when(articleMapper.update(any(), any())).thenReturn(1);

        AdminGovernanceResultView result = service.moderateComment(
                100L, 7L, "TAKE_DOWN", 4, "违反社区规范"
        );

        assertThat(result.status()).isEqualTo("TAKEN_DOWN");
        assertThat(result.affectedCount()).isEqualTo(2);
        assertThat(result.affectedIds()).containsExactly(100L, 101L);
        verify(commentEventMapper, org.mockito.Mockito.times(2))
                .insert(any());
    }

    @Test
    void approvePendingRepostMomentUpdatesSourceAndAuditEvent() {
        CommunityMoment pending = moment(
                200L, "PENDING_REVIEW", 3
        );
        pending.setRepostMomentId(199L);
        when(momentMapper.selectById(200L)).thenReturn(pending);
        when(momentMapper.update(any(), any())).thenReturn(1);
        when(momentEventMapper.insert(any())).thenReturn(1);

        AdminGovernanceResultView result = service.moderateMoment(
                200L, 7L, "APPROVE", 3, null
        );

        assertThat(result.status()).isEqualTo("PUBLISHED");
        assertThat(result.lockVersion()).isEqualTo(4);
        verify(momentMapper, org.mockito.Mockito.times(2))
                .update(any(), any());
        ArgumentCaptor<CommunityMomentModerationEvent> event =
                ArgumentCaptor.forClass(
                        CommunityMomentModerationEvent.class
                );
        verify(momentEventMapper).insert(event.capture());
        assertThat(event.getValue().getAction())
                .isEqualTo("PLATFORM_APPROVED");
        assertThat(event.getValue().getActorAdminId()).isEqualTo(7L);
    }

    @Test
    void destructiveGovernanceRequiresReasonBeforeAnyWrite() {
        CommunityMoment pending = moment(
                200L, "PENDING_REVIEW", 3
        );
        when(momentMapper.selectById(200L)).thenReturn(pending);

        assertThatThrownBy(() -> service.moderateMoment(
                200L, 7L, "REJECT", 3, " "
        ))
                .isInstanceOf(BusinessException.class)
                .hasMessage("请填写治理原因");

        verify(momentMapper, never()).update(any(), any());
        verify(momentEventMapper, never()).insert(any());
    }

    @Test
    @SuppressWarnings("unchecked")
    void interactionQueryMapsSnowflakeIdsAndActorIdentity() {
        CommunityContentLike like = new CommunityContentLike();
        like.setId(9007199254740993L);
        like.setUserId(300L);
        like.setTargetType("MOMENT");
        like.setTargetId(200L);
        like.setCreatedAt(LocalDateTime.now());
        CommunityUser actor = new CommunityUser();
        actor.setId(300L);
        actor.setUsername("eva");
        actor.setDisplayName("Eva");
        when(likeMapper.selectPage(any(), any())).thenAnswer(invocation -> {
            Page<CommunityContentLike> page = invocation.getArgument(0);
            page.setRecords(List.of(like));
            page.setTotal(1);
            return page;
        });
        when(userMapper.selectById(300L)).thenReturn(actor);
        when(momentMapper.selectById(200L)).thenReturn(moment(
                200L, "PUBLISHED", 1
        ));

        var result = service.interactions(
                "LIKE", "MOMENT", 200L, null, 1, 20
        );

        assertThat(result.total()).isEqualTo(1);
        assertThat(result.list()).singleElement().satisfies(row -> {
            assertThat(row.id()).isEqualTo(9007199254740993L);
            assertThat(row.actorUsername()).isEqualTo("eva");
            assertThat(row.targetTitle()).isEqualTo("治理测试动态");
        });
    }

    @Test
    void batchRejectsDuplicateTargets() {
        assertThatThrownBy(() -> service.moderateComments(
                List.of(
                        new AdminGovernanceTarget(100L, 1),
                        new AdminGovernanceTarget(100L, 1)
                ),
                7L,
                "TAKE_DOWN",
                "重复目标"
        ))
                .isInstanceOf(BusinessException.class)
                .hasMessage("治理目标不能重复");
        verify(commentMapper, never()).selectById(anyLong());
    }

    private static CommunityComment comment(
            Long id,
            String status,
            int lockVersion,
            Long rootId
    ) {
        CommunityComment comment = new CommunityComment();
        comment.setId(id);
        comment.setAuthorUserId(300L);
        comment.setTargetType("ARTICLE");
        comment.setTargetId(400L);
        comment.setRootCommentId(rootId);
        comment.setParentCommentId(rootId);
        comment.setContentText("治理测试评论");
        comment.setRenderedHtml("<p>治理测试评论</p>");
        comment.setStatus(status);
        comment.setLikeCount(0L);
        comment.setLockVersion(lockVersion);
        comment.setCreatedAt(LocalDateTime.now());
        return comment;
    }

    private static CommunityMoment moment(
            Long id,
            String status,
            int lockVersion
    ) {
        CommunityMoment moment = new CommunityMoment();
        moment.setId(id);
        moment.setActorUserId(300L);
        moment.setBlogId(500L);
        moment.setMomentType("TEXT");
        moment.setTextContent("治理测试动态");
        moment.setRenderedHtml("<p>治理测试动态</p>");
        moment.setVisibility("PUBLIC");
        moment.setStatus(status);
        moment.setLikeCount(0L);
        moment.setFavoriteCount(0L);
        moment.setCommentCount(0L);
        moment.setRepostCount(0L);
        moment.setLockVersion(lockVersion);
        moment.setCreatedAt(LocalDateTime.now());
        return moment;
    }
}

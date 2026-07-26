package top.pxczxn.community.social.application;

import com.mars.common.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import top.pxczxn.community.blog.model.Blog;
import top.pxczxn.community.blog.persistence.BlogMapper;
import top.pxczxn.community.moderation.application.ArticleKeywordReviewEngine;
import top.pxczxn.community.moderation.application.KeywordReviewOutcome;
import top.pxczxn.community.shared.auth.CommunityAuth;
import top.pxczxn.community.social.model.CommunityContentLike;
import top.pxczxn.community.social.model.CommunityMoment;
import top.pxczxn.community.social.model.FavoriteItem;
import top.pxczxn.community.social.persistence.CommunityContentLikeMapper;
import top.pxczxn.community.social.persistence.CommunityMomentMapper;
import top.pxczxn.community.social.persistence.FavoriteItemMapper;
import top.pxczxn.community.user.model.CommunityUser;
import top.pxczxn.community.user.persistence.CommunityUserMapper;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MomentServiceTest {

    private CommunityMomentMapper momentMapper;
    private CommunityUserMapper userMapper;
    private BlogMapper blogMapper;
    private CommunityContentLikeMapper likeMapper;
    private FavoriteItemMapper favoriteItemMapper;
    private CommunityContentAccessService accessService;
    private MomentContentRenderer renderer;
    private ArticleKeywordReviewEngine reviewEngine;
    private CommunityAuth auth;
    private MomentService service;
    private CommunityUser actor;
    private Blog blog;

    @BeforeEach
    void setUp() {
        momentMapper = mock(CommunityMomentMapper.class);
        userMapper = mock(CommunityUserMapper.class);
        blogMapper = mock(BlogMapper.class);
        likeMapper = mock(CommunityContentLikeMapper.class);
        favoriteItemMapper = mock(FavoriteItemMapper.class);
        accessService = mock(CommunityContentAccessService.class);
        renderer = mock(MomentContentRenderer.class);
        reviewEngine = mock(ArticleKeywordReviewEngine.class);
        auth = mock(CommunityAuth.class);
        service = new MomentService(
                momentMapper,
                userMapper,
                blogMapper,
                likeMapper,
                favoriteItemMapper,
                accessService,
                renderer,
                reviewEngine,
                auth,
                List.of()
        );
        actor = user(100L, 300L);
        blog = blog(300L, 100L);
        when(auth.getLoginUserId()).thenReturn(100L);
        when(auth.getOptionalLoginUserId()).thenReturn(100L);
        when(userMapper.selectById(100L)).thenReturn(actor);
        when(blogMapper.selectById(300L)).thenReturn(blog);
        when(renderer.render(any(), anyBoolean(), anyBoolean()))
                .thenReturn(new RenderedMomentContent(
                        "动态正文", "<p>动态正文</p>"
                ));
        when(reviewEngine.review(isNull(), isNull(), any()))
                .thenReturn(approved());
        when(momentMapper.insert(any())).thenReturn(1);
    }

    @Test
    void publishesTextMomentToPersonalBlog() {
        MomentPublishView result = service.publish(command(
                "TEXT", "动态正文", null, null, null
        ));

        assertThat(result.moment().momentType()).isEqualTo("TEXT");
        assertThat(result.moment().status()).isEqualTo("PUBLISHED");
        assertThat(result.moment().blog().blogId()).isEqualTo(300L);
        assertThat(result.moderationResult()).isEqualTo("AUTO_APPROVED");
        verify(momentMapper).insert(any());
    }

    @Test
    void rejectsUnsafeLinkScheme() {
        assertThatThrownBy(() -> service.publish(command(
                "LINK",
                null,
                "javascript:alert(1)",
                null,
                null
        )))
                .isInstanceOf(BusinessException.class)
                .hasMessage("动态链接必须是有效的 HTTP(S) 地址");
        verify(momentMapper, never()).insert(any());
    }

    @Test
    void quoteRequiresTextAndIncrementsSourceRepostCount() {
        CommunityMoment source = moment(
                10L, 101L, 301L, "TEXT", "PUBLISHED"
        );
        source.setRepostCount(3L);
        when(momentMapper.selectById(10L)).thenReturn(source);
        when(momentMapper.update(isNull(), any())).thenReturn(1);

        MomentPublishView result = service.publish(command(
                "QUOTE",
                "我的观点",
                null,
                null,
                10L
        ));

        assertThat(result.moment().repostSource().momentId())
                .isEqualTo(10L);
        assertThat(result.moment().repostSource().available()).isTrue();
        verify(accessService, times(2)).requireAccessible(
                LikeTargetType.MOMENT, 10L
        );
        verify(momentMapper).update(isNull(), any());
    }

    @Test
    void pureRepostRejectsQuoteText() {
        assertThatThrownBy(() -> service.publish(command(
                "REPOST",
                "不允许的观点",
                null,
                null,
                10L
        )))
                .isInstanceOf(BusinessException.class)
                .hasMessage("纯转发不能填写引用观点");
    }

    @Test
    void reviewKeywordCreatesPendingMomentWithoutSourceIncrement() {
        when(reviewEngine.review(isNull(), isNull(), any()))
                .thenReturn(new KeywordReviewOutcome(
                        KeywordReviewOutcome.Decision.MANUAL_REVIEW,
                        "HIGH",
                        "AUTO_ESCALATED_KEYWORD",
                        "命中规则",
                        List.of(900L)
                ));
        CommunityMoment source = moment(
                10L, 101L, 301L, "TEXT", "PUBLISHED"
        );
        when(momentMapper.selectById(10L)).thenReturn(source);

        MomentPublishView result = service.publish(command(
                "REPOST", null, null, null, 10L
        ));

        assertThat(result.moment().status()).isEqualTo("PENDING_REVIEW");
        verify(momentMapper, never()).update(any(), any());
    }

    @Test
    void articleShareValidatesPublishedArticle() {
        when(accessService.requireAccessible(
                LikeTargetType.ARTICLE, 20L
        )).thenReturn(new AccessibleContentTarget(
                LikeTargetType.ARTICLE,
                20L,
                101L,
                301L,
                "文章",
                "摘要",
                null,
                "/articles/20",
                0,
                0,
                0
        ));

        service.publish(command(
                "ARTICLE_SHARE", "推荐阅读", null, 20L, null
        ));

        verify(accessService, times(2)).requireAccessible(
                LikeTargetType.ARTICLE, 20L
        );
    }

    @Test
    void activePublishRestrictionBlocksMomentCreation() {
        actor.setPublishRestrictedUntil(LocalDateTime.now().plusHours(1));

        assertThatThrownBy(() -> service.publish(command(
                "TEXT", "动态正文", null, null, null
        )))
                .isInstanceOf(BusinessException.class)
                .hasMessage("当前账号暂时不能发布动态");
    }

    @Test
    void publicPageFiltersInaccessibleCandidatesBeforePagination() {
        CommunityMoment visible = moment(
                10L, 100L, 300L, "TEXT", "PUBLISHED"
        );
        CommunityMoment hidden = moment(
                11L, 101L, 301L, "TEXT", "PUBLISHED"
        );
        when(momentMapper.selectList(any()))
                .thenReturn(List.of(visible, hidden));
        when(accessService.requireAccessible(
                LikeTargetType.MOMENT, 10L
        )).thenReturn(target(10L));
        when(accessService.requireAccessible(
                LikeTargetType.MOMENT, 11L
        )).thenThrow(new BusinessException(404, "内容不存在"));

        MomentPageView result =
                service.publicPage(null, 1, 20);

        assertThat(result.total()).isEqualTo(1);
        assertThat(result.records()).extracting(MomentView::momentId)
                .containsExactly(10L);
    }

    @Test
    void detailIncludesCurrentLikeAndFavoriteRelationships() {
        CommunityMoment moment = moment(
                10L, 100L, 300L, "TEXT", "PUBLISHED"
        );
        when(momentMapper.selectById(10L)).thenReturn(moment);
        when(likeMapper.findRelation(100L, "MOMENT", 10L))
                .thenReturn(new CommunityContentLike());
        when(favoriteItemMapper.findRelation(100L, "MOMENT", 10L))
                .thenReturn(new FavoriteItem());

        MomentView result = service.detail(10L);

        assertThat(result.liked()).isTrue();
        assertThat(result.favorited()).isTrue();
        assertThat(result.canonicalPath()).isEqualTo("/moments/10");
    }

    @Test
    void deletingPublishedRepostDecrementsSourceAndIsIdempotent() {
        CommunityMoment repost = moment(
                20L, 100L, 300L, "REPOST", "PUBLISHED"
        );
        repost.setRepostMomentId(10L);
        repost.setLockVersion(0);
        CommunityMoment source = moment(
                10L, 101L, 301L, "TEXT", "PUBLISHED"
        );
        source.setRepostCount(2L);
        when(momentMapper.selectById(20L)).thenReturn(
                repost,
                deleted(repost)
        );
        when(momentMapper.selectById(10L)).thenReturn(source);
        when(momentMapper.update(isNull(), any())).thenReturn(1);

        MomentDeletionView first = service.delete(20L, 0);
        MomentDeletionView second = service.delete(20L, 0);

        assertThat(first.idempotentReplay()).isFalse();
        assertThat(second.idempotentReplay()).isTrue();
        verify(momentMapper, times(2)).update(isNull(), any());
    }

    @Test
    void userCannotDeletePlatformTakenDownMoment() {
        CommunityMoment moment = moment(
                10L, 100L, 300L, "TEXT", "TAKEN_DOWN"
        );
        when(momentMapper.selectById(10L)).thenReturn(moment);

        assertThatThrownBy(() -> service.delete(10L, 0))
                .isInstanceOf(BusinessException.class)
                .hasMessage("平台下架的动态不能由用户删除或恢复");
    }

    private static PublishMomentCommand command(
            String type,
            String text,
            String link,
            Long articleId,
            Long repostId
    ) {
        return new PublishMomentCommand(
                null,
                type,
                text,
                link,
                articleId,
                repostId,
                "PUBLIC"
        );
    }

    private static KeywordReviewOutcome approved() {
        return new KeywordReviewOutcome(
                KeywordReviewOutcome.Decision.AUTO_APPROVE,
                "LOW",
                "AUTO_APPROVED",
                "未命中规则",
                List.of()
        );
    }

    private static AccessibleContentTarget target(Long id) {
        return new AccessibleContentTarget(
                LikeTargetType.MOMENT,
                id,
                100L,
                300L,
                "动态",
                "动态正文",
                null,
                null,
                0,
                0,
                0
        );
    }

    private static CommunityMoment moment(
            Long id,
            Long actorId,
            Long blogId,
            String type,
            String status
    ) {
        CommunityMoment moment = new CommunityMoment();
        moment.setId(id);
        moment.setActorUserId(actorId);
        moment.setBlogId(blogId);
        moment.setMomentType(type);
        moment.setTextContent("动态正文");
        moment.setRenderedHtml("<p>动态正文</p>");
        moment.setVisibility("PUBLIC");
        moment.setStatus(status);
        moment.setLikeCount(0L);
        moment.setFavoriteCount(0L);
        moment.setCommentCount(0L);
        moment.setRepostCount(0L);
        moment.setLockVersion(0);
        moment.setCreatedAt(LocalDateTime.of(2026, 7, 26, 0, 0));
        return moment;
    }

    private static CommunityMoment deleted(CommunityMoment source) {
        CommunityMoment moment = moment(
                source.getId(),
                source.getActorUserId(),
                source.getBlogId(),
                source.getMomentType(),
                "DELETED"
        );
        moment.setRepostMomentId(source.getRepostMomentId());
        moment.setDeletedAt(LocalDateTime.now());
        moment.setLockVersion(1);
        return moment;
    }

    private static CommunityUser user(Long id, Long personalBlogId) {
        CommunityUser user = new CommunityUser();
        user.setId(id);
        user.setUsername("user" + id);
        user.setDisplayName("用户" + id);
        user.setStatus("NORMAL");
        user.setPersonalBlogId(personalBlogId);
        return user;
    }

    private static Blog blog(Long id, Long ownerId) {
        Blog blog = new Blog();
        blog.setId(id);
        blog.setOwnerUserId(ownerId);
        blog.setBlogType("PERSONAL");
        blog.setName("博客" + id);
        blog.setSlug("blog-" + id);
        blog.setStatus("ACTIVE");
        return blog;
    }
}

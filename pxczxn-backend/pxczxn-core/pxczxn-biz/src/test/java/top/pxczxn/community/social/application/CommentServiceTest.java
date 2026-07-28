package top.pxczxn.community.social.application;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import top.pxczxn.community.article.persistence.ArticleMapper;
import top.pxczxn.community.block.application.CommunityBlockService;
import top.pxczxn.community.blog.model.Blog;
import top.pxczxn.community.blog.persistence.BlogMapper;
import top.pxczxn.community.moderation.application.ArticleKeywordReviewEngine;
import top.pxczxn.community.moderation.application.KeywordReviewOutcome;
import top.pxczxn.community.shared.auth.CommunityAuth;
import top.pxczxn.community.social.model.CommunityComment;
import top.pxczxn.community.social.persistence.CommunityCommentMapper;
import top.pxczxn.community.social.persistence.CommunityCommentModerationEventMapper;
import top.pxczxn.community.social.persistence.CommunityContentLikeMapper;
import top.pxczxn.community.social.persistence.CommunityMomentMapper;
import top.pxczxn.community.user.model.CommunityUser;
import top.pxczxn.community.user.persistence.CommunityUserMapper;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CommentServiceTest {

    private CommunityCommentMapper commentMapper;
    private CommunityCommentModerationEventMapper eventMapper;
    private CommunityContentLikeMapper likeMapper;
    private CommunityContentAccessService accessService;
    private CommentScopeService scopeService;
    private CommentContentRenderer renderer;
    private ArticleKeywordReviewEngine reviewEngine;
    private ArticleMapper articleMapper;
    private CommunityMomentMapper momentMapper;
    private CommunityUserMapper userMapper;
    private BlogMapper blogMapper;
    private CommunityAuth auth;
    private CommunityBlockService blockService;
    private CommentService service;
    private CommunityUser actor;

    @BeforeEach
    void setUp() {
        commentMapper = mock(CommunityCommentMapper.class);
        eventMapper = mock(CommunityCommentModerationEventMapper.class);
        likeMapper = mock(CommunityContentLikeMapper.class);
        accessService = mock(CommunityContentAccessService.class);
        scopeService = mock(CommentScopeService.class);
        renderer = mock(CommentContentRenderer.class);
        reviewEngine = mock(ArticleKeywordReviewEngine.class);
        articleMapper = mock(ArticleMapper.class);
        momentMapper = mock(CommunityMomentMapper.class);
        userMapper = mock(CommunityUserMapper.class);
        blogMapper = mock(BlogMapper.class);
        auth = mock(CommunityAuth.class);
        blockService = mock(CommunityBlockService.class);
        service = new CommentService(
                commentMapper,
                eventMapper,
                likeMapper,
                accessService,
                scopeService,
                renderer,
                reviewEngine,
                articleMapper,
                momentMapper,
                userMapper,
                blogMapper,
                auth,
                blockService
        );
        actor = user(100L);
        when(accessService.requireAccessible(
                LikeTargetType.ARTICLE, 400L
        )).thenReturn(target(0));
        when(scopeService.requireCanComment(target(0)))
                .thenReturn(new CommentActorContext(
                        actor, blog(300L, 101L), null
                ));
        when(renderer.render(any())).thenReturn(
                new RenderedCommentContent(
                        "安全内容",
                        "<p>安全内容</p>"
                )
        );
        when(reviewEngine.review(isNull(), isNull(), any()))
                .thenReturn(approved());
        when(commentMapper.insert(any())).thenReturn(1);
        when(eventMapper.insert(any())).thenReturn(1);
        when(articleMapper.update(isNull(), any())).thenReturn(1);
    }

    @Test
    void createsPublishedCommentAndIncrementsTargetCount() {
        CommentView result = service.create(
                "article", 400L, "安全内容"
        );

        assertThat(result.status()).isEqualTo("PUBLISHED");
        assertThat(result.author().userId()).isEqualTo(100L);
        assertThat(result.moderationResult()).isEqualTo("AUTO_APPROVED");
        verify(articleMapper).update(isNull(), any());
        verify(eventMapper).insert(any());
    }

    @Test
    void reviewKeywordQueuesCommentWithoutIncrementingCount() {
        when(reviewEngine.review(isNull(), isNull(), any()))
                .thenReturn(new KeywordReviewOutcome(
                        KeywordReviewOutcome.Decision.MANUAL_REVIEW,
                        "HIGH",
                        "AUTO_ESCALATED_KEYWORD",
                        "命中规则",
                        List.of(900L)
                ));

        CommentView result = service.create(
                "ARTICLE", 400L, "待审核"
        );

        assertThat(result.status()).isEqualTo("PENDING_REVIEW");
        verify(articleMapper, never()).update(any(), any());
    }

    @Test
    void replyAlwaysPointsToTheFlatRootThread() {
        CommunityComment root = comment(
                10L, null, null, 101L, "PUBLISHED"
        );
        CommunityComment parent = comment(
                11L, 10L, 10L, 102L, "PUBLISHED"
        );
        when(commentMapper.selectById(11L)).thenReturn(parent);
        when(commentMapper.selectById(10L)).thenReturn(root);

        service.reply(11L, "三级回复");

        ArgumentCaptor<CommunityComment> captor =
                ArgumentCaptor.forClass(CommunityComment.class);
        verify(commentMapper).insert(captor.capture());
        assertThat(captor.getValue().getRootCommentId()).isEqualTo(10L);
        assertThat(captor.getValue().getParentCommentId()).isEqualTo(11L);
        assertThat(captor.getValue().getReplyToUserId()).isEqualTo(102L);
    }

    @Test
    void pageReturnsThreeReplyPreviewItemsAndFullReplyCount() {
        CommunityComment root = comment(
                10L, null, null, 100L, "PUBLISHED"
        );
        List<CommunityComment> replies = List.of(
                comment(11L, 10L, 10L, 101L, "PUBLISHED"),
                comment(12L, 10L, 11L, 102L, "PUBLISHED"),
                comment(13L, 10L, 12L, 103L, "PUBLISHED"),
                comment(14L, 10L, 13L, 104L, "PUBLISHED")
        );
        when(commentMapper.findPublicRoots("ARTICLE", 400L))
                .thenReturn(List.of(root));
        when(commentMapper.findPublicReplies(10L)).thenReturn(replies);
        for (long id = 100; id <= 104; id++) {
            when(userMapper.selectById(id)).thenReturn(user(id));
        }

        CommentPageView result =
                service.page("ARTICLE", 400L, 1, 20);

        assertThat(result.total()).isEqualTo(1);
        assertThat(result.records().getFirst().replyPreview()).hasSize(3);
        assertThat(result.records().getFirst().replyCount()).isEqualTo(4);
    }

    @Test
    void deletingPublishedOwnCommentIsIdempotentAndDecrementsOnce() {
        CommunityComment comment = comment(
                10L, null, null, 100L, "PUBLISHED"
        );
        when(auth.getLoginUserId()).thenReturn(100L);
        when(userMapper.selectById(100L)).thenReturn(actor);
        when(commentMapper.selectById(10L)).thenReturn(
                comment,
                deleted(comment)
        );
        when(commentMapper.update(isNull(), any())).thenReturn(1);

        CommentModerationView first = service.delete(10L);
        CommentModerationView second = service.delete(10L);

        assertThat(first.affectedComments()).isEqualTo(1);
        assertThat(second.affectedComments()).isZero();
        verify(articleMapper, times(1)).update(isNull(), any());
    }

    @Test
    void contentAuthorHidingRootHidesWholePublishedThread() {
        CommunityComment root = comment(
                10L, null, null, 100L, "PUBLISHED"
        );
        CommunityComment reply = comment(
                11L, 10L, 10L, 102L, "PUBLISHED"
        );
        when(auth.getLoginUserId()).thenReturn(102L);
        when(userMapper.selectById(102L)).thenReturn(user(102L));
        when(commentMapper.selectById(10L)).thenReturn(root);
        when(commentMapper.findPublishedThread(10L))
                .thenReturn(List.of(root, reply));
        when(commentMapper.update(isNull(), any())).thenReturn(1);
        when(blogMapper.selectById(300L))
                .thenReturn(blog(300L, 101L));
        when(accessService.requireAccessible(
                LikeTargetType.ARTICLE, 400L
        )).thenReturn(new AccessibleContentTarget(
                LikeTargetType.ARTICLE,
                400L,
                102L,
                300L,
                "文章",
                "摘要",
                null,
                "/article/400",
                0,
                0,
                2
        ));

        CommentModerationView result =
                service.hide(10L, "离题");

        assertThat(result.status()).isEqualTo("HIDDEN_BY_AUTHOR");
        assertThat(result.affectedComments()).isEqualTo(2);
        assertThat(result.targetCommentCount()).isZero();
        verify(eventMapper, times(2)).insert(any());
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

    private static AccessibleContentTarget target(long commentCount) {
        return new AccessibleContentTarget(
                LikeTargetType.ARTICLE,
                400L,
                102L,
                300L,
                "文章",
                "摘要",
                null,
                "/article/400",
                0,
                0,
                commentCount
        );
    }

    private static CommunityComment comment(
            Long id,
            Long rootId,
            Long parentId,
            Long authorId,
            String status
    ) {
        CommunityComment comment = new CommunityComment();
        comment.setId(id);
        comment.setTargetType("ARTICLE");
        comment.setTargetId(400L);
        comment.setRootCommentId(rootId);
        comment.setParentCommentId(parentId);
        comment.setAuthorUserId(authorId);
        comment.setStatus(status);
        comment.setContentText("评论" + id);
        comment.setRenderedHtml("<p>评论" + id + "</p>");
        comment.setLikeCount(0L);
        comment.setCreatedAt(LocalDateTime.of(2026, 7, 25, 12, 0));
        return comment;
    }

    private static CommunityComment deleted(CommunityComment source) {
        CommunityComment copy = comment(
                source.getId(),
                source.getRootCommentId(),
                source.getParentCommentId(),
                source.getAuthorUserId(),
                "DELETED_BY_USER"
        );
        copy.setDeletedAt(LocalDateTime.now());
        return copy;
    }

    private static CommunityUser user(Long id) {
        CommunityUser user = new CommunityUser();
        user.setId(id);
        user.setUsername("user" + id);
        user.setDisplayName("用户" + id);
        user.setStatus("NORMAL");
        return user;
    }

    private static Blog blog(Long id, Long ownerId) {
        Blog blog = new Blog();
        blog.setId(id);
        blog.setOwnerUserId(ownerId);
        blog.setStatus("ACTIVE");
        return blog;
    }
}

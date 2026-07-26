package top.pxczxn.community.social.application;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import top.pxczxn.community.article.persistence.ArticleMapper;
import top.pxczxn.community.shared.auth.CommunityAuth;
import top.pxczxn.community.social.model.CommunityContentLike;
import top.pxczxn.community.social.persistence.CommunityCommentMapper;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ContentLikeServiceTest {

    private CommunityContentLikeMapper likeMapper;
    private CommunityContentAccessService contentAccessService;
    private ArticleMapper articleMapper;
    private CommunityMomentMapper momentMapper;
    private CommunityCommentMapper commentMapper;
    private CommunityUserMapper userMapper;
    private CommunityAuth communityAuth;
    private LikeListPrivacyService privacyService;
    private ContentLikeService service;

    @BeforeEach
    void setUp() {
        likeMapper = mock(CommunityContentLikeMapper.class);
        contentAccessService = mock(CommunityContentAccessService.class);
        articleMapper = mock(ArticleMapper.class);
        momentMapper = mock(CommunityMomentMapper.class);
        commentMapper = mock(CommunityCommentMapper.class);
        userMapper = mock(CommunityUserMapper.class);
        communityAuth = mock(CommunityAuth.class);
        privacyService = mock(LikeListPrivacyService.class);
        service = new ContentLikeService(
                likeMapper,
                contentAccessService,
                articleMapper,
                momentMapper,
                commentMapper,
                userMapper,
                communityAuth,
                privacyService
        );
        CommunityUser actor = new CommunityUser();
        actor.setId(100L);
        actor.setStatus("NORMAL");
        when(communityAuth.getLoginUserId()).thenReturn(100L);
        when(userMapper.selectById(100L)).thenReturn(actor);
    }

    @Test
    void firstArticleLikeCreatesRelationAndIncrementsCount() {
        when(contentAccessService.requireAccessible(
                LikeTargetType.ARTICLE, 10L
        )).thenReturn(target(LikeTargetType.ARTICLE, 10L, 3));
        when(likeMapper.findRelation(100L, "ARTICLE", 10L)).thenReturn(null);
        when(likeMapper.insert(any())).thenReturn(1);
        when(articleMapper.update(isNull(), any())).thenReturn(1);

        ContentLikeRelationshipView result = service.like("article", 10L);

        assertThat(result.liked()).isTrue();
        assertThat(result.likeCount()).isEqualTo(4);
        verify(likeMapper).insert(any());
        verify(articleMapper).update(isNull(), any());
    }

    @Test
    void repeatedLikeIsIdempotent() {
        when(contentAccessService.requireAccessible(
                LikeTargetType.MOMENT, 11L
        )).thenReturn(target(LikeTargetType.MOMENT, 11L, 8));
        when(likeMapper.findRelation(100L, "MOMENT", 11L))
                .thenReturn(relation(200L, "MOMENT", 11L));

        ContentLikeRelationshipView result = service.like("MOMENT", 11L);

        assertThat(result.liked()).isTrue();
        assertThat(result.likeCount()).isEqualTo(8);
        verify(likeMapper, never()).insert(any());
        verify(momentMapper, never()).update(any(), any());
    }

    @Test
    void unlikeDeletesRelationAndNeverReturnsNegativeCount() {
        when(contentAccessService.requireAccessible(
                LikeTargetType.COMMENT, 12L
        )).thenReturn(target(LikeTargetType.COMMENT, 12L, 0));
        when(likeMapper.findRelation(100L, "COMMENT", 12L))
                .thenReturn(relation(201L, "COMMENT", 12L));
        when(likeMapper.deleteRelation(100L, "COMMENT", 12L)).thenReturn(1);
        when(commentMapper.update(isNull(), any())).thenReturn(1);

        ContentLikeRelationshipView result =
                service.unlike("COMMENT", 12L);

        assertThat(result.liked()).isFalse();
        assertThat(result.likeCount()).isZero();
        verify(commentMapper).update(isNull(), any());
    }

    @Test
    void repeatedUnlikeDoesNotTouchTargetCount() {
        when(contentAccessService.requireAccessible(
                LikeTargetType.ARTICLE, 13L
        )).thenReturn(target(LikeTargetType.ARTICLE, 13L, 5));
        when(likeMapper.findRelation(100L, "ARTICLE", 13L)).thenReturn(null);

        ContentLikeRelationshipView result =
                service.unlike("ARTICLE", 13L);

        assertThat(result.liked()).isFalse();
        assertThat(result.likeCount()).isEqualTo(5);
        verify(likeMapper, never()).deleteRelation(any(), any(), any());
        verify(articleMapper, never()).update(any(), any());
    }

    @Test
    void anonymousRelationshipStillReturnsPublicCount() {
        when(contentAccessService.requireAccessible(
                LikeTargetType.ARTICLE, 14L
        )).thenReturn(target(LikeTargetType.ARTICLE, 14L, 9));
        when(communityAuth.getOptionalLoginUserId()).thenReturn(null);

        ContentLikeRelationshipView result =
                service.relationship("ARTICLE", 14L);

        assertThat(result.liked()).isFalse();
        assertThat(result.likeCount()).isEqualTo(9);
        verify(likeMapper, never()).findRelation(any(), any(), any());
    }

    @Test
    void privateLikePageReturnsAccessibleTargets() {
        CommunityContentLike articleLike =
                relation(301L, "ARTICLE", 15L);
        articleLike.setCreatedAt(LocalDateTime.of(2026, 7, 25, 10, 0));
        when(likeMapper.selectList(any())).thenReturn(List.of(articleLike));
        when(contentAccessService.requireAccessible(
                LikeTargetType.ARTICLE, 15L
        )).thenReturn(target(LikeTargetType.ARTICLE, 15L, 2));

        LikedContentPageView result = service.myLikes(null, 1, 20);

        assertThat(result.total()).isEqualTo(1);
        assertThat(result.records()).hasSize(1);
        assertThat(result.records().getFirst().likeId()).isEqualTo(301L);
    }

    @Test
    void publicLikePageRequiresPrivacyDecision() {
        when(likeMapper.selectList(any())).thenReturn(List.of());

        LikedContentPageView result =
                service.likesOf(101L, "ARTICLE", 1, 20);

        assertThat(result.records()).isEmpty();
        verify(privacyService).requireCanView(101L);
    }

    private static AccessibleContentTarget target(
            LikeTargetType type,
            Long id,
            long likeCount
    ) {
        return new AccessibleContentTarget(
                type,
                id,
                101L,
                201L,
                "内容标题",
                "内容摘要",
                null,
                type == LikeTargetType.ARTICLE ? "/blog/" + id : null,
                likeCount,
                0,
                0
        );
    }

    private static CommunityContentLike relation(
            Long id,
            String targetType,
            Long targetId
    ) {
        CommunityContentLike relation = new CommunityContentLike();
        relation.setId(id);
        relation.setUserId(100L);
        relation.setTargetType(targetType);
        relation.setTargetId(targetId);
        return relation;
    }
}

package top.pxczxn.community.social.application;

import top.pxczxn.platform.common.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import top.pxczxn.community.article.model.Article;
import top.pxczxn.community.article.permission.ArticleAction;
import top.pxczxn.community.article.permission.ArticlePermissionService;
import top.pxczxn.community.article.permission.ArticlePublicAccess;
import top.pxczxn.community.blog.model.Blog;
import top.pxczxn.community.blog.persistence.BlogMapper;
import top.pxczxn.community.shared.auth.CommunityAuth;
import top.pxczxn.community.social.model.CommunityComment;
import top.pxczxn.community.social.model.CommunityFollow;
import top.pxczxn.community.social.model.CommunityMoment;
import top.pxczxn.community.social.persistence.CommunityCommentMapper;
import top.pxczxn.community.social.persistence.CommunityFollowMapper;
import top.pxczxn.community.social.persistence.CommunityMomentMapper;
import top.pxczxn.community.user.model.CommunityUser;
import top.pxczxn.community.user.persistence.CommunityUserMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CommunityContentAccessServiceTest {

    private ArticlePermissionService articlePermissionService;
    private CommunityMomentMapper momentMapper;
    private CommunityCommentMapper commentMapper;
    private BlogMapper blogMapper;
    private CommunityUserMapper userMapper;
    private CommunityFollowMapper followMapper;
    private CommunityAuth communityAuth;
    private CommunityContentAccessService service;

    @BeforeEach
    void setUp() {
        articlePermissionService = mock(ArticlePermissionService.class);
        momentMapper = mock(CommunityMomentMapper.class);
        commentMapper = mock(CommunityCommentMapper.class);
        blogMapper = mock(BlogMapper.class);
        userMapper = mock(CommunityUserMapper.class);
        followMapper = mock(CommunityFollowMapper.class);
        communityAuth = mock(CommunityAuth.class);
        service = new CommunityContentAccessService(
                articlePermissionService,
                momentMapper,
                commentMapper,
                blogMapper,
                userMapper,
                followMapper,
                communityAuth
        );
    }

    @Test
    void articleAccessUsesPublishedArticlePermission() {
        Article article = article(10L, 20L, 30L, 7L);
        Blog blog = blog(20L, 40L);
        CommunityUser author = user(30L);
        when(articlePermissionService.requirePublicArticle(
                10L, ArticleAction.VIEW_DETAIL
        )).thenReturn(new ArticlePublicAccess(
                null, author, blog, article, null
        ));

        AccessibleContentTarget target =
                service.requireAccessible("article", 10L);

        assertThat(target.targetType()).isEqualTo(LikeTargetType.ARTICLE);
        assertThat(target.title()).isEqualTo("文章 10");
        assertThat(target.likeCount()).isEqualTo(7);
    }

    @Test
    void followerOnlyMomentRequiresRealFollowRelation() {
        CommunityMoment moment = moment(
                11L, 31L, 21L, "FOLLOWERS_ONLY", "PUBLISHED"
        );
        when(momentMapper.selectById(11L)).thenReturn(moment);
        when(blogMapper.selectById(21L)).thenReturn(blog(21L, 41L));
        when(userMapper.selectById(31L)).thenReturn(user(31L));
        when(communityAuth.getOptionalLoginUserId()).thenReturn(50L);

        assertThatThrownBy(() -> service.requireAccessible("MOMENT", 11L))
                .isInstanceOf(BusinessException.class)
                .hasMessage("内容不存在");

        when(followMapper.findRelation(50L, "BLOG", 21L))
                .thenReturn(new CommunityFollow());
        AccessibleContentTarget target =
                service.requireAccessible("MOMENT", 11L);

        assertThat(target.targetId()).isEqualTo(11L);
    }

    @Test
    void privateMomentIsVisibleToItsAuthor() {
        CommunityMoment moment =
                moment(12L, 32L, 22L, "PRIVATE", "PUBLISHED");
        when(momentMapper.selectById(12L)).thenReturn(moment);
        when(blogMapper.selectById(22L)).thenReturn(blog(22L, 42L));
        when(userMapper.selectById(32L)).thenReturn(user(32L));
        when(communityAuth.getOptionalLoginUserId()).thenReturn(32L);

        AccessibleContentTarget target =
                service.requireAccessible("MOMENT", 12L);

        assertThat(target.authorUserId()).isEqualTo(32L);
    }

    @Test
    void publishedCommentAlsoChecksItsUnderlyingArticle() {
        CommunityComment comment = new CommunityComment();
        comment.setId(13L);
        comment.setAuthorUserId(33L);
        comment.setTargetType("ARTICLE");
        comment.setTargetId(10L);
        comment.setContentText("一条评论");
        comment.setStatus("PUBLISHED");
        comment.setLikeCount(4L);
        when(commentMapper.selectById(13L)).thenReturn(comment);
        when(userMapper.selectById(33L)).thenReturn(user(33L));

        Article article = article(10L, 20L, 30L, 7L);
        when(articlePermissionService.requirePublicArticle(
                10L, ArticleAction.VIEW_DETAIL
        )).thenReturn(new ArticlePublicAccess(
                null, user(30L), blog(20L, 40L), article, null
        ));

        AccessibleContentTarget target =
                service.requireAccessible("COMMENT", 13L);

        assertThat(target.targetType()).isEqualTo(LikeTargetType.COMMENT);
        assertThat(target.excerpt()).isEqualTo("一条评论");
        assertThat(target.likeCount()).isEqualTo(4);
    }

    @Test
    void hiddenMomentCannotBeUsedAsAnInteractionTarget() {
        when(momentMapper.selectById(14L)).thenReturn(
                moment(14L, 34L, 24L, "PUBLIC", "TAKEN_DOWN")
        );

        assertThatThrownBy(() -> service.requireAccessible("MOMENT", 14L))
                .isInstanceOf(BusinessException.class)
                .hasMessage("内容不存在");
    }

    private static Article article(
            Long id,
            Long blogId,
            Long authorId,
            Long likeCount
    ) {
        Article article = new Article();
        article.setId(id);
        article.setBlogId(blogId);
        article.setAuthorUserId(authorId);
        article.setTitle("文章 " + id);
        article.setSummary("摘要");
        article.setCanonicalPath("/blog/" + id);
        article.setLikeCount(likeCount);
        return article;
    }

    private static CommunityMoment moment(
            Long id,
            Long actorId,
            Long blogId,
            String visibility,
            String status
    ) {
        CommunityMoment moment = new CommunityMoment();
        moment.setId(id);
        moment.setActorUserId(actorId);
        moment.setBlogId(blogId);
        moment.setMomentType("TEXT");
        moment.setTextContent("动态正文");
        moment.setVisibility(visibility);
        moment.setStatus(status);
        moment.setLikeCount(2L);
        return moment;
    }

    private static Blog blog(Long id, Long ownerId) {
        Blog blog = new Blog();
        blog.setId(id);
        blog.setOwnerUserId(ownerId);
        blog.setStatus("ACTIVE");
        return blog;
    }

    private static CommunityUser user(Long id) {
        CommunityUser user = new CommunityUser();
        user.setId(id);
        user.setStatus("NORMAL");
        return user;
    }
}

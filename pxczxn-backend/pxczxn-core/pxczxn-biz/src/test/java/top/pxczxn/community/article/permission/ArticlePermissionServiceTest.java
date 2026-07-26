package top.pxczxn.community.article.permission;

import top.pxczxn.platform.common.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import top.pxczxn.community.article.model.Article;
import top.pxczxn.community.article.persistence.ArticleMapper;
import top.pxczxn.community.blog.model.Blog;
import top.pxczxn.community.blog.persistence.BlogMapper;
import top.pxczxn.community.shared.auth.CommunityAuth;
import top.pxczxn.community.user.model.CommunityUser;
import top.pxczxn.community.user.persistence.CommunityUserMapper;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ArticlePermissionServiceTest {

    private ArticleMapper articleMapper;
    private BlogMapper blogMapper;
    private CommunityUserMapper userMapper;
    private CommunityAuth communityAuth;
    private PlatformArticleAuthority platformAuthority;
    private ArticlePermissionService service;
    private CommunityUser owner;
    private CommunityUser other;
    private Blog personalBlog;
    private Article article;

    @BeforeEach
    void setUp() {
        articleMapper = mock(ArticleMapper.class);
        blogMapper = mock(BlogMapper.class);
        userMapper = mock(CommunityUserMapper.class);
        communityAuth = mock(CommunityAuth.class);
        platformAuthority = mock(PlatformArticleAuthority.class);
        service = service(List.of(new PersonalBlogArticleRoleResolver()), List.of());

        owner = user(100L, "NORMAL");
        owner.setPersonalBlogId(200L);
        other = user(101L, "NORMAL");
        personalBlog = blog(200L, "PERSONAL", 100L, "ACTIVE");
        article = article(300L, 200L, 100L, "DRAFT", "NOT_SUBMITTED");
    }

    @Test
    void unauthenticatedEditorRequestReturns401BeforeResourceLookup() {
        when(communityAuth.getOptionalLoginUserId()).thenReturn(null);

        assertDenied(
                () -> service.requireCommunityArticle(
                        300L,
                        ArticleAction.VIEW_EDITOR
                ),
                401
        );
        verify(articleMapper, never()).selectById(300L);
    }

    @Test
    void ownerCanEditButAnotherCommunityUserGets403() {
        ArticlePermissionDecision ownerDecision = service.decideCommunity(
                ArticleAction.EDIT,
                owner,
                personalBlog,
                article,
                BlogArticleRole.OWNER
        );
        ArticlePermissionDecision otherDecision = service.decideCommunity(
                ArticleAction.EDIT,
                other,
                personalBlog,
                article,
                null
        );

        assertThat(ownerDecision.allowed()).isTrue();
        assertThat(otherDecision.allowed()).isFalse();
        assertThat(otherDecision.failure())
                .isEqualTo(ArticlePermissionFailure.FORBIDDEN);
    }

    @Test
    void pendingReviewEditAndDeleteAreStateConflicts() {
        article.setPublishStatus("PENDING_REVIEW");

        assertThat(service.decideCommunity(
                ArticleAction.EDIT,
                owner,
                personalBlog,
                article,
                BlogArticleRole.OWNER
        ).failure()).isEqualTo(ArticlePermissionFailure.CONFLICT);
        assertThat(service.decideCommunity(
                ArticleAction.DELETE,
                owner,
                personalBlog,
                article,
                BlogArticleRole.OWNER
        ).failure()).isEqualTo(ArticlePermissionFailure.CONFLICT);
    }

    @Test
    void activeReviewAlsoBlocksEditingWhenOldPublicationStaysLive() {
        article.setPublishedVersionId(499L);
        article.setPublishStatus("PUBLISHED");
        article.setReviewStatus("QUEUED");
        article.setReviewVersionId(500L);

        assertThat(service.decideCommunity(
                ArticleAction.EDIT,
                owner,
                personalBlog,
                article,
                BlogArticleRole.OWNER
        ).failure()).isEqualTo(ArticlePermissionFailure.CONFLICT);
        assertThat(service.decideCommunity(
                ArticleAction.WITHDRAW_REVIEW,
                owner,
                personalBlog,
                article,
                BlogArticleRole.OWNER
        ).allowed()).isTrue();
    }

    @Test
    void teamEditorCanEditAnyTeamArticleButTeamAuthorCannot() {
        Blog teamBlog = blog(201L, "TEAM", null, "ACTIVE");
        Article teamArticle =
                article(301L, 201L, 102L, "DRAFT", "NOT_SUBMITTED");

        assertThat(service.decideCommunity(
                ArticleAction.EDIT,
                other,
                teamBlog,
                teamArticle,
                BlogArticleRole.EDITOR
        ).allowed()).isTrue();
        assertThat(service.decideCommunity(
                ArticleAction.EDIT,
                other,
                teamBlog,
                teamArticle,
                BlogArticleRole.AUTHOR
        ).failure()).isEqualTo(ArticlePermissionFailure.FORBIDDEN);
    }

    @Test
    void limitedAccountMayEditButCannotSubmitOrPublish() {
        owner.setStatus("LIMITED");

        assertThat(service.decideCommunity(
                ArticleAction.EDIT,
                owner,
                personalBlog,
                article,
                BlogArticleRole.OWNER
        ).allowed()).isTrue();
        assertThat(service.decideCommunity(
                ArticleAction.SUBMIT_REVIEW,
                owner,
                personalBlog,
                article,
                BlogArticleRole.OWNER
        ).failure()).isEqualTo(ArticlePermissionFailure.FORBIDDEN);
    }

    @Test
    void publicAndUnlistedPublishedContentHaveDifferentListingRules() {
        article.setPublishedVersionId(500L);
        article.setPublishStatus("PUBLISHED");
        article.setReviewStatus("APPROVED");
        article.setVisibility("PUBLIC");

        assertThat(service.decidePublic(
                ArticleAction.VIEW_DETAIL,
                null,
                owner,
                personalBlog,
                article,
                null,
                false
        ).allowed()).isTrue();
        assertThat(service.decidePublic(
                ArticleAction.LIST_PUBLIC,
                null,
                owner,
                personalBlog,
                article,
                null,
                false
        ).allowed()).isTrue();

        article.setVisibility("UNLISTED");
        assertThat(service.decidePublic(
                ArticleAction.VIEW_DETAIL,
                null,
                owner,
                personalBlog,
                article,
                null,
                false
        ).allowed()).isTrue();
        assertThat(service.decidePublic(
                ArticleAction.LIST_PUBLIC,
                null,
                owner,
                personalBlog,
                article,
                null,
                false
        ).failure()).isEqualTo(ArticlePermissionFailure.NOT_FOUND);
    }

    @Test
    void privateAndFollowerOnlyContentDefaultsToNonDisclosure() {
        article.setPublishedVersionId(500L);
        article.setPublishStatus("PUBLISHED");
        article.setVisibility("PRIVATE");

        assertThat(service.decidePublic(
                ArticleAction.VIEW_DETAIL,
                null,
                owner,
                personalBlog,
                article,
                null,
                false
        ).failure()).isEqualTo(ArticlePermissionFailure.NOT_FOUND);
        assertThat(service.decidePublic(
                ArticleAction.VIEW_DETAIL,
                owner,
                owner,
                personalBlog,
                article,
                BlogArticleRole.OWNER,
                false
        ).allowed()).isTrue();

        article.setVisibility("FOLLOWERS_ONLY");
        assertThat(service.decidePublic(
                ArticleAction.VIEW_DETAIL,
                other,
                owner,
                personalBlog,
                article,
                null,
                false
        ).failure()).isEqualTo(ArticlePermissionFailure.NOT_FOUND);
        assertThat(service.decidePublic(
                ArticleAction.VIEW_DETAIL,
                other,
                owner,
                personalBlog,
                article,
                null,
                true
        ).allowed()).isTrue();
    }

    @Test
    void existingPublishedVersionRemainsVisibleDuringNewReview() {
        article.setPublishedVersionId(500L);
        article.setPublishStatus("PENDING_REVIEW");
        article.setReviewStatus("QUEUED");
        article.setVisibility("PUBLIC");

        assertThat(service.decidePublic(
                ArticleAction.VIEW_DETAIL,
                null,
                owner,
                personalBlog,
                article,
                null,
                false
        ).allowed()).isTrue();
    }

    @Test
    void unpublishedDeletedAndTakenDownContentAllReturnNotFoundPublicly() {
        article.setVisibility("PUBLIC");
        assertThat(publicDecision().failure())
                .isEqualTo(ArticlePermissionFailure.NOT_FOUND);

        article.setPublishedVersionId(500L);
        article.setPublishStatus("TAKEN_DOWN");
        assertThat(publicDecision().failure())
                .isEqualTo(ArticlePermissionFailure.NOT_FOUND);

        article.setPublishStatus("DELETED");
        assertThat(publicDecision().failure())
                .isEqualTo(ArticlePermissionFailure.NOT_FOUND);
    }

    @Test
    void platformReviewSeparates401ForbiddenAndWorkflowConflict() {
        when(platformAuthority.isAuthenticated()).thenReturn(false);
        assertDenied(
                () -> service.requirePlatformArticle(
                        300L,
                        ArticleAction.PLATFORM_REVIEW
                ),
                401
        );

        when(platformAuthority.isAuthenticated()).thenReturn(true);
        when(platformAuthority.hasPermission(
                ArticlePermissionService.PLATFORM_REVIEW_PERMISSION
        )).thenReturn(false);
        assertDenied(
                () -> service.requirePlatformArticle(
                        300L,
                        ArticleAction.PLATFORM_REVIEW
                ),
                403
        );

        when(platformAuthority.hasPermission(
                ArticlePermissionService.PLATFORM_REVIEW_PERMISSION
        )).thenReturn(true);
        when(articleMapper.selectById(300L)).thenReturn(article);
        when(blogMapper.selectById(200L)).thenReturn(personalBlog);
        assertDenied(
                () -> service.requirePlatformArticle(
                        300L,
                        ArticleAction.PLATFORM_REVIEW
                ),
                409
        );

        article.setPublishStatus("PENDING_REVIEW");
        article.setReviewStatus("QUEUED");
        article.setReviewVersionId(500L);
        assertThat(service.requirePlatformArticle(
                300L,
                ArticleAction.PLATFORM_REVIEW
        ).article()).isSameAs(article);
    }

    @Test
    void personalAuthoringRejectsClosedBlogAsConflict() {
        when(communityAuth.getOptionalLoginUserId()).thenReturn(100L);
        when(userMapper.selectById(100L)).thenReturn(owner);
        personalBlog.setStatus("CLOSED");
        when(blogMapper.selectById(200L)).thenReturn(personalBlog);

        assertDenied(service::requirePersonalAuthoringContext, 409);
    }

    private ArticlePermissionService service(
            List<BlogArticleRoleResolver> roles,
            List<BlogFollowerResolver> followers
    ) {
        return new ArticlePermissionService(
                articleMapper,
                blogMapper,
                userMapper,
                communityAuth,
                roles,
                followers,
                platformAuthority
        );
    }

    private ArticlePermissionDecision publicDecision() {
        return service.decidePublic(
                ArticleAction.VIEW_DETAIL,
                null,
                owner,
                personalBlog,
                article,
                null,
                false
        );
    }

    private static CommunityUser user(Long id, String status) {
        CommunityUser user = new CommunityUser();
        user.setId(id);
        user.setStatus(status);
        return user;
    }

    private static Blog blog(
            Long id,
            String type,
            Long ownerId,
            String status
    ) {
        Blog blog = new Blog();
        blog.setId(id);
        blog.setBlogType(type);
        blog.setOwnerUserId(ownerId);
        blog.setStatus(status);
        return blog;
    }

    private static Article article(
            Long id,
            Long blogId,
            Long authorId,
            String publishStatus,
            String reviewStatus
    ) {
        Article article = new Article();
        article.setId(id);
        article.setBlogId(blogId);
        article.setAuthorUserId(authorId);
        article.setVisibility("PUBLIC");
        article.setPublishStatus(publishStatus);
        article.setReviewStatus(reviewStatus);
        article.setCurrentVersionId(500L);
        article.setLockVersion(0);
        return article;
    }

    private static void assertDenied(
            org.assertj.core.api.ThrowableAssert.ThrowingCallable callable,
            int expectedCode
    ) {
        assertThatThrownBy(callable)
                .isInstanceOf(BusinessException.class)
                .satisfies(error -> assertThat(
                        ((BusinessException) error).getCode()
                ).isEqualTo(expectedCode));
    }
}

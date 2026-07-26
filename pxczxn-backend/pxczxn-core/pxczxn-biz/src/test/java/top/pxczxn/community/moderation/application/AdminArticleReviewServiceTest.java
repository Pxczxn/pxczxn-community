package top.pxczxn.community.moderation.application;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.mars.common.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import top.pxczxn.community.article.application.ArticlePublishedEvent;
import top.pxczxn.community.article.model.Article;
import top.pxczxn.community.article.model.ArticleVersion;
import top.pxczxn.community.article.permission.ArticlePlatformAccess;
import top.pxczxn.community.article.permission.ArticlePermissionService;
import top.pxczxn.community.article.persistence.ArticleMapper;
import top.pxczxn.community.article.persistence.ArticleVersionMapper;
import top.pxczxn.community.blog.model.Blog;
import top.pxczxn.community.blog.persistence.BlogMapper;
import top.pxczxn.community.moderation.model.ContentReviewTask;
import top.pxczxn.community.moderation.persistence.ContentReviewTaskMapper;
import top.pxczxn.community.notification.application.ArticleReviewDecisionNotificationEvent;
import top.pxczxn.community.user.model.CommunityUser;
import top.pxczxn.community.user.persistence.CommunityUserMapper;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminArticleReviewServiceTest {

    private ContentReviewTaskMapper taskMapper;
    private ArticleMapper articleMapper;
    private ArticleVersionMapper versionMapper;
    private BlogMapper blogMapper;
    private CommunityUserMapper userMapper;
    private ArticlePermissionService permissionService;
    private ApplicationEventPublisher eventPublisher;
    private AdminArticleReviewService service;
    private ContentReviewTask task;
    private Article article;
    private ArticleVersion fixedVersion;
    private Blog blog;
    private CommunityUser author;

    @BeforeEach
    void setUp() {
        taskMapper = mock(ContentReviewTaskMapper.class);
        articleMapper = mock(ArticleMapper.class);
        versionMapper = mock(ArticleVersionMapper.class);
        blogMapper = mock(BlogMapper.class);
        userMapper = mock(CommunityUserMapper.class);
        permissionService = mock(ArticlePermissionService.class);
        eventPublisher = mock(ApplicationEventPublisher.class);
        service = new AdminArticleReviewService(
                taskMapper,
                articleMapper,
                versionMapper,
                blogMapper,
                userMapper,
                permissionService,
                eventPublisher
        );

        task = new ContentReviewTask();
        task.setId(600L);
        task.setSubjectType("ARTICLE");
        task.setSubjectId(300L);
        task.setArticleId(300L);
        task.setFixedVersionId(500L);
        task.setReviewStage("PLATFORM_MANUAL");
        task.setReviewType("MANUAL");
        task.setStatus("QUEUED");
        task.setRiskLevel("HIGH");
        task.setSubmittedByUserId(100L);
        task.setResultCode("AUTO_ESCALATED_KEYWORD");
        task.setResultReason("ruleIds=[10]");
        task.setSubmittedAt(LocalDateTime.now());
        task.setLockVersion(0);

        article = new Article();
        article.setId(300L);
        article.setBlogId(200L);
        article.setAuthorUserId(100L);
        article.setTitle("固定审核文章");
        article.setSummary("摘要");
        article.setVisibility("PUBLIC");
        article.setPublishStatus("PENDING_REVIEW");
        article.setReviewStatus("QUEUED");
        article.setCurrentVersionId(501L);
        article.setReviewVersionId(500L);
        article.setLockVersion(3);

        fixedVersion = new ArticleVersion();
        fixedVersion.setId(500L);
        fixedVersion.setArticleId(300L);
        fixedVersion.setVersionNo(2);
        fixedVersion.setContentMode("MARKDOWN");
        fixedVersion.setMarkdownContent("# fixed");
        fixedVersion.setRenderedHtml("<h1>fixed</h1>");
        fixedVersion.setPlainText("fixed");
        fixedVersion.setContentHash("hash");
        fixedVersion.setWordCount(1);
        fixedVersion.setReadingTimeMinutes(1);

        blog = new Blog();
        blog.setId(200L);
        blog.setName("作者博客");
        blog.setSlug("author-blog");
        blog.setStatus("ACTIVE");

        author = new CommunityUser();
        author.setId(100L);
        author.setUsername("author");
        author.setDisplayName("作者");

        when(taskMapper.selectById(600L)).thenReturn(task);
        when(articleMapper.selectById(300L)).thenReturn(article);
        when(versionMapper.selectById(500L)).thenReturn(fixedVersion);
        when(blogMapper.selectById(200L)).thenReturn(blog);
        when(userMapper.selectById(100L)).thenReturn(author);
        when(permissionService.requirePlatformArticle(any(), any()))
                .thenReturn(new ArticlePlatformAccess(blog, article));
        when(taskMapper.update(any(), any())).thenReturn(1);
        when(articleMapper.update(any(), any())).thenReturn(1);
    }

    @Test
    void detailAlwaysReadsTaskFixedVersionInsteadOfCurrentDraft() {
        AdminArticleReviewDetailView result = service.detail(600L);

        assertThat(result.fixedVersionId()).isEqualTo(500L);
        assertThat(result.content().versionId()).isEqualTo(500L);
        assertThat(result.content().markdownContent()).isEqualTo("# fixed");
        verify(versionMapper).selectById(500L);
        verify(versionMapper, never()).selectById(501L);
    }

    @Test
    void claimMovesTaskAndArticleWithIndependentOptimisticLocks() {
        AdminArticleReviewDetailView result = service.claim(
                600L,
                9L,
                new ClaimArticleReviewCommand(0)
        );

        assertThat(result.taskStatus()).isEqualTo("MANUAL_REVIEWING");
        assertThat(result.reviewStatus()).isEqualTo("MANUAL_REVIEWING");
        assertThat(result.assigneeAdminId()).isEqualTo(9L);
        assertThat(result.taskLockVersion()).isEqualTo(1);
        assertThat(result.articleLockVersion()).isEqualTo(4);
        verify(taskMapper).update(any(), any());
        verify(articleMapper).update(any(), any());
    }

    @Test
    void concurrentClaimReturnsConflictWithoutChangingArticle() {
        when(taskMapper.update(any(), any())).thenReturn(0);

        assertThatThrownBy(() -> service.claim(
                600L,
                9L,
                new ClaimArticleReviewCommand(0)
        ))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo(409);
        verify(articleMapper, never()).update(any(), any());
    }

    @Test
    void approveCompletesTaskAndPublishesNotificationAfterTransaction() {
        task.setStatus("MANUAL_REVIEWING");
        task.setAssigneeAdminId(9L);
        task.setLockVersion(1);
        article.setReviewStatus("MANUAL_REVIEWING");

        AdminArticleReviewDetailView result = service.approve(
                600L,
                9L,
                new DecideArticleReviewCommand(1, "内容符合规范")
        );

        assertThat(result.taskStatus()).isEqualTo("APPROVED");
        assertThat(result.publishStatus()).isEqualTo("APPROVED");
        assertThat(result.reviewStatus()).isEqualTo("APPROVED");
        assertThat(result.resultCode()).isEqualTo("MANUAL_APPROVED");
        assertThat(result.taskLockVersion()).isEqualTo(2);

        ArgumentCaptor<ArticleReviewDecisionNotificationEvent> event =
                ArgumentCaptor.forClass(
                        ArticleReviewDecisionNotificationEvent.class
                );
        verify(eventPublisher).publishEvent(event.capture());
        assertThat(event.getValue().recipientUserId()).isEqualTo(100L);
        assertThat(event.getValue().decision()).isEqualTo("APPROVED");
    }

    @Test
    void immediateArticlePublishesWhenManualReviewApproves() {
        task.setStatus("MANUAL_REVIEWING");
        task.setAssigneeAdminId(9L);
        task.setLockVersion(1);
        article.setReviewStatus("MANUAL_REVIEWING");
        article.setSlug("instant");
        article.setPublishMethod("IMMEDIATE");

        AdminArticleReviewDetailView result = service.approve(
                600L,
                9L,
                new DecideArticleReviewCommand(1, "内容符合规范")
        );

        assertThat(result.publishStatus()).isEqualTo("PUBLISHED");
        assertThat(result.reviewStatus()).isEqualTo("APPROVED");
        assertThat(article.getPublishedVersionId()).isEqualTo(500L);
        assertThat(article.getCanonicalPath())
                .isEqualTo("/author-blog/300/instant");
        ArgumentCaptor<Object> events = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher, times(2)).publishEvent(events.capture());
        assertThat(events.getAllValues()).anyMatch(
                event -> event instanceof ArticlePublishedEvent published
                        && Long.valueOf(500L).equals(
                                published.publishedVersionId()
                        )
                        && "ADMIN_REVIEW".equals(published.trigger())
        );
    }

    @Test
    void revisionRequiresReasonBeforeAnyWrite() {
        task.setStatus("MANUAL_REVIEWING");
        task.setAssigneeAdminId(9L);
        task.setLockVersion(1);
        article.setReviewStatus("MANUAL_REVIEWING");

        assertThatThrownBy(() -> service.requestRevision(
                600L,
                9L,
                new DecideArticleReviewCommand(1, " ")
        ))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo(400);
        verify(taskMapper, never()).update(any(), any());
        verify(articleMapper, never()).update(any(), any());
    }

    @Test
    void onlyAssignedAdministratorCanDecide() {
        task.setStatus("MANUAL_REVIEWING");
        task.setAssigneeAdminId(9L);
        task.setLockVersion(1);
        article.setReviewStatus("MANUAL_REVIEWING");

        assertThatThrownBy(() -> service.reject(
                600L,
                10L,
                new DecideArticleReviewCommand(1, "不符合规范")
        ))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo(403);
        verify(taskMapper, never()).update(any(), any());
        verify(articleMapper, never()).update(any(), any());
    }

    @Test
    void decisionPreservesPreviouslyPublishedVersionVisibility() {
        task.setStatus("MANUAL_REVIEWING");
        task.setAssigneeAdminId(9L);
        task.setLockVersion(1);
        article.setReviewStatus("MANUAL_REVIEWING");
        article.setPublishedVersionId(499L);
        article.setPublishStatus("PUBLISHED");

        AdminArticleReviewDetailView result = service.requestRevision(
                600L,
                9L,
                new DecideArticleReviewCommand(1, "请补充来源")
        );

        assertThat(result.taskStatus()).isEqualTo("REVISION_REQUIRED");
        assertThat(result.publishStatus()).isEqualTo("PUBLISHED");
        assertThat(result.reviewStatus()).isEqualTo("REVISION_REQUIRED");
    }

    @Test
    void invalidPageFilterIsRejectedBeforeDatabaseQuery() {
        assertThatThrownBy(() -> service.page(
                new AdminArticleReviewQuery(
                        "UNKNOWN",
                        null,
                        null,
                        null,
                        1,
                        20
                )
        ))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo(400);
        verify(taskMapper, never()).selectPage(
                any(IPage.class),
                any()
        );
    }
}

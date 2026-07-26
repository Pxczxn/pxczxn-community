package top.pxczxn.community.moderation.application;

import com.mars.common.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import top.pxczxn.community.article.application.ArticlePublishedEvent;
import top.pxczxn.community.article.model.Article;
import top.pxczxn.community.article.model.ArticleVersion;
import top.pxczxn.community.article.persistence.ArticleMapper;
import top.pxczxn.community.article.persistence.ArticleVersionMapper;
import top.pxczxn.community.article.permission.ArticleAction;
import top.pxczxn.community.article.permission.ArticleCommunityAccess;
import top.pxczxn.community.article.permission.ArticlePermissionService;
import top.pxczxn.community.article.permission.BlogArticleRole;
import top.pxczxn.community.blog.model.Blog;
import top.pxczxn.community.moderation.model.ContentReviewTask;
import top.pxczxn.community.moderation.persistence.ContentReviewTaskMapper;
import top.pxczxn.community.user.model.CommunityUser;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ArticleReviewSubmissionServiceTest {

    private ArticleMapper articleMapper;
    private ArticleVersionMapper versionMapper;
    private ContentReviewTaskMapper taskMapper;
    private ArticlePermissionService permissionService;
    private ArticleKeywordReviewEngine reviewEngine;
    private ApplicationEventPublisher eventPublisher;
    private ArticleReviewSubmissionService service;
    private Article article;
    private ArticleVersion version;
    private ArticleCommunityAccess access;

    @BeforeEach
    void setUp() {
        articleMapper = mock(ArticleMapper.class);
        versionMapper = mock(ArticleVersionMapper.class);
        taskMapper = mock(ContentReviewTaskMapper.class);
        permissionService = mock(ArticlePermissionService.class);
        reviewEngine = mock(ArticleKeywordReviewEngine.class);
        eventPublisher = mock(ApplicationEventPublisher.class);
        service = new ArticleReviewSubmissionService(
                articleMapper,
                versionMapper,
                taskMapper,
                permissionService,
                reviewEngine,
                eventPublisher
        );

        CommunityUser actor = new CommunityUser();
        actor.setId(100L);
        actor.setStatus("NORMAL");
        Blog blog = new Blog();
        blog.setId(200L);
        blog.setBlogType("PERSONAL");
        blog.setOwnerUserId(100L);
        blog.setStatus("ACTIVE");
        article = new Article();
        article.setId(300L);
        article.setBlogId(200L);
        article.setAuthorUserId(100L);
        article.setTitle("标题");
        article.setVisibility("PUBLIC");
        article.setPublishStatus("DRAFT");
        article.setReviewStatus("NOT_SUBMITTED");
        article.setCurrentVersionId(500L);
        article.setLockVersion(0);
        access = new ArticleCommunityAccess(
                actor,
                blog,
                article,
                BlogArticleRole.OWNER
        );
        when(permissionService.requireCommunityArticle(eq(300L), any()))
                .thenReturn(access);

        version = new ArticleVersion();
        version.setId(500L);
        version.setArticleId(300L);
        version.setPlainText("正文");
        when(versionMapper.selectOne(any())).thenReturn(version);
        when(taskMapper.selectOne(any())).thenReturn(null);
        when(taskMapper.insert(any())).thenReturn(1);
        when(articleMapper.update(any(), any())).thenReturn(1);
        when(taskMapper.update(any(), any())).thenReturn(1);
        when(reviewEngine.review(any(), any(), any())).thenReturn(
                outcome(
                        KeywordReviewOutcome.Decision.AUTO_APPROVE,
                        "LOW",
                        "AUTO_APPROVED"
                )
        );
    }

    @Test
    void lowRiskSubmissionFixesVersionAndAutoApproves() {
        ArticleReviewStatusView result = service.submit(
                300L,
                new SubmitArticleReviewCommand("submit-key-0001", 0)
        );

        assertThat(result.publishStatus()).isEqualTo("APPROVED");
        assertThat(result.reviewStatus()).isEqualTo("APPROVED");
        assertThat(result.reviewVersionId()).isEqualTo(500L);
        assertThat(result.lockVersion()).isEqualTo(1);
        assertThat(result.latestTask().fixedVersionId()).isEqualTo(500L);
        assertThat(result.latestTask().status()).isEqualTo("APPROVED");

        ArgumentCaptor<ContentReviewTask> task =
                ArgumentCaptor.forClass(ContentReviewTask.class);
        verify(taskMapper).insert(task.capture());
        assertThat(task.getValue().getReviewStage()).isEqualTo("PLATFORM_AUTO");
        assertThat(task.getValue().getReviewType()).isEqualTo("AUTO");
        assertThat(task.getValue().getCompletedAt()).isNotNull();
    }

    @Test
    void immediateArticlePublishesWhenAutomaticReviewApproves() {
        access.blog().setSlug("author-blog");
        article.setSlug("instant");
        article.setPublishMethod("IMMEDIATE");

        ArticleReviewStatusView result = service.submit(
                300L,
                new SubmitArticleReviewCommand("submit-key-immediate", 0)
        );

        assertThat(result.publishStatus()).isEqualTo("PUBLISHED");
        assertThat(result.reviewStatus()).isEqualTo("APPROVED");
        assertThat(article.getPublishedVersionId()).isEqualTo(500L);
        assertThat(article.getCanonicalPath())
                .isEqualTo("/author-blog/300/instant");
        assertThat(article.getPublishedAt()).isNotNull();

        ArgumentCaptor<ArticlePublishedEvent> event =
                ArgumentCaptor.forClass(ArticlePublishedEvent.class);
        verify(eventPublisher).publishEvent(event.capture());
        assertThat(event.getValue().publishedVersionId()).isEqualTo(500L);
        assertThat(event.getValue().canonicalPath())
                .isEqualTo("/author-blog/300/instant");
        assertThat(event.getValue().trigger()).isEqualTo("AUTO_REVIEW");
    }

    @Test
    void manualEscalationQueuesFixedVersionAndPreservesOldPublication() {
        article.setPublishedVersionId(499L);
        article.setPublishStatus("PUBLISHED");
        when(reviewEngine.review(any(), any(), any())).thenReturn(
                outcome(
                        KeywordReviewOutcome.Decision.MANUAL_REVIEW,
                        "HIGH",
                        "AUTO_ESCALATED_KEYWORD"
                )
        );

        ArticleReviewStatusView result = service.submit(
                300L,
                new SubmitArticleReviewCommand("submit-key-0002", 0)
        );

        assertThat(result.publishStatus()).isEqualTo("PUBLISHED");
        assertThat(result.reviewStatus()).isEqualTo("QUEUED");
        assertThat(result.latestTask().status()).isEqualTo("QUEUED");
        assertThat(result.latestTask().reviewStage())
                .isEqualTo("PLATFORM_MANUAL");
        assertThat(result.latestTask().completedAt()).isNull();
    }

    @Test
    void identicalIdempotencyKeyReturnsOriginalTaskWithoutNewWrite() {
        ContentReviewTask existing = activeTask();
        existing.setIdempotencyKey("submit-key-0003");
        when(taskMapper.selectOne(any())).thenReturn(existing);
        article.setPublishStatus("PENDING_REVIEW");
        article.setReviewStatus("QUEUED");
        article.setReviewVersionId(500L);
        article.setLockVersion(1);

        ArticleReviewStatusView result = service.submit(
                300L,
                new SubmitArticleReviewCommand("submit-key-0003", 0)
        );

        assertThat(result.latestTask().taskId()).isEqualTo(600L);
        assertThat(result.lockVersion()).isEqualTo(1);
        verify(taskMapper, never()).insert(any());
        verify(reviewEngine, never()).review(any(), any(), any());
        verify(permissionService, never()).requireCommunityArticle(
                300L,
                ArticleAction.SUBMIT_REVIEW
        );
    }

    @Test
    void privateArticleCannotEnterPublicReview() {
        article.setVisibility("PRIVATE");

        assertThatThrownBy(() -> service.submit(
                300L,
                new SubmitArticleReviewCommand("submit-key-0004", 0)
        ))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("私密");
        verify(taskMapper, never()).insert(any());
        verify(reviewEngine, never()).review(any(), any(), any());
    }

    @Test
    void staleArticleLockStopsBeforeCreatingTask() {
        article.setLockVersion(2);

        assertThatThrownBy(() -> service.submit(
                300L,
                new SubmitArticleReviewCommand("submit-key-0005", 1)
        ))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("其他窗口");
        verify(taskMapper, never()).insert(any());
        verify(reviewEngine, never()).review(any(), any(), any());
    }

    @Test
    void blockOutcomeCreatesRejectedAuditWithoutPublishing() {
        when(reviewEngine.review(any(), any(), any())).thenReturn(
                outcome(
                        KeywordReviewOutcome.Decision.BLOCK,
                        "CRITICAL",
                        "AUTO_BLOCKED_KEYWORD"
                )
        );

        ArticleReviewStatusView result = service.submit(
                300L,
                new SubmitArticleReviewCommand("submit-key-0006", 0)
        );

        assertThat(result.publishStatus()).isEqualTo("DRAFT");
        assertThat(result.reviewStatus()).isEqualTo("REJECTED");
        assertThat(result.latestTask().status()).isEqualTo("REJECTED");
        assertThat(result.latestTask().riskLevel()).isEqualTo("CRITICAL");
    }

    @Test
    void withdrawCancelsActiveTaskAndKeepsOldPublishedVersion() {
        article.setPublishedVersionId(499L);
        article.setPublishStatus("PUBLISHED");
        article.setReviewStatus("QUEUED");
        article.setReviewVersionId(500L);
        article.setLockVersion(1);
        ContentReviewTask active = activeTask();
        when(taskMapper.selectOne(any())).thenReturn(active);

        ArticleReviewStatusView result = service.withdraw(
                300L,
                new WithdrawArticleReviewCommand(1, "需要继续修改")
        );

        assertThat(result.publishStatus()).isEqualTo("PUBLISHED");
        assertThat(result.reviewStatus()).isEqualTo("CANCELLED");
        assertThat(result.reviewVersionId()).isNull();
        assertThat(result.lockVersion()).isEqualTo(2);
        assertThat(result.latestTask().status()).isEqualTo("CANCELLED");
        assertThat(result.latestTask().resultCode()).isEqualTo("USER_WITHDRAWN");
        assertThat(result.latestTask().lockVersion()).isEqualTo(1);
        verify(taskMapper).update(any(), any());
        verify(articleMapper).update(any(), any());
    }

    private static KeywordReviewOutcome outcome(
            KeywordReviewOutcome.Decision decision,
            String risk,
            String code
    ) {
        return new KeywordReviewOutcome(
                decision,
                risk,
                code,
                "自动审核结果",
                List.of()
        );
    }

    private ContentReviewTask activeTask() {
        ContentReviewTask task = new ContentReviewTask();
        task.setId(600L);
        task.setArticleId(300L);
        task.setSubjectId(300L);
        task.setSubjectType("ARTICLE");
        task.setFixedVersionId(500L);
        task.setReviewStage("PLATFORM_MANUAL");
        task.setReviewType("MANUAL");
        task.setStatus("QUEUED");
        task.setRiskLevel("HIGH");
        task.setIdempotencyKey("submit-key");
        task.setSubmittedByUserId(100L);
        task.setResultCode("AUTO_ESCALATED_KEYWORD");
        task.setResultReason("自动审核结果");
        task.setSubmittedAt(LocalDateTime.now());
        task.setLockVersion(0);
        task.setCreatedAt(LocalDateTime.now());
        task.setUpdatedAt(LocalDateTime.now());
        return task;
    }
}

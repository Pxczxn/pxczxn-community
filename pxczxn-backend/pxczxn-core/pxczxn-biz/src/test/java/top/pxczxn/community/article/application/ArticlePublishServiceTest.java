package top.pxczxn.community.article.application;

import top.pxczxn.platform.common.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import top.pxczxn.community.article.model.Article;
import top.pxczxn.community.article.model.ArticleVersion;
import top.pxczxn.community.article.permission.ArticleAction;
import top.pxczxn.community.article.permission.ArticleCommunityAccess;
import top.pxczxn.community.article.permission.ArticlePermissionService;
import top.pxczxn.community.article.permission.BlogArticleRole;
import top.pxczxn.community.article.persistence.ArticleMapper;
import top.pxczxn.community.article.persistence.ArticleVersionMapper;
import top.pxczxn.community.blog.model.Blog;
import top.pxczxn.community.user.model.CommunityUser;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ArticlePublishServiceTest {

    private ArticleMapper articleMapper;
    private ArticleVersionMapper versionMapper;
    private ArticlePermissionService permissionService;
    private ArticlePublishTaskManager taskManager;
    private ApplicationEventPublisher eventPublisher;
    private ArticlePublishService service;
    private Article article;
    private ArticleVersion version;
    private ArticleCommunityAccess access;

    @BeforeEach
    void setUp() {
        articleMapper = mock(ArticleMapper.class);
        versionMapper = mock(ArticleVersionMapper.class);
        permissionService = mock(ArticlePermissionService.class);
        taskManager = mock(ArticlePublishTaskManager.class);
        eventPublisher = mock(ApplicationEventPublisher.class);
        service = new ArticlePublishService(
                articleMapper,
                versionMapper,
                permissionService,
                taskManager,
                eventPublisher
        );

        CommunityUser actor = new CommunityUser();
        actor.setId(100L);
        actor.setStatus("NORMAL");
        Blog blog = new Blog();
        blog.setId(200L);
        blog.setBlogType("PERSONAL");
        blog.setOwnerUserId(100L);
        blog.setSlug("author-blog");
        blog.setStatus("ACTIVE");
        article = new Article();
        article.setId(300L);
        article.setBlogId(200L);
        article.setAuthorUserId(100L);
        article.setSlug("approved-article");
        article.setPublishMethod("MANUAL");
        article.setPublishStatus("APPROVED");
        article.setReviewStatus("APPROVED");
        article.setCurrentVersionId(500L);
        article.setReviewVersionId(500L);
        article.setLockVersion(4);
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
        when(versionMapper.selectById(500L)).thenReturn(version);
        when(articleMapper.update(any(), any())).thenReturn(1);
    }

    @Test
    void manualPublishAtomicallySwitchesApprovedVersionAndCanonical() {
        ArticlePublishView result = service.publish(
                300L,
                new PublishArticleCommand(4)
        );

        assertThat(result.publishedVersionId()).isEqualTo(500L);
        assertThat(result.publishStatus()).isEqualTo("PUBLISHED");
        assertThat(result.reviewStatus()).isEqualTo("APPROVED");
        assertThat(result.canonicalPath())
                .isEqualTo("/author-blog/300/approved-article");
        assertThat(result.lockVersion()).isEqualTo(5);
        assertThat(result.idempotentReplay()).isFalse();
        verify(articleMapper).update(any(), any());

        ArgumentCaptor<ArticlePublishedEvent> event =
                ArgumentCaptor.forClass(ArticlePublishedEvent.class);
        verify(eventPublisher).publishEvent(event.capture());
        assertThat(event.getValue().publishedVersionId()).isEqualTo(500L);
        assertThat(event.getValue().trigger()).isEqualTo("USER");
    }

    @Test
    void publishingApprovedRevisionReturnsPreviousPublicVersion() {
        article.setPublishedVersionId(499L);
        article.setPublishStatus("PUBLISHED");

        ArticlePublishView result = service.publish(
                300L,
                new PublishArticleCommand(4)
        );

        assertThat(result.previousPublishedVersionId()).isEqualTo(499L);
        assertThat(result.publishedVersionId()).isEqualTo(500L);
    }

    @Test
    void exactAlreadyPublishedVersionIsIdempotentEvenWithOldLock() {
        article.setPublishedVersionId(500L);
        article.setPublishStatus("PUBLISHED");
        article.setCanonicalPath("/author-blog/300/approved-article");
        article.setPublishedAt(LocalDateTime.now());
        article.setLockVersion(5);

        ArticlePublishView result = service.publish(
                300L,
                new PublishArticleCommand(4)
        );

        assertThat(result.idempotentReplay()).isTrue();
        assertThat(result.lockVersion()).isEqualTo(5);
        verify(permissionService, never()).requireCommunityArticle(
                300L,
                ArticleAction.PUBLISH
        );
        verify(articleMapper, never()).update(any(), any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void staleLockRejectsBeforeVersionReadOrWrite() {
        assertThatThrownBy(() -> service.publish(
                300L,
                new PublishArticleCommand(3)
        ))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo(409);
        verify(versionMapper, never()).selectById(any());
        verify(articleMapper, never()).update(any(), any());
    }

    @Test
    void scheduledArticleRequiresPlannedTime() {
        article.setPublishMethod("SCHEDULED");

        assertThatThrownBy(() -> service.publish(
                300L,
                new PublishArticleCommand(4)
        ))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo(400);
        verify(articleMapper, never()).update(any(), any());
    }

    @Test
    void scheduledArticleCreatesDurableTaskAndPendingState() {
        article.setPublishMethod("SCHEDULED");
        LocalDateTime scheduledAt = LocalDateTime.now().plusHours(2);

        ArticlePublishView result = service.publish(
                300L,
                new PublishArticleCommand(4, scheduledAt, false)
        );

        assertThat(result.publishStatus()).isEqualTo("SCHEDULED");
        assertThat(result.scheduledPublishAt()).isEqualTo(scheduledAt);
        assertThat(result.lockVersion()).isEqualTo(5);
        verify(taskManager).replaceActiveTask(
                article,
                500L,
                scheduledAt,
                article.getUpdatedAt()
        );
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void exactScheduledRequestIsIdempotentEvenWithOldLock() {
        article.setPublishMethod("SCHEDULED");
        article.setPublishStatus("SCHEDULED");
        article.setScheduledPublishAt(LocalDateTime.now().plusHours(2));
        article.setLockVersion(5);

        ArticlePublishView result = service.publish(
                300L,
                new PublishArticleCommand(
                        4,
                        article.getScheduledPublishAt(),
                        false
                )
        );

        assertThat(result.idempotentReplay()).isTrue();
        assertThat(result.lockVersion()).isEqualTo(5);
        verify(articleMapper, never()).update(any(), any());
        verify(taskManager, never()).replaceActiveTask(
                any(),
                any(),
                any(),
                any()
        );
    }

    @Test
    void cancellationRestoresApprovedStateAndCancelsTask() {
        article.setPublishMethod("SCHEDULED");
        article.setPublishStatus("SCHEDULED");
        article.setScheduledPublishAt(LocalDateTime.now().plusHours(2));

        ArticlePublishView result = service.publish(
                300L,
                new PublishArticleCommand(4, null, true)
        );

        assertThat(result.publishStatus()).isEqualTo("APPROVED");
        assertThat(result.scheduledPublishAt()).isNull();
        assertThat(result.lockVersion()).isEqualTo(5);
        verify(taskManager).cancelActiveTask(
                eq(300L),
                eq("USER_CANCELLED"),
                any(),
                any()
        );
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void currentDraftMustStillMatchApprovedReviewVersion() {
        article.setCurrentVersionId(501L);

        assertThatThrownBy(() -> service.publish(
                300L,
                new PublishArticleCommand(4)
        ))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo(409);
        verify(articleMapper, never()).update(any(), any());
    }

    @Test
    void optimisticUpdateCollisionReturnsConflictWithoutEvent() {
        when(articleMapper.update(any(), any())).thenReturn(0);

        assertThatThrownBy(() -> service.publish(
                300L,
                new PublishArticleCommand(4)
        ))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo(409);
        verify(eventPublisher, never()).publishEvent(any());
    }
}

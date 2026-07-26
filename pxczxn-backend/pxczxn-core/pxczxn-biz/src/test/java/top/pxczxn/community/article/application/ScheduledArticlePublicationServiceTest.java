package top.pxczxn.community.article.application;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import top.pxczxn.community.article.model.Article;
import top.pxczxn.community.article.model.ArticlePublishTask;
import top.pxczxn.community.article.model.ArticleVersion;
import top.pxczxn.community.article.persistence.ArticleMapper;
import top.pxczxn.community.article.persistence.ArticlePublishTaskMapper;
import top.pxczxn.community.article.persistence.ArticleVersionMapper;
import top.pxczxn.community.blog.model.Blog;
import top.pxczxn.community.blog.persistence.BlogMapper;
import top.pxczxn.community.user.model.CommunityUser;
import top.pxczxn.community.user.persistence.CommunityUserMapper;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ScheduledArticlePublicationServiceTest {

    private ArticlePublishTaskMapper taskMapper;
    private ArticleMapper articleMapper;
    private ArticleVersionMapper versionMapper;
    private BlogMapper blogMapper;
    private CommunityUserMapper userMapper;
    private ApplicationEventPublisher eventPublisher;
    private ScheduledArticlePublicationService service;
    private ArticlePublishTask task;
    private Article article;
    private LocalDateTime now;

    @BeforeEach
    void setUp() {
        taskMapper = mock(ArticlePublishTaskMapper.class);
        articleMapper = mock(ArticleMapper.class);
        versionMapper = mock(ArticleVersionMapper.class);
        blogMapper = mock(BlogMapper.class);
        userMapper = mock(CommunityUserMapper.class);
        eventPublisher = mock(ApplicationEventPublisher.class);
        service = new ScheduledArticlePublicationService(
                taskMapper,
                articleMapper,
                versionMapper,
                blogMapper,
                userMapper,
                eventPublisher
        );

        now = LocalDateTime.of(2026, 7, 25, 12, 0);
        task = new ArticlePublishTask();
        task.setId(700L);
        task.setArticleId(300L);
        task.setArticleVersionId(500L);
        task.setScheduledPublishAt(now.minusMinutes(1));
        task.setStatus("RUNNING");
        task.setAttemptCount(1);
        task.setMaxAttempts(3);
        task.setLockVersion(1);

        article = new Article();
        article.setId(300L);
        article.setBlogId(200L);
        article.setAuthorUserId(100L);
        article.setSlug("scheduled-article");
        article.setPublishMethod("SCHEDULED");
        article.setPublishStatus("SCHEDULED");
        article.setReviewStatus("APPROVED");
        article.setCurrentVersionId(500L);
        article.setReviewVersionId(500L);
        article.setPublishedVersionId(499L);
        article.setScheduledPublishAt(task.getScheduledPublishAt());
        article.setLockVersion(4);

        ArticleVersion version = new ArticleVersion();
        version.setId(500L);
        version.setArticleId(300L);
        Blog blog = new Blog();
        blog.setId(200L);
        blog.setSlug("author-blog");
        blog.setStatus("ACTIVE");
        CommunityUser author = new CommunityUser();
        author.setId(100L);
        author.setStatus("NORMAL");

        when(taskMapper.selectById(700L)).thenReturn(task);
        when(articleMapper.selectById(300L)).thenReturn(article);
        when(versionMapper.selectById(500L)).thenReturn(version);
        when(blogMapper.selectById(200L)).thenReturn(blog);
        when(userMapper.selectById(100L)).thenReturn(author);
        when(articleMapper.update(any(), any())).thenReturn(1);
        when(taskMapper.update(any(), any())).thenReturn(1);
    }

    @Test
    void dueTaskAtomicallyPublishesApprovedVersion() {
        ScheduledPublishResult result =
                service.publishClaimed(700L, now);

        assertThat(result.status()).isEqualTo("PUBLISHED");
        assertThat(result.reasonCode()).isNull();
        verify(articleMapper).update(any(), any());
        verify(taskMapper).update(any(), any());

        ArgumentCaptor<ArticlePublishedEvent> event =
                ArgumentCaptor.forClass(ArticlePublishedEvent.class);
        verify(eventPublisher).publishEvent(event.capture());
        assertThat(event.getValue().previousPublishedVersionId())
                .isEqualTo(499L);
        assertThat(event.getValue().publishedVersionId()).isEqualTo(500L);
        assertThat(event.getValue().canonicalPath())
                .isEqualTo("/author-blog/300/scheduled-article");
        assertThat(event.getValue().trigger()).isEqualTo("QUARTZ");
    }

    @Test
    void duplicateExecutionBecomesIdempotentSuccess() {
        Article latest = new Article();
        latest.setId(300L);
        latest.setPublishStatus("PUBLISHED");
        latest.setPublishedVersionId(500L);
        latest.setPublishedAt(now.minusSeconds(1));
        when(articleMapper.update(any(), any())).thenReturn(0);
        when(articleMapper.selectById(300L))
                .thenReturn(article)
                .thenReturn(latest);

        ScheduledPublishResult result =
                service.publishClaimed(700L, now);

        assertThat(result.status()).isEqualTo("IDEMPOTENT");
        assertThat(result.reasonCode()).isEqualTo("ALREADY_PUBLISHED");
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void limitedAuthorCreatesStructuredPermanentFailure() {
        CommunityUser limited = new CommunityUser();
        limited.setId(100L);
        limited.setStatus("LIMITED");
        when(userMapper.selectById(100L)).thenReturn(limited);

        ScheduledPublishResult result =
                service.publishClaimed(700L, now);

        assertThat(result.status()).isEqualTo("FAILED");
        assertThat(result.reasonCode())
                .isEqualTo("AUTHOR_NOT_PUBLISHABLE");
        verify(articleMapper).update(any(), any());
        verify(taskMapper).update(any(), any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void oldTaskCannotOverwriteChangedSchedule() {
        article.setScheduledPublishAt(now.plusHours(2));

        ScheduledPublishResult result =
                service.publishClaimed(700L, now);

        assertThat(result.status()).isEqualTo("CANCELLED");
        assertThat(result.reasonCode()).isEqualTo("STATE_CHANGED");
        verify(articleMapper, never()).update(any(), any());
        verify(taskMapper).update(any(), any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void taskClaimMustStillBeRunning() {
        task.setStatus("RETRY_WAIT");

        ScheduledPublishResult result =
                service.publishClaimed(700L, now);

        assertThat(result.status()).isEqualTo("SKIPPED");
        assertThat(result.reasonCode()).isEqualTo("TASK_NOT_RUNNING");
        verify(articleMapper, never()).selectById(any());
        verify(eventPublisher, never()).publishEvent(any());
    }
}

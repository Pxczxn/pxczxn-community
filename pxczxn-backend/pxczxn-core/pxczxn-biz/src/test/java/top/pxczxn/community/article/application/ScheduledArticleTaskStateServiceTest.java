package top.pxczxn.community.article.application;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import top.pxczxn.community.article.model.Article;
import top.pxczxn.community.article.model.ArticlePublishTask;
import top.pxczxn.community.article.persistence.ArticleMapper;
import top.pxczxn.community.article.persistence.ArticlePublishTaskMapper;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ScheduledArticleTaskStateServiceTest {

    private ArticlePublishTaskMapper taskMapper;
    private ArticleMapper articleMapper;
    private ScheduledArticleTaskStateService service;
    private ArticlePublishTask task;
    private Article article;
    private LocalDateTime now;

    @BeforeEach
    void setUp() {
        taskMapper = mock(ArticlePublishTaskMapper.class);
        articleMapper = mock(ArticleMapper.class);
        service = new ScheduledArticleTaskStateService(
                taskMapper,
                articleMapper
        );
        now = LocalDateTime.of(2026, 7, 25, 12, 0);

        task = new ArticlePublishTask();
        task.setId(700L);
        task.setArticleId(300L);
        task.setArticleVersionId(500L);
        task.setScheduledPublishAt(now.minusMinutes(1));
        task.setNextAttemptAt(now.minusMinutes(1));
        task.setStatus("WAITING");
        task.setAttemptCount(0);
        task.setMaxAttempts(3);
        task.setLockVersion(0);
        task.setUpdatedAt(now.minusMinutes(1));

        article = new Article();
        article.setId(300L);
        article.setPublishStatus("SCHEDULED");
        article.setReviewVersionId(500L);
        article.setScheduledPublishAt(task.getScheduledPublishAt());
        article.setLockVersion(4);

        when(taskMapper.selectById(700L)).thenReturn(task);
        when(articleMapper.selectById(300L)).thenReturn(article);
        when(taskMapper.update(any(), any())).thenReturn(1);
        when(articleMapper.update(any(), any())).thenReturn(1);
    }

    @Test
    void dueTaskCanOnlyBeClaimedWithItsLockVersion() {
        ArticlePublishTask claimed = service.claim(700L, now);

        assertThat(claimed).isNotNull();
        assertThat(claimed.getStatus()).isEqualTo("RUNNING");
        assertThat(claimed.getAttemptCount()).isEqualTo(1);
        assertThat(claimed.getLockVersion()).isEqualTo(1);
        verify(taskMapper).update(any(), any());
    }

    @Test
    void transientFailureUsesBoundedBackoff() {
        task.setStatus("RUNNING");
        task.setAttemptCount(1);
        task.setLockVersion(1);

        ScheduledPublishResult result =
                service.recordTransientFailure(700L, now);

        assertThat(result.status()).isEqualTo("RETRY_WAIT");
        assertThat(result.reasonCode()).isEqualTo("TRANSIENT_FAILURE");
        verify(taskMapper).update(any(), any());
    }

    @Test
    void retryExhaustionMovesArticleAndTaskToFailure() {
        task.setStatus("RUNNING");
        task.setAttemptCount(3);
        task.setLockVersion(3);

        ScheduledPublishResult result =
                service.recordTransientFailure(700L, now);

        assertThat(result.status()).isEqualTo("FAILED");
        assertThat(result.reasonCode()).isEqualTo("RETRY_EXHAUSTED");
        verify(articleMapper).update(any(), any());
        verify(taskMapper).update(any(), any());
    }
}

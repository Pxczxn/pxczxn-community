package top.pxczxn.community.web.article;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import top.pxczxn.community.article.application.ArticlePublishService;
import top.pxczxn.community.article.application.ArticlePublishView;
import top.pxczxn.community.article.application.PublishArticleCommand;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ArticlePublishControllerTest {

    private ArticlePublishService service;
    private ArticlePublishController controller;

    @BeforeEach
    void setUp() {
        service = mock(ArticlePublishService.class);
        controller = new ArticlePublishController(service);
    }

    @Test
    void publishMapsLockAndSnowflakeIds() {
        LocalDateTime now = LocalDateTime.now();
        when(service.publish(
                300L,
                new PublishArticleCommand(4)
        )).thenReturn(new ArticlePublishView(
                300L,
                499L,
                500L,
                "PUBLISHED",
                "APPROVED",
                "/blog/300/article",
                now,
                5,
                false
        ));

        var result = controller.publish(
                300L,
                new PublishArticleRequest(4)
        );

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData().articleId()).isEqualTo("300");
        assertThat(result.getData().previousPublishedVersionId())
                .isEqualTo("499");
        assertThat(result.getData().publishedVersionId()).isEqualTo("500");
        assertThat(result.getData().canonicalPath())
                .isEqualTo("/blog/300/article");
        verify(service).publish(300L, new PublishArticleCommand(4));
    }

    @Test
    void publishExposesIdempotentReplayFlag() {
        when(service.publish(
                300L,
                new PublishArticleCommand(4)
        )).thenReturn(new ArticlePublishView(
                300L,
                500L,
                500L,
                "PUBLISHED",
                "APPROVED",
                "/blog/300/article",
                LocalDateTime.now(),
                5,
                true
        ));

        var result = controller.publish(
                300L,
                new PublishArticleRequest(4)
        );

        assertThat(result.getData().idempotentReplay()).isTrue();
    }

    @Test
    void publishMapsScheduledTimeAndCancellationFlag() {
        LocalDateTime scheduledAt = LocalDateTime.now().plusHours(1);
        when(service.publish(
                300L,
                new PublishArticleCommand(4, scheduledAt, false)
        )).thenReturn(new ArticlePublishView(
                300L,
                null,
                null,
                "SCHEDULED",
                "APPROVED",
                null,
                scheduledAt,
                null,
                5,
                false
        ));

        var result = controller.publish(
                300L,
                new PublishArticleRequest(4, scheduledAt, false)
        );

        assertThat(result.getData().publishStatus()).isEqualTo("SCHEDULED");
        assertThat(result.getData().scheduledPublishAt())
                .isEqualTo(scheduledAt);
        verify(service).publish(
                300L,
                new PublishArticleCommand(4, scheduledAt, false)
        );
    }
}

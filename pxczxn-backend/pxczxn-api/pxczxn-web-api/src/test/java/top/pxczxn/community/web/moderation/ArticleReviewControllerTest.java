package top.pxczxn.community.web.moderation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import top.pxczxn.community.moderation.application.ArticleReviewStatusView;
import top.pxczxn.community.moderation.application.ArticleReviewSubmissionService;
import top.pxczxn.community.moderation.application.ArticleReviewTaskView;
import top.pxczxn.community.moderation.application.SubmitArticleReviewCommand;
import top.pxczxn.community.moderation.application.WithdrawArticleReviewCommand;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ArticleReviewControllerTest {

    private ArticleReviewSubmissionService service;
    private ArticleReviewController controller;

    @BeforeEach
    void setUp() {
        service = mock(ArticleReviewSubmissionService.class);
        controller = new ArticleReviewController(service);
    }

    @Test
    void submitMapsStringIdsAndDelegatesIdempotencyData() {
        when(service.submit(
                300L,
                new SubmitArticleReviewCommand("submit-key-1000", 2)
        )).thenReturn(status("QUEUED", 3));

        var result = controller.submit(
                300L,
                new SubmitArticleReviewRequest("submit-key-1000", 2)
        );

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData().articleId()).isEqualTo("300");
        assertThat(result.getData().reviewVersionId()).isEqualTo("500");
        assertThat(result.getData().latestTask().taskId()).isEqualTo("600");
        assertThat(result.getData().latestTask().fixedVersionId())
                .isEqualTo("500");
        verify(service).submit(
                300L,
                new SubmitArticleReviewCommand("submit-key-1000", 2)
        );
    }

    @Test
    void withdrawDelegatesLockAndReason() {
        when(service.withdraw(
                300L,
                new WithdrawArticleReviewCommand(3, "继续修改")
        )).thenReturn(status("CANCELLED", 4));

        var result = controller.withdraw(
                300L,
                new WithdrawArticleReviewRequest(3, "继续修改")
        );

        assertThat(result.getData().reviewStatus()).isEqualTo("CANCELLED");
        assertThat(result.getData().lockVersion()).isEqualTo(4);
    }

    @Test
    void statusReturnsLatestTask() {
        when(service.getStatus(300L)).thenReturn(status("APPROVED", 1));

        var result = controller.status(300L);

        assertThat(result.getData().reviewStatus()).isEqualTo("APPROVED");
        assertThat(result.getData().latestTask().resultCode())
                .isEqualTo("AUTO_RESULT");
        verify(service).getStatus(300L);
    }

    private static ArticleReviewStatusView status(
            String reviewStatus,
            int lockVersion
    ) {
        LocalDateTime now = LocalDateTime.now();
        return new ArticleReviewStatusView(
                300L,
                "PENDING_REVIEW",
                reviewStatus,
                500L,
                500L,
                lockVersion,
                new ArticleReviewTaskView(
                        600L,
                        500L,
                        "PLATFORM_AUTO",
                        "AUTO",
                        reviewStatus,
                        "LOW",
                        "AUTO_RESULT",
                        "结果",
                        0,
                        now,
                        null,
                        now
                )
        );
    }
}

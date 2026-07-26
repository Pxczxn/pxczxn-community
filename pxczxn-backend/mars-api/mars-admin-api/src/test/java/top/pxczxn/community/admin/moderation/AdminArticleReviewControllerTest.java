package top.pxczxn.community.admin.moderation;

import cn.dev33.satoken.stp.StpUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import top.pxczxn.community.moderation.application.AdminArticleReviewContentView;
import top.pxczxn.community.moderation.application.AdminArticleReviewDetailView;
import top.pxczxn.community.moderation.application.AdminArticleReviewListItemView;
import top.pxczxn.community.moderation.application.AdminArticleReviewPageView;
import top.pxczxn.community.moderation.application.AdminArticleReviewQuery;
import top.pxczxn.community.moderation.application.AdminArticleReviewService;
import top.pxczxn.community.moderation.application.ClaimArticleReviewCommand;
import top.pxczxn.community.moderation.application.DecideArticleReviewCommand;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminArticleReviewControllerTest {

    private AdminArticleReviewService service;
    private AdminArticleReviewController controller;

    @BeforeEach
    void setUp() {
        service = mock(AdminArticleReviewService.class);
        controller = new AdminArticleReviewController(service);
    }

    @Test
    void pageMapsSnowflakeIdsToStrings() {
        LocalDateTime now = LocalDateTime.now();
        var query = new AdminArticleReviewQuery(
                "QUEUED",
                "HIGH",
                null,
                null,
                1,
                20
        );
        when(service.page(query)).thenReturn(new AdminArticleReviewPageView(
                List.of(new AdminArticleReviewListItemView(
                        600L,
                        300L,
                        500L,
                        "文章",
                        100L,
                        "作者",
                        200L,
                        "博客",
                        "QUEUED",
                        "HIGH",
                        "PLATFORM_MANUAL",
                        "MANUAL",
                        null,
                        "AUTO_ESCALATED_KEYWORD",
                        0,
                        now,
                        null,
                        null
                )),
                1,
                1,
                20
        ));

        var result = controller.page(
                "QUEUED",
                "HIGH",
                null,
                null,
                1,
                20
        );

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData().getList().getFirst().taskId())
                .isEqualTo("600");
        assertThat(result.getData().getTotal()).isEqualTo(1);
        verify(service).page(query);
    }

    @Test
    void detailReturnsOnlyMappedFixedContentSnapshot() {
        when(service.detail(600L)).thenReturn(detail("QUEUED", 0));

        var result = controller.detail(600L);

        assertThat(result.getData().fixedVersionId()).isEqualTo("500");
        assertThat(result.getData().content().versionId()).isEqualTo("500");
        assertThat(result.getData().content().markdownContent())
                .isEqualTo("# fixed");
    }

    @Test
    void claimPassesCurrentAdminAndTaskLock() {
        when(service.claim(
                600L,
                9L,
                new ClaimArticleReviewCommand(0)
        )).thenReturn(detail("MANUAL_REVIEWING", 1));

        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(9L);

            var result = controller.claim(
                    600L,
                    new ClaimAdminArticleReviewRequest(0)
            );

            assertThat(result.getData().taskStatus())
                    .isEqualTo("MANUAL_REVIEWING");
        }
        verify(service).claim(
                600L,
                9L,
                new ClaimArticleReviewCommand(0)
        );
    }

    @Test
    void revisionDelegatesReasonAndCurrentAdmin() {
        when(service.requestRevision(
                600L,
                9L,
                new DecideArticleReviewCommand(1, "请补充来源")
        )).thenReturn(detail("REVISION_REQUIRED", 2));

        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(9L);

            var result = controller.requestRevision(
                    600L,
                    new DecideAdminArticleReviewRequest(1, "请补充来源")
            );

            assertThat(result.getData().taskStatus())
                    .isEqualTo("REVISION_REQUIRED");
        }
        verify(service).requestRevision(
                600L,
                9L,
                new DecideArticleReviewCommand(1, "请补充来源")
        );
    }

    private static AdminArticleReviewDetailView detail(
            String status,
            int taskLock
    ) {
        LocalDateTime now = LocalDateTime.now();
        return new AdminArticleReviewDetailView(
                600L,
                300L,
                500L,
                "文章",
                "摘要",
                "PUBLIC",
                "PENDING_REVIEW",
                status,
                100L,
                "author",
                "作者",
                200L,
                "博客",
                "blog",
                status,
                "HIGH",
                "PLATFORM_MANUAL",
                "MANUAL",
                100L,
                "QUEUED".equals(status) ? null : 9L,
                "AUTO_ESCALATED_KEYWORD",
                "ruleIds=[10]",
                taskLock,
                3,
                now,
                null,
                null,
                new AdminArticleReviewContentView(
                        500L,
                        2,
                        "MARKDOWN",
                        null,
                        "# fixed",
                        "<h1>fixed</h1>",
                        "fixed",
                        "[]",
                        "hash",
                        1,
                        1
                )
        );
    }
}

package top.pxczxn.community.admin.query;

import top.pxczxn.platform.common.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import top.pxczxn.community.admin.application.AdminCommunityArticleView;
import top.pxczxn.community.admin.application.AdminCommunityDailyMetricView;
import top.pxczxn.community.admin.application.AdminCommunityDashboardView;
import top.pxczxn.community.admin.application.AdminCommunityPageView;
import top.pxczxn.community.admin.application.AdminCommunityQueryService;
import top.pxczxn.community.admin.application.AdminCommunityUserView;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminCommunityQueryControllerTest {

    private AdminCommunityQueryService service;
    private AdminCommunityQueryController controller;

    @BeforeEach
    void setUp() {
        service = mock(AdminCommunityQueryService.class);
        controller = new AdminCommunityQueryController(service);
    }

    @Test
    void dashboardMapsSevenDayMetrics() {
        var today = LocalDate.now();
        when(service.dashboard()).thenReturn(new AdminCommunityDashboardView(
                20, 18, 4, 32, 16, 2, 1, 5,
                List.of(new AdminCommunityDailyMetricView(today, 3, 7))
        ));

        var result = controller.dashboard();

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData().activeUserCount()).isEqualTo(18);
        assertThat(result.getData().dailyMetrics()).singleElement()
                .satisfies(metric -> {
                    assertThat(metric.date()).isEqualTo(today);
                    assertThat(metric.articleCount()).isEqualTo(7);
                });
    }

    @Test
    void usersMapSnowflakeIdsToStringsAndPreservePaging() {
        var now = LocalDateTime.now();
        when(service.users("eva", "NORMAL", null, 2, 10))
                .thenReturn(new AdminCommunityPageView<>(
                        List.of(new AdminCommunityUserView(
                                9007199254740993L,
                                "eva",
                                "Eva",
                                "eva@example.com",
                                "NORMAL",
                                "VERIFIED",
                                9007199254740995L,
                                "AI 探索者团队",
                                now,
                                now
                        )),
                        21,
                        2,
                        10
                ));

        var result = controller.users(
                "eva", "NORMAL", null, 2, 10
        );

        assertThat(result.getData().getList().getFirst().id())
                .isEqualTo("9007199254740993");
        assertThat(result.getData().getList().getFirst().personalBlogId())
                .isEqualTo("9007199254740995");
        assertThat(result.getData().getTotal()).isEqualTo(21);
        assertThat(result.getData().getPage()).isEqualTo(2);
    }

    @Test
    void articleReturnsSafeRenderedSnapshotAndStringIds() {
        var now = LocalDateTime.now();
        when(service.article(300L)).thenReturn(new AdminCommunityArticleView(
                300L, 200L, "AI 探索者团队", "ai-team",
                100L, "eva", 400L, "工程实践",
                "Agent 架构实践", "agent-architecture", "摘要",
                "MARKDOWN", "PUBLIC", "MANUAL", "PUBLISHED",
                "APPROVED", 500L, 500L, 500L, 3,
                "<p>safe</p>", "[]", 3287, 9,
                List.of("AI", "架构"), null, now,
                "/ai-team/300/agent-architecture",
                1200, 68, 22, 18, 4, now, now
        ));

        var result = controller.article(300L);

        assertThat(result.getData().id()).isEqualTo("300");
        assertThat(result.getData().currentVersionId()).isEqualTo("500");
        assertThat(result.getData().renderedHtml()).isEqualTo("<p>safe</p>");
        assertThat(result.getData().tags()).containsExactly("AI", "架构");
        verify(service).article(300L);
    }

    @Test
    void articlesRejectInvalidBlogIdBeforeQuery() {
        assertThatThrownBy(() -> controller.articles(
                null, null, null, null,
                "not-an-id", 1, 20
        ))
                .isInstanceOf(BusinessException.class)
                .hasMessage("博客 ID 无效");
    }
}

package top.pxczxn.community.admin.governance;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.mars.common.exception.BusinessException;
import com.mars.system.annotation.Log;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import top.pxczxn.community.admin.application.AdminCommunityCommentView;
import top.pxczxn.community.admin.application.AdminCommunityInteractionView;
import top.pxczxn.community.admin.application.AdminCommunityPageView;
import top.pxczxn.community.admin.application.AdminInteractionGovernanceService;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminInteractionGovernanceControllerTest {

    private AdminInteractionGovernanceService service;
    private AdminInteractionGovernanceController controller;

    @BeforeEach
    void setUp() {
        service = mock(AdminInteractionGovernanceService.class);
        controller = new AdminInteractionGovernanceController(service);
    }

    @Test
    void commentsMapSnowflakeIdsWithoutJavaScriptPrecisionLoss() {
        LocalDateTime now = LocalDateTime.now();
        when(service.comments(
                "agent", "PENDING_REVIEW", "MOMENT",
                200L, 300L, 2, 10
        )).thenReturn(new AdminCommunityPageView<>(
                List.of(new AdminCommunityCommentView(
                        9007199254740993L,
                        9007199254740995L,
                        "eva",
                        "Eva",
                        "MOMENT",
                        9007199254740997L,
                        "Agent 动态",
                        null,
                        null,
                        null,
                        "待审核评论",
                        "<p>待审核评论</p>",
                        "PENDING_REVIEW",
                        0,
                        3,
                        1,
                        now,
                        now,
                        null
                )),
                21,
                2,
                10
        ));

        var result = controller.comments(
                "agent",
                "PENDING_REVIEW",
                "MOMENT",
                "200",
                "300",
                2,
                10
        );

        assertThat(result.getData().getTotal()).isEqualTo(21);
        assertThat(result.getData().getList()).singleElement()
                .satisfies(row -> {
                    assertThat(row.id())
                            .isEqualTo("9007199254740993");
                    assertThat(row.authorUserId())
                            .isEqualTo("9007199254740995");
                    assertThat(row.targetId())
                            .isEqualTo("9007199254740997");
                });
    }

    @Test
    void interactionsRequireValidStringIdsBeforeServiceCall() {
        assertThatThrownBy(() -> controller.interactions(
                "LIKE", "ARTICLE", "not-an-id",
                null, 1, 20
        ))
                .isInstanceOf(BusinessException.class)
                .hasMessage("目标 ID 无效");
    }

    @Test
    void interactionPagePreservesFollowerPrivacyBoundary() {
        LocalDateTime now = LocalDateTime.now();
        when(service.interactions(
                "FOLLOW", "BLOG", 500L, null, 1, 20
        )).thenReturn(new AdminCommunityPageView<>(
                List.of(new AdminCommunityInteractionView(
                        1L,
                        "FOLLOW",
                        300L,
                        "eva",
                        "Eva",
                        "BLOG",
                        500L,
                        "AI 探索者",
                        "IMPORTANT",
                        true,
                        now,
                        now
                )),
                1,
                1,
                20
        ));

        var result = controller.interactions(
                "FOLLOW", "BLOG", "500",
                null, 1, 20
        );

        assertThat(result.getData().getList()).singleElement()
                .satisfies(row -> {
                    assertThat(row.notificationLevel())
                            .isEqualTo("IMPORTANT");
                    assertThat(row.specialFollow()).isTrue();
                });
        verify(service).interactions(
                "FOLLOW", "BLOG", 500L, null, 1, 20
        );
    }

    @Test
    void destructiveEndpointsCarrySpecificPermissionAndOperationLog()
            throws Exception {
        Method commentTakeDown =
                AdminInteractionGovernanceController.class
                        .getDeclaredMethod(
                                "takeDownComment",
                                Long.class,
                                AdminGovernanceRequest.class
                        );
        Method momentRestore =
                AdminInteractionGovernanceController.class
                        .getDeclaredMethod(
                                "restoreMoment",
                                Long.class,
                                AdminGovernanceRequest.class
                        );

        assertThat(commentTakeDown
                .getAnnotation(SaCheckPermission.class).value())
                .containsExactly("community:comment:takeDown");
        assertThat(momentRestore
                .getAnnotation(SaCheckPermission.class).value())
                .containsExactly("community:moment:restore");
        assertThat(commentTakeDown.getAnnotation(Log.class)).isNotNull();
        assertThat(momentRestore.getAnnotation(Log.class)).isNotNull();
    }
}

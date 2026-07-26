package top.pxczxn.community.web.social;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import top.pxczxn.community.social.application.CommentAuthorView;
import top.pxczxn.community.social.application.CommentModerationView;
import top.pxczxn.community.social.application.CommentPageView;
import top.pxczxn.community.social.application.CommentReplyPageView;
import top.pxczxn.community.social.application.CommentService;
import top.pxczxn.community.social.application.CommentThreadView;
import top.pxczxn.community.social.application.CommentView;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CommentControllerTest {

    private CommentService service;
    private CommentController controller;

    @BeforeEach
    void setUp() {
        service = mock(CommentService.class);
        controller = new CommentController(service);
    }

    @Test
    void createSerializesEveryBusinessIdAsString() {
        when(service.create("ARTICLE", 400L, "内容"))
                .thenReturn(comment(500L));

        var result = controller.create(
                "ARTICLE",
                400L,
                new CreateCommentRequest("内容")
        );

        assertThat(result.getData().commentId()).isEqualTo("500");
        assertThat(result.getData().targetId()).isEqualTo("400");
        assertThat(result.getData().rootCommentId()).isEqualTo("501");
        assertThat(result.getData().parentCommentId()).isEqualTo("502");
        assertThat(result.getData().replyToUserId()).isEqualTo("101");
        assertThat(result.getData().author().userId()).isEqualTo("100");
        assertThat(result.getData().author().avatarFileId())
                .isEqualTo("600");
    }

    @Test
    void pageMapsThreadAndReplyPreview() {
        when(service.page("ARTICLE", 400L, 1, 20))
                .thenReturn(new CommentPageView(
                        "ARTICLE",
                        400L,
                        List.of(new CommentThreadView(
                                comment(500L),
                                List.of(comment(501L)),
                                1
                        )),
                        1,
                        2,
                        1,
                        20
                ));

        var result = controller.page("ARTICLE", 400L, 1, 20);

        assertThat(result.getData().targetId()).isEqualTo("400");
        assertThat(result.getData().records()).hasSize(1);
        assertThat(result.getData().records().getFirst().replyPreview())
                .hasSize(1);
        assertThat(result.getData().commentCount()).isEqualTo(2);
    }

    @Test
    void replyAndReplyPageMapRootId() {
        when(service.reply(500L, "回复"))
                .thenReturn(comment(501L));
        when(service.replies(500L, 1, 20))
                .thenReturn(new CommentReplyPageView(
                        500L,
                        List.of(comment(501L)),
                        1,
                        1,
                        20
                ));

        var created = controller.reply(
                500L, new CreateCommentRequest("回复")
        );
        var page = controller.replies(500L, 1, 20);

        assertThat(created.getData().commentId()).isEqualTo("501");
        assertThat(page.getData().rootCommentId()).isEqualTo("500");
        verify(service).reply(500L, "回复");
    }

    @Test
    void deleteAndHideMapModerationCounters() {
        when(service.delete(500L)).thenReturn(
                new CommentModerationView(
                        500L, "DELETED_BY_USER", 1, 3
                )
        );
        when(service.hide(501L, "离题")).thenReturn(
                new CommentModerationView(
                        501L, "HIDDEN_BY_AUTHOR", 2, 1
                )
        );

        var deleted = controller.delete(500L);
        var hidden = controller.hide(
                501L, new HideCommentRequest("离题")
        );

        assertThat(deleted.getData().affectedComments()).isEqualTo(1);
        assertThat(deleted.getData().targetCommentCount()).isEqualTo(3);
        assertThat(hidden.getData().affectedComments()).isEqualTo(2);
        assertThat(hidden.getData().targetCommentCount()).isEqualTo(1);
    }

    private static CommentView comment(Long id) {
        return new CommentView(
                id,
                "ARTICLE",
                400L,
                501L,
                502L,
                101L,
                new CommentAuthorView(
                        100L,
                        "xingyu",
                        "星语用户",
                        600L
                ),
                "内容",
                "<p>内容</p>",
                "PUBLISHED",
                false,
                2,
                true,
                false,
                "AUTO_APPROVED",
                LocalDateTime.of(2026, 7, 25, 12, 0),
                null
        );
    }
}

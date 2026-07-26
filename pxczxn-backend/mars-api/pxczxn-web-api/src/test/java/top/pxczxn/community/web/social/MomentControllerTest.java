package top.pxczxn.community.web.social;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import top.pxczxn.community.social.application.MomentArticleView;
import top.pxczxn.community.social.application.MomentAuthorView;
import top.pxczxn.community.social.application.MomentBlogView;
import top.pxczxn.community.social.application.MomentDeletionView;
import top.pxczxn.community.social.application.MomentPageView;
import top.pxczxn.community.social.application.MomentPublishView;
import top.pxczxn.community.social.application.MomentService;
import top.pxczxn.community.social.application.MomentShareLinkView;
import top.pxczxn.community.social.application.MomentSourceView;
import top.pxczxn.community.social.application.MomentView;
import top.pxczxn.community.social.application.PublishMomentCommand;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MomentControllerTest {

    private MomentService service;
    private MomentController controller;

    @BeforeEach
    void setUp() {
        service = mock(MomentService.class);
        controller = new MomentController(service);
    }

    @Test
    void publishParsesIdsAndSerializesNestedBusinessIds() {
        PublishMomentCommand command = new PublishMomentCommand(
                300L,
                "QUOTE",
                "观点",
                null,
                null,
                400L,
                "PUBLIC"
        );
        when(service.publish(command)).thenReturn(
                new MomentPublishView(moment(), false, "AUTO_APPROVED")
        );

        var result = controller.publish(new PublishMomentRequest(
                "300",
                "QUOTE",
                "观点",
                null,
                null,
                "400",
                "PUBLIC"
        ));

        MomentResponse response = result.getData().moment();
        assertThat(response.momentId()).isEqualTo("500");
        assertThat(response.author().userId()).isEqualTo("100");
        assertThat(response.blog().blogId()).isEqualTo("300");
        assertThat(response.article().articleId()).isEqualTo("200");
        assertThat(response.repostSource().momentId()).isEqualTo("400");
        assertThat(response.repostSource().repostMomentId())
                .isEqualTo("399");
        verify(service).publish(command);
    }

    @Test
    void publicAndMinePagesMapRecords() {
        MomentPageView page = new MomentPageView(
                List.of(moment()), 1, 1, 20
        );
        when(service.publicPage(null, 1, 20)).thenReturn(page);
        when(service.mine(1, 20)).thenReturn(page);

        var publicResult = controller.publicFeed(1, 20);
        var mineResult = controller.mine(1, 20);

        assertThat(publicResult.getData().records()).hasSize(1);
        assertThat(mineResult.getData().total()).isEqualTo(1);
    }

    @Test
    void deleteMapsLockAndIdempotentState() {
        when(service.delete(500L, 0)).thenReturn(
                new MomentDeletionView(500L, "DELETED", 1, false, 2)
        );

        var result = controller.delete(500L, 0);

        assertThat(result.getData().momentId()).isEqualTo("500");
        assertThat(result.getData().lockVersion()).isEqualTo(1);
        assertThat(result.getData().sourceRepostCount()).isEqualTo(2);
    }

    @Test
    void shareLinkReturnsStableCanonicalPath() {
        when(service.shareLink(500L)).thenReturn(
                new MomentShareLinkView(500L, "/moments/500")
        );

        var result = controller.shareLink(500L);

        assertThat(result.getData().momentId()).isEqualTo("500");
        assertThat(result.getData().canonicalPath())
                .isEqualTo("/moments/500");
    }

    private static MomentView moment() {
        LocalDateTime now = LocalDateTime.of(2026, 7, 26, 0, 0);
        return new MomentView(
                500L,
                "QUOTE",
                new MomentAuthorView(
                        100L, "xingyu", "星语用户", 101L
                ),
                new MomentBlogView(
                        300L, "PERSONAL", "星语博客", "xingyu-blog", 301L
                ),
                "观点",
                "<p>观点</p>",
                null,
                new MomentArticleView(
                        200L,
                        true,
                        "文章",
                        "摘要",
                        201L,
                        "/blog/article"
                ),
                new MomentSourceView(
                        400L,
                        true,
                        "TEXT",
                        new MomentAuthorView(
                                102L, "source", "源作者", 103L
                        ),
                        new MomentBlogView(
                                302L,
                                "PERSONAL",
                                "源博客",
                                "source-blog",
                                303L
                        ),
                        "源动态",
                        "<p>源动态</p>",
                        null,
                        null,
                        399L,
                        now
                ),
                "PUBLIC",
                "PUBLISHED",
                1,
                2,
                3,
                4,
                true,
                false,
                "/moments/500",
                0,
                now,
                now
        );
    }
}

package top.pxczxn.community.web.social;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import top.pxczxn.community.social.application.ContentLikeRelationshipView;
import top.pxczxn.community.social.application.ContentLikeService;
import top.pxczxn.community.social.application.LikedContentPageView;
import top.pxczxn.community.social.application.LikedContentView;
import top.pxczxn.community.social.application.LikeListPrivacyService;
import top.pxczxn.community.social.application.LikeListPrivacyView;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ContentLikeControllerTest {

    private ContentLikeService service;
    private LikeListPrivacyService privacyService;
    private ContentLikeController controller;

    @BeforeEach
    void setUp() {
        service = mock(ContentLikeService.class);
        privacyService = mock(LikeListPrivacyService.class);
        controller = new ContentLikeController(service, privacyService);
    }

    @Test
    void likeMapsTargetIdAsString() {
        when(service.like("article", 300L)).thenReturn(
                new ContentLikeRelationshipView(
                        "ARTICLE", 300L, true, 8
                )
        );

        var result = controller.like("article", 300L);

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData().targetType()).isEqualTo("ARTICLE");
        assertThat(result.getData().targetId()).isEqualTo("300");
        assertThat(result.getData().liked()).isTrue();
        assertThat(result.getData().likeCount()).isEqualTo(8);
        verify(service).like("article", 300L);
    }

    @Test
    void unlikeMapsIdempotentResult() {
        when(service.unlike("COMMENT", 301L)).thenReturn(
                new ContentLikeRelationshipView(
                        "COMMENT", 301L, false, 0
                )
        );

        var result = controller.unlike("COMMENT", 301L);

        assertThat(result.getData().liked()).isFalse();
        assertThat(result.getData().likeCount()).isZero();
    }

    @Test
    void likedPageMapsEveryBusinessIdAsString() {
        LocalDateTime likedAt = LocalDateTime.now();
        when(service.myLikes("ARTICLE", 1, 20)).thenReturn(
                new LikedContentPageView(
                        List.of(new LikedContentView(
                                900L,
                                "ARTICLE",
                                300L,
                                100L,
                                200L,
                                "文章",
                                "摘要",
                                400L,
                                "/blog/300/article",
                                8,
                                likedAt
                        )),
                        1,
                        1,
                        20
                )
        );

        var result = controller.myLikes("ARTICLE", 1, 20);

        assertThat(result.getData().records()).hasSize(1);
        LikedContentResponse content = result.getData().records().getFirst();
        assertThat(content.likeId()).isEqualTo("900");
        assertThat(content.targetId()).isEqualTo("300");
        assertThat(content.authorUserId()).isEqualTo("100");
        assertThat(content.blogId()).isEqualTo("200");
        assertThat(content.coverFileId()).isEqualTo("400");
    }

    @Test
    void privacyUpdateMapsOwnerIdAndVisibility() {
        when(privacyService.update("PUBLIC")).thenReturn(
                new LikeListPrivacyView(100L, "PUBLIC")
        );

        var result = controller.updateLikeListPrivacy(
                new UpdateLikeListPrivacyRequest("PUBLIC")
        );

        assertThat(result.getData().ownerUserId()).isEqualTo("100");
        assertThat(result.getData().visibility()).isEqualTo("PUBLIC");
        verify(privacyService).update("PUBLIC");
    }
}

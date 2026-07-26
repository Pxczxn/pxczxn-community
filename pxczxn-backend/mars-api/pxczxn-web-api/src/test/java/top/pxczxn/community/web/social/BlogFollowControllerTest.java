package top.pxczxn.community.web.social;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import top.pxczxn.community.social.application.BlogFollowRelationshipView;
import top.pxczxn.community.social.application.BlogFollowService;
import top.pxczxn.community.social.application.UpdateBlogFollowCommand;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BlogFollowControllerTest {

    private BlogFollowService service;
    private BlogFollowController controller;

    @BeforeEach
    void setUp() {
        service = mock(BlogFollowService.class);
        controller = new BlogFollowController(service);
    }

    @Test
    void followMapsSettingsAndSnowflakeId() {
        when(service.follow(
                300L, new UpdateBlogFollowCommand("IMPORTANT", true)
        )).thenReturn(new BlogFollowRelationshipView(
                300L, true, true, true, true, "IMPORTANT", 12
        ));

        var result = controller.follow(
                300L, new UpdateBlogFollowRequest("IMPORTANT", true)
        );

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData().blogId()).isEqualTo("300");
        assertThat(result.getData().mutual()).isTrue();
        assertThat(result.getData().notificationLevel())
                .isEqualTo("IMPORTANT");
        verify(service).follow(
                300L, new UpdateBlogFollowCommand("IMPORTANT", true)
        );
    }

    @Test
    void emptyFollowBodyUsesServiceDefaults() {
        when(service.follow(300L, null)).thenReturn(
                new BlogFollowRelationshipView(
                        300L, true, false, false, false, "ALL", 1
                )
        );

        var result = controller.follow(300L, null);

        assertThat(result.getData().specialFollow()).isFalse();
        verify(service).follow(300L, null);
    }
}

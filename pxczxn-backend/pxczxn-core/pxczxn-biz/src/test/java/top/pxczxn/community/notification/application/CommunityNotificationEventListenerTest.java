package top.pxczxn.community.notification.application;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

class CommunityNotificationEventListenerTest {

    @Test
    void postCommitNotificationFailureDoesNotEscapeToBusinessRequest() {
        CommunityNotificationDispatchService dispatch =
                mock(CommunityNotificationDispatchService.class);
        doThrow(new IllegalStateException("notification storage offline"))
                .when(dispatch)
                .createDirect(any());
        CommunityNotificationEventListener listener =
                new CommunityNotificationEventListener(dispatch);

        assertThatCode(() -> listener.onDirect(
                new CommunityNotificationEvent(
                        "LIKE",
                        "INTERACTION",
                        1L,
                        2L,
                        "ARTICLE",
                        3L,
                        "有人点赞了你的内容",
                        "有人点赞了你发布的内容。",
                        "like:ARTICLE:3",
                        "NORMAL"
                )
        )).doesNotThrowAnyException();
    }
}

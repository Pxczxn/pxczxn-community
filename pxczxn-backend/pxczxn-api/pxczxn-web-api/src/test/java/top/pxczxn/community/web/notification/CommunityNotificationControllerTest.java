package top.pxczxn.community.web.notification;

import top.pxczxn.platform.common.result.Result;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import top.pxczxn.community.notification.application.CommunityNotificationInboxService;
import top.pxczxn.community.notification.application.NotificationPageView;
import top.pxczxn.community.notification.application.NotificationReadAllView;
import top.pxczxn.community.notification.application.NotificationReadView;
import top.pxczxn.community.notification.application.NotificationSenderView;
import top.pxczxn.community.notification.application.NotificationView;
import top.pxczxn.community.notification.application.UnreadNotificationCountView;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CommunityNotificationControllerTest {

    private CommunityNotificationInboxService service;
    private CommunityNotificationController controller;

    @BeforeEach
    void setUp() {
        service = mock(CommunityNotificationInboxService.class);
        controller = new CommunityNotificationController(service);
    }

    @Test
    void pageSerializesEveryBusinessIdAsString() {
        when(service.page("COMMENT", "UNREAD", 1, 20))
                .thenReturn(new NotificationPageView(
                        List.of(notification()),
                        1,
                        1,
                        20
                ));

        Result<NotificationPageResponse> result = controller.page(
                "COMMENT", "UNREAD", 1, 20
        );

        NotificationResponse response =
                result.getData().records().getFirst();
        assertThat(response.notificationId()).isEqualTo("1");
        assertThat(response.sender().userId()).isEqualTo("2");
        assertThat(response.sender().avatarFileId()).isEqualTo("3");
        assertThat(response.targetId()).isEqualTo("4");
    }

    @Test
    void unreadCountPreservesCategoryBreakdown() {
        when(service.unreadCount()).thenReturn(
                new UnreadNotificationCountView(
                        3, Map.of("COMMENT", 2L, "REVIEW", 1L)
                )
        );

        Result<UnreadNotificationCountResponse> result =
                controller.unreadCount();

        assertThat(result.getData().total()).isEqualTo(3);
        assertThat(result.getData().categories())
                .containsEntry("COMMENT", 2L);
    }

    @Test
    void readEndpointsDelegateIdempotentStateChanges() {
        LocalDateTime now = LocalDateTime.now();
        when(service.read(1L)).thenReturn(
                new NotificationReadView(
                        1L, "READ", now, true, 2
                )
        );
        when(service.readAll("COMMENT")).thenReturn(
                new NotificationReadAllView("COMMENT", 2, 0)
        );

        NotificationReadResponse single =
                controller.read(1L).getData();
        NotificationReadAllResponse all =
                controller.readAll("COMMENT").getData();

        assertThat(single.notificationId()).isEqualTo("1");
        assertThat(single.idempotentReplay()).isTrue();
        assertThat(all.affectedNotifications()).isEqualTo(2);
        verify(service).read(1L);
        verify(service).readAll("COMMENT");
    }

    private static NotificationView notification() {
        LocalDateTime now = LocalDateTime.now();
        return new NotificationView(
                1L,
                "COMMENT",
                "COMMENT",
                "NORMAL",
                new NotificationSenderView(
                        2L, "sender", "发送者", 3L
                ),
                "有人评论了你的内容",
                "有人评论了你发布的内容。",
                "ARTICLE",
                4L,
                true,
                "/articles/4",
                1,
                "UNREAD",
                null,
                now,
                now
        );
    }
}

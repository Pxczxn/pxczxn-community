package top.pxczxn.community.web.notification;

import top.pxczxn.community.notification.application.NotificationReadView;

import java.time.LocalDateTime;

public record NotificationReadResponse(
        String notificationId,
        String status,
        LocalDateTime readAt,
        boolean idempotentReplay,
        long unreadCount
) {

    static NotificationReadResponse from(NotificationReadView view) {
        return new NotificationReadResponse(
                view.notificationId().toString(),
                view.status(),
                view.readAt(),
                view.idempotentReplay(),
                view.unreadCount()
        );
    }
}

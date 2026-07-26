package top.pxczxn.community.notification.application;

import java.time.LocalDateTime;

public record NotificationReadView(
        Long notificationId,
        String status,
        LocalDateTime readAt,
        boolean idempotentReplay,
        long unreadCount
) {
}

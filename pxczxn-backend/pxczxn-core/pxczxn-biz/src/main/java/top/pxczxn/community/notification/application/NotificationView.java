package top.pxczxn.community.notification.application;

import java.time.LocalDateTime;

public record NotificationView(
        Long notificationId,
        String notificationType,
        String category,
        String importance,
        NotificationSenderView sender,
        String title,
        String content,
        String targetType,
        Long targetId,
        boolean targetAvailable,
        String canonicalPath,
        int aggregateCount,
        String status,
        LocalDateTime readAt,
        LocalDateTime createdAt,
        LocalDateTime lastActivityAt
) {
}

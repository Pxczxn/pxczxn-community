package top.pxczxn.community.web.notification;

import top.pxczxn.community.notification.application.NotificationView;

import java.time.LocalDateTime;

public record NotificationResponse(
        String notificationId,
        String notificationType,
        String category,
        String importance,
        NotificationSenderResponse sender,
        String title,
        String content,
        String targetType,
        String targetId,
        boolean targetAvailable,
        String canonicalPath,
        int aggregateCount,
        String status,
        LocalDateTime readAt,
        LocalDateTime createdAt,
        LocalDateTime lastActivityAt
) {

    static NotificationResponse from(NotificationView view) {
        return new NotificationResponse(
                view.notificationId().toString(),
                view.notificationType(),
                view.category(),
                view.importance(),
                NotificationSenderResponse.from(view.sender()),
                view.title(),
                view.content(),
                view.targetType(),
                string(view.targetId()),
                view.targetAvailable(),
                view.canonicalPath(),
                view.aggregateCount(),
                view.status(),
                view.readAt(),
                view.createdAt(),
                view.lastActivityAt()
        );
    }

    private static String string(Long value) {
        return value == null ? null : value.toString();
    }
}

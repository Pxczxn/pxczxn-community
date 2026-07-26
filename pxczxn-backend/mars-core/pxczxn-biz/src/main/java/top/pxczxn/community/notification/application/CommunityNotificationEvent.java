package top.pxczxn.community.notification.application;

public record CommunityNotificationEvent(
        String notificationType,
        String category,
        Long senderUserId,
        Long recipientUserId,
        String targetType,
        Long targetId,
        String title,
        String content,
        String aggregateKey,
        String importance
) {
}

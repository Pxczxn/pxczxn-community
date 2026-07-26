package top.pxczxn.community.notification.application;

public record MomentPublishedNotificationEvent(
        Long momentId,
        Long blogId,
        Long actorUserId,
        String momentType,
        String visibility
) {
}

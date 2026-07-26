package top.pxczxn.community.notification.application;

public record NotificationSenderView(
        Long userId,
        String username,
        String displayName,
        Long avatarFileId
) {
}

package top.pxczxn.community.notification.application;

public record NotificationReadAllView(
        String category,
        int affectedNotifications,
        long unreadCount
) {
}

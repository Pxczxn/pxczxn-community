package top.pxczxn.community.web.notification;

import top.pxczxn.community.notification.application.NotificationReadAllView;

public record NotificationReadAllResponse(
        String category,
        int affectedNotifications,
        long unreadCount
) {

    static NotificationReadAllResponse from(NotificationReadAllView view) {
        return new NotificationReadAllResponse(
                view.category(),
                view.affectedNotifications(),
                view.unreadCount()
        );
    }
}

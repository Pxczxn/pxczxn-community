package top.pxczxn.community.web.notification;

import top.pxczxn.community.notification.application.UnreadNotificationCountView;

import java.util.Map;

public record UnreadNotificationCountResponse(
        long total,
        Map<String, Long> categories
) {

    static UnreadNotificationCountResponse from(
            UnreadNotificationCountView view
    ) {
        return new UnreadNotificationCountResponse(
                view.total(), view.categories()
        );
    }
}

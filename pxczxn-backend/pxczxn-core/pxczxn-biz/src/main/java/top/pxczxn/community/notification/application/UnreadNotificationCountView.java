package top.pxczxn.community.notification.application;

import java.util.Map;

public record UnreadNotificationCountView(
        long total,
        Map<String, Long> categories
) {
}

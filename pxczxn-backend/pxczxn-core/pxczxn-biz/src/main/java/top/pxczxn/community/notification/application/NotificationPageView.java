package top.pxczxn.community.notification.application;

import java.util.List;

public record NotificationPageView(
        List<NotificationView> records,
        long total,
        int pageNum,
        int pageSize
) {
}

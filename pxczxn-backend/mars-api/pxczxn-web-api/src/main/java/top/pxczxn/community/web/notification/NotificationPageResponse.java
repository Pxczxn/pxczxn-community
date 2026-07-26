package top.pxczxn.community.web.notification;

import top.pxczxn.community.notification.application.NotificationPageView;

import java.util.List;

public record NotificationPageResponse(
        List<NotificationResponse> records,
        long total,
        int pageNum,
        int pageSize
) {

    static NotificationPageResponse from(NotificationPageView view) {
        return new NotificationPageResponse(
                view.records().stream()
                        .map(NotificationResponse::from)
                        .toList(),
                view.total(),
                view.pageNum(),
                view.pageSize()
        );
    }
}

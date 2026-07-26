package top.pxczxn.community.web.notification;

import top.pxczxn.community.notification.application.NotificationSenderView;

public record NotificationSenderResponse(
        String userId,
        String username,
        String displayName,
        String avatarFileId
) {

    static NotificationSenderResponse from(NotificationSenderView view) {
        if (view == null) {
            return null;
        }
        return new NotificationSenderResponse(
                string(view.userId()),
                view.username(),
                view.displayName(),
                string(view.avatarFileId())
        );
    }

    private static String string(Long value) {
        return value == null ? null : value.toString();
    }
}

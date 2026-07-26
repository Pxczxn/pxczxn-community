package top.pxczxn.community.web.notification;

import top.pxczxn.platform.common.result.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import top.pxczxn.community.notification.application.CommunityNotificationInboxService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/notifications")
public class CommunityNotificationController {

    private final CommunityNotificationInboxService service;

    @GetMapping
    public Result<NotificationPageResponse> page(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize
    ) {
        return Result.ok(NotificationPageResponse.from(
                service.page(category, status, pageNum, pageSize)
        ));
    }

    @GetMapping("/unread-count")
    public Result<UnreadNotificationCountResponse> unreadCount() {
        return Result.ok(UnreadNotificationCountResponse.from(
                service.unreadCount()
        ));
    }

    @PatchMapping("/{notificationId}/read")
    public Result<NotificationReadResponse> read(
            @PathVariable Long notificationId
    ) {
        return Result.ok(NotificationReadResponse.from(
                service.read(notificationId)
        ));
    }

    @PatchMapping("/read-all")
    public Result<NotificationReadAllResponse> readAll(
            @RequestParam(required = false) String category
    ) {
        return Result.ok(NotificationReadAllResponse.from(
                service.readAll(category)
        ));
    }
}

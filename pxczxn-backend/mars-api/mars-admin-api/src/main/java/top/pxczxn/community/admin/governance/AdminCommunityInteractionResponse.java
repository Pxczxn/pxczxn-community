package top.pxczxn.community.admin.governance;

import top.pxczxn.community.admin.application.AdminCommunityInteractionView;

import java.time.LocalDateTime;

public record AdminCommunityInteractionResponse(
        String id,
        String interactionType,
        String actorUserId,
        String actorUsername,
        String actorDisplayName,
        String targetType,
        String targetId,
        String targetTitle,
        String notificationLevel,
        boolean specialFollow,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    static AdminCommunityInteractionResponse from(
            AdminCommunityInteractionView view
    ) {
        return new AdminCommunityInteractionResponse(
                id(view.id()),
                view.interactionType(),
                id(view.actorUserId()),
                view.actorUsername(),
                view.actorDisplayName(),
                view.targetType(),
                id(view.targetId()),
                view.targetTitle(),
                view.notificationLevel(),
                view.specialFollow(),
                view.createdAt(),
                view.updatedAt()
        );
    }

    private static String id(Long value) {
        return value == null ? null : value.toString();
    }
}

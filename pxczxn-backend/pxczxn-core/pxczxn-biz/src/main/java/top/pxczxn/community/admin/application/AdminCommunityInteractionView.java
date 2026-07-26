package top.pxczxn.community.admin.application;

import java.time.LocalDateTime;

public record AdminCommunityInteractionView(
        Long id,
        String interactionType,
        Long actorUserId,
        String actorUsername,
        String actorDisplayName,
        String targetType,
        Long targetId,
        String targetTitle,
        String notificationLevel,
        boolean specialFollow,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}

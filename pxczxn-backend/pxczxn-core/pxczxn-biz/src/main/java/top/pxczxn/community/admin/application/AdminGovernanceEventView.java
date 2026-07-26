package top.pxczxn.community.admin.application;

import java.time.LocalDateTime;

public record AdminGovernanceEventView(
        Long id,
        String subjectType,
        Long subjectId,
        String action,
        String actorType,
        Long actorUserId,
        Long actorAdminId,
        String previousStatus,
        String newStatus,
        String reason,
        String metadataJson,
        LocalDateTime createdAt
) {
}

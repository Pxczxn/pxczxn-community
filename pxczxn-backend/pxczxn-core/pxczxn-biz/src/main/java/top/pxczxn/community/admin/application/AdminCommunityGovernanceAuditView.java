package top.pxczxn.community.admin.application;

import java.time.LocalDateTime;

public record AdminCommunityGovernanceAuditView(
        String source,
        String eventType,
        String actorType,
        Long actorId,
        Long referenceId,
        LocalDateTime occurredAt
) {
}

package top.pxczxn.community.admin.governance;

import top.pxczxn.community.admin.application.AdminGovernanceEventView;

import java.time.LocalDateTime;

public record AdminGovernanceEventResponse(
        String id,
        String subjectType,
        String subjectId,
        String action,
        String actorType,
        String actorUserId,
        String actorAdminId,
        String previousStatus,
        String newStatus,
        String reason,
        String metadataJson,
        LocalDateTime createdAt
) {

    static AdminGovernanceEventResponse from(
            AdminGovernanceEventView view
    ) {
        return new AdminGovernanceEventResponse(
                id(view.id()),
                view.subjectType(),
                id(view.subjectId()),
                view.action(),
                view.actorType(),
                id(view.actorUserId()),
                id(view.actorAdminId()),
                view.previousStatus(),
                view.newStatus(),
                view.reason(),
                view.metadataJson(),
                view.createdAt()
        );
    }

    private static String id(Long value) {
        return value == null ? null : value.toString();
    }
}

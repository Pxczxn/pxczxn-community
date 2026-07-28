package top.pxczxn.community.admin.query;

import top.pxczxn.community.admin.application.AdminCommunityGovernanceAuditView;
import java.time.LocalDateTime;

public record AdminCommunityGovernanceAuditResponse(String source, String eventType, String actorType, String actorId, String referenceId, LocalDateTime occurredAt) {
    static AdminCommunityGovernanceAuditResponse from(AdminCommunityGovernanceAuditView view) { return new AdminCommunityGovernanceAuditResponse(view.source(), view.eventType(), view.actorType(), view.actorId() == null ? null : view.actorId().toString(), view.referenceId() == null ? null : view.referenceId().toString(), view.occurredAt()); }
}

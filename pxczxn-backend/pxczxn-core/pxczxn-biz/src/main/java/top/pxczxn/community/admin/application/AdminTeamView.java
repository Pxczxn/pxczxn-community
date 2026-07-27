package top.pxczxn.community.admin.application;

import java.time.LocalDateTime;
import java.util.List;

public record AdminTeamView(
        Long id, Long blogId, String name, String slug, String status,
        Long ownerUserId, String ownerUsername, int memberCount,
        Integer lockVersion, LocalDateTime createdAt, LocalDateTime updatedAt,
        List<AdminTeamMemberView> members, List<AdminTeamAuditEventView> auditEvents
) { }

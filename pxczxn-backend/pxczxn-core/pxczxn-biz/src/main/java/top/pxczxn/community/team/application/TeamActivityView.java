package top.pxczxn.community.team.application;

import java.time.LocalDateTime;

/**
 * One entry of the team activity feed, projected from the append-only {@code team_audit_event} table.
 * Raw before/after snapshots are intentionally not exposed - this is a collaboration feed, not an audit console.
 */
public record TeamActivityView(
        Long id,
        Long teamId,
        String eventType,
        String targetType,
        Long targetId,
        Long actorUserId,
        String actorDisplayName,
        String actorUsername,
        Long actorAvatarFileId,
        LocalDateTime occurredAt) {
}

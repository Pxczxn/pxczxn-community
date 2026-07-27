package top.pxczxn.community.team.application;

import java.time.LocalDateTime;

public record TeamInvitationView(
        Long id, Long teamId, Long inviteeUserId, String roleCode,
        String status, LocalDateTime expiresAt, LocalDateTime createdAt
) {
}

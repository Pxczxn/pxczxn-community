package top.pxczxn.community.team.application;

import java.time.LocalDateTime;

/**
 * Team invitation as shown to the invitee and to the team side.
 *
 * <p>Team identity and inviter identity are denormalised on purpose: an invitee is not yet a member,
 * so the client cannot resolve the team name from any list it is allowed to read. Without these
 * fields the invitation card can only render "team #123", which is what the old UI did.
 *
 * <p>Team display data lives on the linked {@code blog} row, not on {@code team}, so name, slug and
 * avatar are resolved through {@code team.blog_id}. Identifier fields stay {@code Long} because
 * {@code JacksonConfig} serialises them through {@code ToStringSerializer} for the frontend.
 */
public record TeamInvitationView(
        Long id,
        Long teamId,
        String teamName,
        String teamSlug,
        Long teamAvatarFileId,
        Long inviteeUserId,
        String inviteeDisplayName,
        String inviteeUsername,
        Long inviteeAvatarFileId,
        String roleCode,
        String status,
        Long inviterUserId,
        String inviterDisplayName,
        String inviterUsername,
        LocalDateTime expiresAt,
        LocalDateTime createdAt
) {
}

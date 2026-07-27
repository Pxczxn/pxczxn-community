package top.pxczxn.community.team.application;

public record InviteTeamMemberCommand(
        Long teamId,
        Long inviteeUserId,
        String roleCode,
        String idempotencyKey
) {
}

package top.pxczxn.community.team.application;

import java.util.List;

public interface TeamMemberService {

    TeamInvitationView invite(Long actorUserId, InviteTeamMemberCommand command);

    List<TeamInvitationView> myPendingInvitations(Long userId);

    /** Invitations this team has issued, for the member page "已邀请" list. Requires MANAGE_MEMBERS. */
    List<TeamInvitationView> teamInvitations(Long actorUserId, Long teamId);

    /** Withdraw a still-pending invitation so the invitee can be invited again later. */
    void revokeInvitation(Long actorUserId, Long teamId, Long invitationId);

    void accept(Long inviteeUserId, Long invitationId);

    void reject(Long inviteeUserId, Long invitationId);

    void leave(Long userId, Long teamId);

    void remove(Long actorUserId, Long teamId, Long memberUserId);

    void changeRole(Long actorUserId, Long teamId, Long memberUserId, String roleCode);

    void transferOwnership(Long ownerUserId, Long teamId, Long newOwnerUserId);

    void disband(Long ownerUserId, Long teamId);

    List<TeamMemberView> members(Long viewerUserId, Long teamId);
}

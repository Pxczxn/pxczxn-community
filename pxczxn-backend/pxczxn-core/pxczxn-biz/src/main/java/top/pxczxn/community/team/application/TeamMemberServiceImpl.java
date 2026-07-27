package top.pxczxn.community.team.application;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.pxczxn.community.notification.application.CommunityNotificationEvent;
import top.pxczxn.community.team.model.Team;
import top.pxczxn.community.team.model.TeamInvitation;
import top.pxczxn.community.team.model.TeamMember;
import top.pxczxn.community.team.persistence.TeamInvitationMapper;
import top.pxczxn.community.team.persistence.TeamMapper;
import top.pxczxn.community.team.persistence.TeamMemberMapper;
import top.pxczxn.community.user.model.CommunityUser;
import top.pxczxn.community.user.persistence.CommunityUserMapper;
import top.pxczxn.platform.common.exception.BusinessException;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TeamMemberServiceImpl implements TeamMemberService {

    private static final Set<String> ROLES = Set.of("OWNER", "ADMIN", "EDITOR", "AUTHOR");
    private static final Set<String> ACTIVE_USER_STATUSES = Set.of("NORMAL", "LIMITED");
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final TeamInvitationMapper invitationMapper;
    private final TeamMemberMapper memberMapper;
    private final TeamMapper teamMapper;
    private final CommunityUserMapper userMapper;
    private final TeamAuthorityService authorityService;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TeamInvitationView invite(Long actorUserId, InviteTeamMemberCommand command) {
        Long teamId = requirePositive(command.teamId(), "Invalid team ID");
        Long inviteeUserId = requirePositive(command.inviteeUserId(), "Invalid invitee user ID");
        String role = role(command.roleCode());
        requireActiveTeam(teamId);
        if (!authorityService.canManageMember(actorUserId, teamId, role)) {
            throw new BusinessException(403, "Not allowed to invite this member role");
        }
        if (actorUserId.equals(inviteeUserId) || authorityService.isMember(inviteeUserId, teamId)) {
            throw new BusinessException(409, "User is already a member or cannot invite self");
        }
        requireActiveUser(inviteeUserId);

        String idempotencyKey = normalizeKey(command.idempotencyKey());
        if (idempotencyKey != null) {
            TeamInvitation replay = invitationMapper.findByIdempotencyKey(idempotencyKey);
            if (replay != null) {
                if (!teamId.equals(replay.getTeamId()) || !inviteeUserId.equals(replay.getInviteeUserId())
                        || !role.equals(replay.getRoleCode()) || !actorUserId.equals(replay.getInvitedByUserId())) {
                    throw new BusinessException(409, "Idempotency key conflicts with an existing request");
                }
                return invitationView(replay);
            }
        }

        TeamInvitation pending = invitationMapper.findPendingForTeamAndInvitee(teamId, inviteeUserId);
        if (pending != null) {
            if (!expireIfNecessary(pending)) {
                throw new BusinessException(409, "User already has a pending invitation for this team");
            }
        }

        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        TeamInvitation invitation = new TeamInvitation();
        invitation.setTeamId(teamId);
        invitation.setInviteeUserId(inviteeUserId);
        invitation.setRoleCode(role);
        invitation.setTokenHash(hashToken());
        invitation.setIdempotencyKey(idempotencyKey);
        invitation.setStatus("PENDING");
        invitation.setInvitedByUserId(actorUserId);
        invitation.setExpiresAt(now.plusDays(7));
        invitation.setLockVersion(0);
        invitation.setCreatedAt(now);
        try {
            invitationMapper.insert(invitation);
        } catch (DuplicateKeyException exception) {
            TeamInvitation concurrent = invitationMapper.findPendingForTeamAndInvitee(teamId, inviteeUserId);
            if (concurrent != null) {
                return invitationView(concurrent);
            }
            throw exception;
        }

        authorityService.recordAuditEvent(teamId, actorUserId, "MEMBER_INVITED", "TEAM_INVITATION",
                invitation.getId(), UUID.randomUUID().toString(), null,
                "{\"inviteeUserId\":" + inviteeUserId + ",\"role\":\"" + role + "\"}");
        eventPublisher.publishEvent(new CommunityNotificationEvent(
                "TEAM_INVITATION", "SYSTEM", actorUserId, inviteeUserId, "TEAM_INVITATION", invitation.getId(),
                "Team invitation", "You have received a team member invitation", "team-invitation:" + invitation.getId(), "NORMAL"));
        return invitationView(invitation);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<TeamInvitationView> myPendingInvitations(Long userId) {
        return invitationMapper.findPendingForInvitee(userId).stream()
                .filter(invitation -> !expireIfNecessary(invitation))
                .map(this::invitationView)
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void accept(Long inviteeUserId, Long invitationId) {
        TeamInvitation invitation = pendingInvitation(inviteeUserId, invitationId);
        requireActiveTeam(invitation.getTeamId());
        requireActiveUser(inviteeUserId);
        if (authorityService.isMember(inviteeUserId, invitation.getTeamId())) {
            throw new BusinessException(409, "Already a team member");
        }
        if (invitationMapper.accept(invitationId, inviteeUserId, invitation.getLockVersion()) != 1) {
            throw new BusinessException(409, "Invitation state has changed; refresh and retry");
        }
        TeamMember member = new TeamMember();
        member.setTeamId(invitation.getTeamId());
        member.setUserId(inviteeUserId);
        member.setRoleCode(invitation.getRoleCode());
        member.setInvitedByUserId(invitation.getInvitedByUserId());
        member.setJoinedAt(LocalDateTime.now(ZoneOffset.UTC));
        member.setLockVersion(0);
        memberMapper.insert(member);
        authorityService.recordAuditEvent(invitation.getTeamId(), inviteeUserId, "INVITATION_ACCEPTED",
                "TEAM_MEMBER", member.getId(), UUID.randomUUID().toString(), null,
                "{\"role\":\"" + invitation.getRoleCode() + "\"}");
        eventPublisher.publishEvent(new CommunityNotificationEvent(
                "TEAM_INVITATION_ACCEPTED", "SYSTEM", inviteeUserId, invitation.getInvitedByUserId(),
                "TEAM_MEMBER", member.getId(), "Invitation accepted", "The invited user joined your team",
                "team-invitation-accepted:" + invitation.getId(), "NORMAL"));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reject(Long inviteeUserId, Long invitationId) {
        TeamInvitation invitation = pendingInvitation(inviteeUserId, invitationId);
        if (invitationMapper.reject(invitationId, inviteeUserId, invitation.getLockVersion()) != 1) {
            throw new BusinessException(409, "Invitation state has changed; refresh and retry");
        }
        authorityService.recordAuditEvent(invitation.getTeamId(), inviteeUserId, "INVITATION_REJECTED",
                "TEAM_INVITATION", invitationId, UUID.randomUUID().toString(), null, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void leave(Long userId, Long teamId) {
        requireActiveTeam(teamId);
        TeamMember member = activeMember(teamId, userId);
        if ("OWNER".equals(member.getRoleCode())) {
            throw new BusinessException(409, "Team owner must transfer ownership before leaving");
        }
        deactivate(member, userId, "MEMBER_LEFT");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void remove(Long actorUserId, Long teamId, Long memberUserId) {
        requireActiveTeam(teamId);
        TeamMember member = activeMember(teamId, memberUserId);
        if (!authorityService.canManageMember(actorUserId, teamId, member.getRoleCode())) {
            throw new BusinessException(403, "Not allowed to remove this member");
        }
        if (actorUserId.equals(memberUserId) || "OWNER".equals(member.getRoleCode())) {
            throw new BusinessException(409, "Cannot remove this member with this operation");
        }
        deactivate(member, actorUserId, "MEMBER_REMOVED");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changeRole(Long actorUserId, Long teamId, Long memberUserId, String roleCode) {
        requireActiveTeam(teamId);
        TeamMember member = activeMember(teamId, memberUserId);
        String role = role(roleCode);
        if ("OWNER".equals(role) || "OWNER".equals(member.getRoleCode())
                || !authorityService.canManageMember(actorUserId, teamId, member.getRoleCode())
                || !authorityService.canManageMember(actorUserId, teamId, role)) {
            throw new BusinessException(403, "Not allowed to change this member role");
        }
        if (memberMapper.updateRoleWithOptimisticLock(member.getId(), role, member.getLockVersion()) != 1) {
            throw new BusinessException(409, "Member state has changed; refresh and retry");
        }
        authorityService.recordAuditEvent(teamId, actorUserId, "MEMBER_ROLE_CHANGED", "TEAM_MEMBER",
                member.getId(), UUID.randomUUID().toString(), "{\"role\":\"" + member.getRoleCode() + "\"}",
                "{\"role\":\"" + role + "\"}");
    }

    @Override
    public void transferOwnership(Long ownerUserId, Long teamId, Long newOwnerUserId) {
        requireActiveTeam(teamId);
        authorityService.transferOwnership(teamId, ownerUserId, newOwnerUserId, UUID.randomUUID().toString());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void disband(Long ownerUserId, Long teamId) {
        Team team = requireActiveTeam(teamId);
        if (!ownerUserId.equals(team.getOwnerUserId())
                || teamMapper.disbandWithOptimisticLock(teamId, ownerUserId, team.getLockVersion()) != 1) {
            throw new BusinessException(403, "Only the team owner can disband the team");
        }
        memberMapper.leaveAllActive(teamId);
        authorityService.recordAuditEvent(teamId, ownerUserId, "TEAM_DISBANDED", "TEAM", teamId,
                UUID.randomUUID().toString(), "{\"status\":\"ACTIVE\"}", "{\"status\":\"DISBANDED\"}");
    }

    @Override
    public List<TeamMemberView> members(Long viewerUserId, Long teamId) {
        requireActiveTeam(teamId);
        if (!authorityService.isMember(viewerUserId, teamId)) {
            throw new BusinessException(403, "Not allowed to view team members");
        }
        return memberMapper.findActiveMembers(teamId).stream()
                .map(member -> new TeamMemberView(member.getUserId(), member.getRoleCode(), member.getJoinedAt()))
                .toList();
    }

    private TeamInvitation pendingInvitation(Long userId, Long invitationId) {
        TeamInvitation invitation = invitationMapper.findForInvitee(invitationId, userId);
        if (invitation == null || !"PENDING".equals(invitation.getStatus())) {
            throw new BusinessException(404, "Invitation does not exist or was already processed");
        }
        if (expireIfNecessary(invitation)) {
            throw new BusinessException(409, "Invitation has expired");
        }
        return invitation;
    }

    private TeamMember activeMember(Long teamId, Long userId) {
        TeamMember member = memberMapper.findActiveMember(requirePositive(teamId, "Invalid team ID"),
                requirePositive(userId, "Invalid user ID"));
        if (member == null) {
            throw new BusinessException(404, "Team member does not exist");
        }
        return member;
    }

    private void deactivate(TeamMember member, Long actorUserId, String eventType) {
        if (memberMapper.leaveWithOptimisticLock(member.getId(), member.getLockVersion()) != 1) {
            throw new BusinessException(409, "Member state has changed; refresh and retry");
        }
        authorityService.recordAuditEvent(member.getTeamId(), actorUserId, eventType, "TEAM_MEMBER",
                member.getId(), UUID.randomUUID().toString(), "{\"role\":\"" + member.getRoleCode() + "\"}", null);
    }

    private Team requireActiveTeam(Long teamId) {
        Team team = teamMapper.selectById(requirePositive(teamId, "Invalid team ID"));
        if (team == null || team.getDeletedAt() != null || !"ACTIVE".equals(team.getStatus())) {
            throw new BusinessException(404, "Team does not exist or has been disbanded");
        }
        return team;
    }

    private void requireActiveUser(Long userId) {
        CommunityUser user = userMapper.selectById(userId);
        if (user == null || !ACTIVE_USER_STATUSES.contains(user.getStatus())) {
            throw new BusinessException(400, "Invitee user is not active");
        }
    }

    private boolean expireIfNecessary(TeamInvitation invitation) {
        if (invitation.getExpiresAt().isAfter(LocalDateTime.now(ZoneOffset.UTC))) {
            return false;
        }
        invitationMapper.expire(invitation.getId(), invitation.getLockVersion());
        return true;
    }

    private static Long requirePositive(Long value, String message) {
        if (value == null || value <= 0) {
            throw new BusinessException(400, message);
        }
        return value;
    }

    private static String role(String value) {
        String role = value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
        if (!ROLES.contains(role)) {
            throw new BusinessException(400, "Invalid team role");
        }
        return role;
    }

    private static String normalizeKey(String key) {
        if (key == null || key.isBlank()) {
            return null;
        }
        if (key.length() > 64) {
            throw new BusinessException(400, "Idempotency key is too long");
        }
        return key.trim();
    }

    private static String hashToken() {
        byte[] token = new byte[32];
        SECURE_RANDOM.nextBytes(token);
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(token));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private TeamInvitationView invitationView(TeamInvitation invitation) {
        return new TeamInvitationView(invitation.getId(), invitation.getTeamId(), invitation.getInviteeUserId(),
                invitation.getRoleCode(), invitation.getStatus(), invitation.getExpiresAt(), invitation.getCreatedAt());
    }
}

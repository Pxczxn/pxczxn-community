package top.pxczxn.community.team.application;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.pxczxn.community.abuse.application.CommunityAbuseGuard;
import top.pxczxn.community.article.persistence.ArticleAuthorCountRow;
import top.pxczxn.community.article.persistence.ArticleMapper;
import top.pxczxn.community.blog.model.Blog;
import top.pxczxn.community.blog.persistence.BlogMapper;
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
import java.util.Collection;
import java.util.HexFormat;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class TeamMemberServiceImpl implements TeamMemberService {

    private static final Set<String> ROLES = Set.of("OWNER", "ADMIN", "EDITOR", "AUTHOR");
    private static final Set<String> ACTIVE_USER_STATUSES = Set.of("NORMAL", "LIMITED");
    /** The team-side invitation list is a management panel, not a feed; a hard cap keeps it one query. */
    private static final int TEAM_INVITATION_LIMIT = 100;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final TeamInvitationMapper invitationMapper;
    private final TeamMemberMapper memberMapper;
    private final TeamMapper teamMapper;
    private final CommunityUserMapper userMapper;
    private final BlogMapper blogMapper;
    private final ArticleMapper articleMapper;
    private final TeamAuthorityService authorityService;
    private final ApplicationEventPublisher eventPublisher;
    private final CommunityAbuseGuard abuseGuard;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TeamInvitationView invite(Long actorUserId, InviteTeamMemberCommand command) {
        Long teamId = requirePositive(command.teamId(), "Invalid team ID");
        Long inviteeUserId = requirePositive(command.inviteeUserId(), "Invalid invitee user ID");
        String role = role(command.roleCode());
        requireActiveTeam(teamId);
        abuseGuard.check("USER:" + actorUserId, "TEAM_INVITE", 5, 300);
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
        List<TeamInvitation> live = invitationMapper.findPendingForInvitee(userId).stream()
                .filter(invitation -> !expireIfNecessary(invitation))
                .toList();
        return invitationViews(live);
    }

    @Override
    public List<TeamInvitationView> teamInvitations(Long actorUserId, Long teamId) {
        requireActiveTeam(teamId);
        if (!authorityService.hasPermission(actorUserId, teamId, "MANAGE_MEMBERS")) {
            throw new BusinessException(403, "Not allowed to view team invitations");
        }
        return invitationViews(invitationMapper.findByTeam(teamId, TEAM_INVITATION_LIMIT));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void revokeInvitation(Long actorUserId, Long teamId, Long invitationId) {
        requireActiveTeam(teamId);
        TeamInvitation invitation = invitationMapper.findForTeam(
                requirePositive(invitationId, "Invalid invitation ID"),
                requirePositive(teamId, "Invalid team ID"));
        if (invitation == null) {
            throw new BusinessException(404, "Invitation does not exist");
        }
        // Judged against the invited role, so an ADMIN cannot cancel an invitation addressed to an ADMIN.
        if (!authorityService.canManageMember(actorUserId, teamId, invitation.getRoleCode())) {
            throw new BusinessException(403, "Not allowed to revoke this invitation");
        }
        if (!"PENDING".equals(invitation.getStatus())) {
            throw new BusinessException(409, "Only pending invitations can be revoked");
        }
        if (invitationMapper.revoke(invitationId, teamId, invitation.getLockVersion()) != 1) {
            throw new BusinessException(409, "Invitation state has changed; refresh and retry");
        }
        authorityService.recordAuditEvent(teamId, actorUserId, "INVITATION_REVOKED", "TEAM_INVITATION",
                invitationId, UUID.randomUUID().toString(), "{\"status\":\"PENDING\"}", "{\"status\":\"REVOKED\"}");
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
        Team team = requireActiveTeam(teamId);
        if (!authorityService.isMember(viewerUserId, teamId)) {
            throw new BusinessException(403, "Not allowed to view team members");
        }
        List<TeamMember> members = memberMapper.findActiveMembers(teamId);
        if (members.isEmpty()) {
            return List.of();
        }
        List<Long> userIds = members.stream()
                .map(TeamMember::getUserId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, CommunityUser> users = loadByIds(userIds.stream(), userMapper::selectBatchIds, CommunityUser::getId);
        Map<Long, ArticleAuthorCountRow> contributions = contributionsOf(team.getBlogId(), userIds);

        return members.stream()
                .map(member -> {
                    CommunityUser user = users.get(member.getUserId());
                    ArticleAuthorCountRow contribution = contributions.get(member.getUserId());
                    Integer total = contribution == null ? null : contribution.getTotal();
                    return new TeamMemberView(
                            member.getUserId(),
                            user == null ? null : user.getDisplayName(),
                            user == null ? null : user.getUsername(),
                            user == null ? null : user.getAvatarFileId(),
                            member.getRoleCode(),
                            member.getJoinedAt(),
                            total == null ? 0 : total,
                            contribution == null ? null : contribution.getLastActiveAt());
                })
                .toList();
    }

    /** Article counts per member inside the team blog; empty when the team has no blog or no members. */
    private Map<Long, ArticleAuthorCountRow> contributionsOf(Long blogId, List<Long> userIds) {
        if (blogId == null || userIds.isEmpty()) {
            return Map.of();
        }
        return articleMapper.countByBlogAndAuthors(blogId, userIds).stream()
                .filter(row -> row.getAuthorUserId() != null)
                .collect(Collectors.toMap(ArticleAuthorCountRow::getAuthorUserId, Function.identity(), (first, ignored) -> first));
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
        return invitationViews(List.of(invitation)).getFirst();
    }

    /**
     * Build invitation views for a batch in a bounded number of queries (teams, blogs, users), so the
     * "邀请与申请" list never degenerates into an N+1 loop.
     *
     * <p>Team display data lives on {@code blog}, hence the team -&gt; blog hop. Missing rows degrade to
     * null fields instead of throwing: a disbanded team or a deleted inviter must not break the list.
     */
    private List<TeamInvitationView> invitationViews(List<TeamInvitation> invitations) {
        if (invitations.isEmpty()) {
            return List.of();
        }
        Map<Long, Team> teams = loadByIds(invitations.stream().map(TeamInvitation::getTeamId),
                teamMapper::selectBatchIds, Team::getId);
        Map<Long, Blog> blogs = loadByIds(teams.values().stream().map(Team::getBlogId),
                blogMapper::selectBatchIds, Blog::getId);
        Map<Long, CommunityUser> users = loadByIds(
                invitations.stream().flatMap(item -> Stream.of(item.getInviteeUserId(), item.getInvitedByUserId())),
                userMapper::selectBatchIds, CommunityUser::getId);

        return invitations.stream()
                .map(invitation -> {
                    Team team = teams.get(invitation.getTeamId());
                    Blog blog = team == null || team.getBlogId() == null ? null : blogs.get(team.getBlogId());
                    CommunityUser invitee = users.get(invitation.getInviteeUserId());
                    CommunityUser inviter = users.get(invitation.getInvitedByUserId());
                    return new TeamInvitationView(
                            invitation.getId(),
                            invitation.getTeamId(),
                            blog == null ? null : blog.getName(),
                            blog == null ? null : blog.getSlug(),
                            blog == null ? null : blog.getAvatarFileId(),
                            invitation.getInviteeUserId(),
                            invitee == null ? null : invitee.getDisplayName(),
                            invitee == null ? null : invitee.getUsername(),
                            invitee == null ? null : invitee.getAvatarFileId(),
                            invitation.getRoleCode(),
                            invitation.getStatus(),
                            invitation.getInvitedByUserId(),
                            inviter == null ? null : inviter.getDisplayName(),
                            inviter == null ? null : inviter.getUsername(),
                            invitation.getExpiresAt(),
                            invitation.getCreatedAt());
                })
                .toList();
    }

    /** Batch-load rows keyed by id, skipping the query entirely when there is nothing to look up. */
    private static <T> Map<Long, T> loadByIds(Stream<Long> ids,
                                              Function<Collection<Long>, List<T>> loader,
                                              Function<T, Long> keyFunction) {
        Set<Long> unique = ids.filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (unique.isEmpty()) {
            return Map.of();
        }
        return loader.apply(unique).stream()
                .collect(Collectors.toMap(keyFunction, Function.identity(), (first, ignored) -> first));
    }
}

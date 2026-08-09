package top.pxczxn.community.team.application;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.pxczxn.community.blog.model.Blog;
import top.pxczxn.community.blog.persistence.BlogMapper;
import top.pxczxn.community.team.model.Team;
import top.pxczxn.community.team.model.TeamAuditEvent;
import top.pxczxn.community.team.model.TeamMember;
import top.pxczxn.community.team.persistence.TeamAuditEventMapper;
import top.pxczxn.community.team.persistence.TeamMapper;
import top.pxczxn.community.team.persistence.TeamMemberMapper;
import top.pxczxn.community.team.persistence.TeamPermissionMapper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * Centralized team authority service for resource-level permission decisions.
 * Controllers must NOT judge roles directly; they delegate to this service.
 */
@Service
@RequiredArgsConstructor
public class TeamAuthorityService {

    private final TeamMapper teamMapper;
    private final TeamMemberMapper teamMemberMapper;
    private final TeamPermissionMapper teamPermissionMapper;
    private final TeamAuditEventMapper teamAuditEventMapper;
    private final BlogMapper blogMapper;

    /**
     * Check if user has a specific permission on a team.
     */
    public boolean hasPermission(Long userId, Long teamId, String permissionCode) {
        if (userId == null || teamId == null || permissionCode == null) {
            return false;
        }

        TeamMember member = teamMemberMapper.findActiveMember(teamId, userId);
        if (member == null) {
            return false;
        }

        List<String> permissions = teamPermissionMapper.findPermissionsByRole(member.getRoleCode());
        return permissions.contains(permissionCode);
    }

    /**
     * Get the full permission set for a user on a team (empty list if not a member).
     * Used by portal views that need to expose the permission list to the frontend for UI gating,
     * so the permission source stays in one place instead of each service calling the mapper directly.
     */
    public List<String> getPermissions(Long userId, Long teamId) {
        if (userId == null || teamId == null) {
            return List.of();
        }
        TeamMember member = teamMemberMapper.findActiveMember(teamId, userId);
        if (member == null) {
            return List.of();
        }
        return teamPermissionMapper.findPermissionsByRole(member.getRoleCode());
    }

    /**
     * Check if user can manage a specific member (for add/remove/role change).
     * OWNER can manage all; ADMIN can manage EDITOR/AUTHOR only.
     */
    public boolean canManageMember(Long actorUserId, Long teamId, String targetRoleCode) {
        if (actorUserId == null || teamId == null || targetRoleCode == null) {
            return false;
        }

        TeamMember actorMember = teamMemberMapper.findActiveMember(teamId, actorUserId);
        if (actorMember == null) {
            return false;
        }

        String actorRole = actorMember.getRoleCode();

        // OWNER can manage all
        if ("OWNER".equals(actorRole)) {
            return true;
        }

        // ADMIN can only manage EDITOR and AUTHOR
        if ("ADMIN".equals(actorRole)) {
            return "EDITOR".equals(targetRoleCode) || "AUTHOR".equals(targetRoleCode);
        }

        // EDITOR and AUTHOR cannot manage members
        return false;
    }

    /**
     * Check if user can edit a specific article in a team context.
     * OWNER/ADMIN/EDITOR can edit all; AUTHOR can only edit their own.
     */
    public boolean canEditArticle(Long userId, Long teamId, Long articleAuthorId) {
        if (userId == null || teamId == null) {
            return false;
        }

        TeamMember member = teamMemberMapper.findActiveMember(teamId, userId);
        if (member == null) {
            return false;
        }

        String role = member.getRoleCode();

        // OWNER, ADMIN, EDITOR can edit all
        if ("OWNER".equals(role) || "ADMIN".equals(role) || "EDITOR".equals(role)) {
            return true;
        }

        // AUTHOR can only edit own articles
        if ("AUTHOR".equals(role)) {
            return Objects.equals(userId, articleAuthorId);
        }

        return false;
    }

    /**
     * Check if user can delete a specific article in a team context.
     * Same logic as edit.
     */
    public boolean canDeleteArticle(Long userId, Long teamId, Long articleAuthorId) {
        if (userId == null || teamId == null) {
            return false;
        }
        TeamMember member = teamMemberMapper.findActiveMember(teamId, userId);
        return member != null && ("OWNER".equals(member.getRoleCode())
                || "ADMIN".equals(member.getRoleCode()));
    }

    /**
     * Get user's role in a team (returns null if not a member).
     */
    public String getUserRole(Long userId, Long teamId) {
        if (userId == null || teamId == null) {
            return null;
        }

        TeamMember member = teamMemberMapper.findActiveMember(teamId, userId);
        return member != null ? member.getRoleCode() : null;
    }

    /**
     * Check if user is an active member of a team.
     */
    public boolean isMember(Long userId, Long teamId) {
        if (userId == null || teamId == null) {
            return false;
        }
        return teamMemberMapper.findActiveMember(teamId, userId) != null;
    }

    /**
     * Get team by blog ID.
     */
    public Team getTeamByBlogId(Long blogId) {
        if (blogId == null) {
            return null;
        }
        LambdaQueryWrapper<Team> query = new LambdaQueryWrapper<>();
        query.eq(Team::getBlogId, blogId);
        query.isNull(Team::getDeletedAt);
        return teamMapper.selectOne(query);
    }

    /**
     * Transfer team ownership within a transaction.
     * Must verify target is active member, update team.owner_user_id,
     * update both members' roles, update blog.owner_user_id, and write two audit events.
     */
    @Transactional(rollbackFor = Exception.class)
    public void transferOwnership(Long teamId, Long currentOwnerId, Long newOwnerId, String requestId) {
        // Verify current owner
        Team team = teamMapper.selectById(teamId);
        if (team == null || team.getDeletedAt() != null) {
            throw new IllegalStateException("团队不存在或已解散");
        }
        if (!Objects.equals(team.getOwnerUserId(), currentOwnerId)) {
            throw new IllegalStateException("当前用户不是团队负责人");
        }

        // Verify blog exists and is TEAM type
        Blog blog = blogMapper.selectById(team.getBlogId());
        if (blog == null || !"TEAM".equals(blog.getBlogType())) {
            throw new IllegalStateException("团队博客不存在");
        }

        // Verify target is active member
        TeamMember newOwnerMember = teamMemberMapper.findActiveMember(teamId, newOwnerId);
        if (newOwnerMember == null) {
            throw new IllegalStateException("目标用户不是活跃的团队成员");
        }

        TeamMember currentOwnerMember = teamMemberMapper.findActiveMember(teamId, currentOwnerId);
        if (currentOwnerMember == null) {
            throw new IllegalStateException("当前负责人记录不存在");
        }

        String newOwnerOldRole = newOwnerMember.getRoleCode();
        Integer teamLockVersion = team.getLockVersion();
        Integer currentOwnerMemberLockVersion = currentOwnerMember.getLockVersion();
        Integer newOwnerMemberLockVersion = newOwnerMember.getLockVersion();
        Integer blogLockVersion = blog.getLockVersion();

        // Update team owner with conditional update (id + lock_version + current owner)
        int teamUpdated = teamMapper.updateOwnerWithOptimisticLock(
                teamId,
                currentOwnerId,
                newOwnerId,
                teamLockVersion
        );
        if (teamUpdated != 1) {
            throw new IllegalStateException("负责人移交失败，请刷新后重试");
        }

        // Update current owner to ADMIN with conditional update
        int currentOwnerUpdated = teamMemberMapper.updateRoleWithOptimisticLock(
                currentOwnerMember.getId(),
                "ADMIN",
                currentOwnerMemberLockVersion
        );
        if (currentOwnerUpdated != 1) {
            throw new IllegalStateException("负责人移交失败，请刷新后重试");
        }

        // Update new owner to OWNER with conditional update
        int newOwnerUpdated = teamMemberMapper.updateRoleWithOptimisticLock(
                newOwnerMember.getId(),
                "OWNER",
                newOwnerMemberLockVersion
        );
        if (newOwnerUpdated != 1) {
            throw new IllegalStateException("负责人移交失败，请刷新后重试");
        }

        // Update blog owner with conditional update
        int blogUpdated = blogMapper.updateOwnerWithOptimisticLock(
                blog.getId(),
                newOwnerId,
                blogLockVersion
        );
        if (blogUpdated != 1) {
            throw new IllegalStateException("负责人移交失败，请刷新后重试");
        }

        // Write audit events
        TeamAuditEvent event1 = new TeamAuditEvent();
        event1.setTeamId(teamId);
        event1.setActorUserId(currentOwnerId);
        event1.setEventType("OWNERSHIP_TRANSFERRED");
        event1.setTargetType("TEAM_MEMBER");
        event1.setTargetId(newOwnerId);
        event1.setRequestId(requestId);
        event1.setBeforeSnapshot("{\"oldOwner\":" + currentOwnerId + ",\"newOwnerRole\":\"" + newOwnerOldRole + "\"}");
        event1.setAfterSnapshot("{\"newOwner\":" + newOwnerId + ",\"oldOwnerRole\":\"ADMIN\"}");
        event1.setOccurredAt(LocalDateTime.now());
        teamAuditEventMapper.insert(event1);

        TeamAuditEvent event2 = new TeamAuditEvent();
        event2.setTeamId(teamId);
        event2.setActorUserId(currentOwnerId);
        event2.setEventType("MEMBER_ROLE_CHANGED");
        event2.setTargetType("TEAM_MEMBER");
        event2.setTargetId(currentOwnerId);
        event2.setRequestId(requestId);
        event2.setBeforeSnapshot("{\"role\":\"OWNER\"}");
        event2.setAfterSnapshot("{\"role\":\"ADMIN\"}");
        event2.setOccurredAt(LocalDateTime.now());
        teamAuditEventMapper.insert(event2);
    }

    /**
     * Record an audit event.
     */
    public void recordAuditEvent(Long teamId, Long actorUserId, String eventType,
                                   String targetType, Long targetId, String requestId,
                                   String beforeSnapshot, String afterSnapshot) {
        TeamAuditEvent event = new TeamAuditEvent();
        event.setTeamId(teamId);
        event.setActorUserId(actorUserId);
        event.setEventType(eventType);
        event.setTargetType(targetType);
        event.setTargetId(targetId);
        event.setRequestId(requestId);
        event.setBeforeSnapshot(beforeSnapshot);
        event.setAfterSnapshot(afterSnapshot);
        event.setOccurredAt(LocalDateTime.now());
        teamAuditEventMapper.insert(event);
    }
}

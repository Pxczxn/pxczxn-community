package top.pxczxn.community.team.application;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import top.pxczxn.community.abuse.application.CommunityAbuseGuard;
import top.pxczxn.community.article.persistence.ArticleAuthorCountRow;
import top.pxczxn.community.article.persistence.ArticleMapper;
import top.pxczxn.community.blog.model.Blog;
import top.pxczxn.community.blog.persistence.BlogMapper;
import top.pxczxn.community.team.model.Team;
import top.pxczxn.community.team.model.TeamInvitation;
import top.pxczxn.community.team.model.TeamMember;
import top.pxczxn.community.team.persistence.TeamInvitationMapper;
import top.pxczxn.community.team.persistence.TeamMapper;
import top.pxczxn.community.team.persistence.TeamMemberMapper;
import top.pxczxn.community.user.model.CommunityUser;
import top.pxczxn.community.user.persistence.CommunityUserMapper;
import top.pxczxn.platform.common.exception.BusinessException;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TeamMemberServiceImplTest {

    private TeamInvitationMapper invitationMapper;
    private TeamMemberMapper memberMapper;
    private TeamMapper teamMapper;
    private CommunityUserMapper userMapper;
    private BlogMapper blogMapper;
    private ArticleMapper articleMapper;
    private TeamAuthorityService authorityService;
    private ApplicationEventPublisher eventPublisher;
    private CommunityAbuseGuard abuseGuard;
    private TeamMemberServiceImpl service;

    @BeforeEach
    void setUp() {
        invitationMapper = mock(TeamInvitationMapper.class);
        memberMapper = mock(TeamMemberMapper.class);
        teamMapper = mock(TeamMapper.class);
        userMapper = mock(CommunityUserMapper.class);
        blogMapper = mock(BlogMapper.class);
        articleMapper = mock(ArticleMapper.class);
        authorityService = mock(TeamAuthorityService.class);
        eventPublisher = mock(ApplicationEventPublisher.class);
        abuseGuard = mock(CommunityAbuseGuard.class);
        service = new TeamMemberServiceImpl(invitationMapper, memberMapper, teamMapper, userMapper,
                blogMapper, articleMapper, authorityService, eventPublisher, abuseGuard);
    }

    @Test
    void inviteReplaysMatchingIdempotencyKey() {
        TeamInvitation invitation = pendingInvitation(10L, 20L, "EDITOR");
        invitation.setIdempotencyKey("key-1");
        invitation.setInvitedByUserId(1L);
        when(teamMapper.selectById(10L)).thenReturn(activeTeam(10L, 1L));
        when(authorityService.canManageMember(1L, 10L, "EDITOR")).thenReturn(true);
        when(authorityService.isMember(20L, 10L)).thenReturn(false);
        when(userMapper.selectById(20L)).thenReturn(activeUser(20L));
        when(invitationMapper.findByIdempotencyKey("key-1")).thenReturn(invitation);

        TeamInvitationView result = service.invite(1L, new InviteTeamMemberCommand(10L, 20L, "EDITOR", "key-1"));

        assertThat(result.id()).isEqualTo(99L);
        verify(invitationMapper, never()).insert(any());
    }

    @Test
    void inviteRejectsExistingPendingInvitationEvenWhenRoleChanges() {
        TeamInvitation pending = pendingInvitation(10L, 20L, "AUTHOR");
        when(teamMapper.selectById(10L)).thenReturn(activeTeam(10L, 1L));
        when(authorityService.canManageMember(1L, 10L, "EDITOR")).thenReturn(true);
        when(authorityService.isMember(20L, 10L)).thenReturn(false);
        when(userMapper.selectById(20L)).thenReturn(activeUser(20L));
        when(invitationMapper.findPendingForTeamAndInvitee(10L, 20L)).thenReturn(pending);

        assertThatThrownBy(() -> service.invite(1L, new InviteTeamMemberCommand(10L, 20L, "EDITOR", null)))
                .isInstanceOf(BusinessException.class);
        verify(invitationMapper, never()).insert(any());
    }

    @Test
    void acceptAddsMemberAndAuditsAfterConditionalInvitationUpdate() {
        TeamInvitation invitation = pendingInvitation(10L, 20L, "EDITOR");
        when(invitationMapper.findForInvitee(99L, 20L)).thenReturn(invitation);
        when(teamMapper.selectById(10L)).thenReturn(activeTeam(10L, 1L));
        when(userMapper.selectById(20L)).thenReturn(activeUser(20L));
        when(authorityService.isMember(20L, 10L)).thenReturn(false);
        when(invitationMapper.accept(99L, 20L, 0)).thenReturn(1);

        service.accept(20L, 99L);

        ArgumentCaptor<TeamMember> member = ArgumentCaptor.forClass(TeamMember.class);
        verify(memberMapper).insert(member.capture());
        assertThat(member.getValue().getRoleCode()).isEqualTo("EDITOR");
        verify(authorityService).recordAuditEvent(eq(10L), eq(20L), eq("INVITATION_ACCEPTED"), any(), isNull(), any(), any(), any());
    }

    @Test
    void expiredInvitationIsPersistedAndCannotBeAccepted() {
        TeamInvitation invitation = pendingInvitation(10L, 20L, "AUTHOR");
        invitation.setExpiresAt(LocalDateTime.now(ZoneOffset.UTC).minusMinutes(1));
        when(invitationMapper.findForInvitee(99L, 20L)).thenReturn(invitation);

        assertThatThrownBy(() -> service.accept(20L, 99L)).isInstanceOf(BusinessException.class);
        verify(invitationMapper).expire(99L, 0);
        verify(memberMapper, never()).insert(any());
    }

    @Test
    void editorCannotRemoveMembers() {
        TeamMember target = activeMember(10L, 20L, "AUTHOR");
        when(teamMapper.selectById(10L)).thenReturn(activeTeam(10L, 1L));
        when(memberMapper.findActiveMember(10L, 20L)).thenReturn(target);
        when(authorityService.canManageMember(1L, 10L, "AUTHOR")).thenReturn(false);

        assertThatThrownBy(() -> service.remove(1L, 10L, 20L)).isInstanceOf(BusinessException.class);
        verify(memberMapper, never()).leaveWithOptimisticLock(anyLong(), any());
    }

    @Test
    void ownerCannotLeaveWithoutTransfer() {
        when(teamMapper.selectById(10L)).thenReturn(activeTeam(10L, 1L));
        when(memberMapper.findActiveMember(10L, 1L)).thenReturn(activeMember(10L, 1L, "OWNER"));

        assertThatThrownBy(() -> service.leave(1L, 10L)).isInstanceOf(BusinessException.class);
    }

    @Test
    void disbandRequiresOwnerAndLeavesActiveMembers() {
        Team team = activeTeam(10L, 1L);
        when(teamMapper.selectById(10L)).thenReturn(team);
        when(teamMapper.disbandWithOptimisticLock(10L, 1L, 0)).thenReturn(1);

        service.disband(1L, 10L);

        verify(memberMapper).leaveAllActive(10L);
        verify(authorityService).recordAuditEvent(eq(10L), eq(1L), eq("TEAM_DISBANDED"), any(), eq(10L), any(), any(), any());
    }

    @Test
    void membersRejectsCrossTeamViewer() {
        when(teamMapper.selectById(10L)).thenReturn(activeTeam(10L, 1L));
        when(authorityService.isMember(99L, 10L)).thenReturn(false);

        assertThatThrownBy(() -> service.members(99L, 10L)).isInstanceOf(BusinessException.class);
        verify(memberMapper, never()).findActiveMembers(anyLong());
    }

    @Test
    void teamInvitationsRequireManageMembers() {
        when(teamMapper.selectById(10L)).thenReturn(activeTeam(10L, 1L));
        when(authorityService.hasPermission(5L, 10L, "MANAGE_MEMBERS")).thenReturn(false);

        assertThatThrownBy(() -> service.teamInvitations(5L, 10L)).isInstanceOf(BusinessException.class);
        verify(invitationMapper, never()).findByTeam(anyLong(), anyInt());
    }

    @Test
    void revokeInvitationIsJudgedAgainstTheInvitedRole() {
        when(teamMapper.selectById(10L)).thenReturn(activeTeam(10L, 1L));
        when(invitationMapper.findForTeam(99L, 10L)).thenReturn(pendingInvitation(10L, 20L, "ADMIN"));
        when(authorityService.canManageMember(2L, 10L, "ADMIN")).thenReturn(false);

        assertThatThrownBy(() -> service.revokeInvitation(2L, 10L, 99L)).isInstanceOf(BusinessException.class);
        verify(invitationMapper, never()).revoke(anyLong(), anyLong(), any());
    }

    @Test
    void revokeInvitationWritesAuditAfterConditionalUpdate() {
        when(teamMapper.selectById(10L)).thenReturn(activeTeam(10L, 1L));
        when(invitationMapper.findForTeam(99L, 10L)).thenReturn(pendingInvitation(10L, 20L, "EDITOR"));
        when(authorityService.canManageMember(1L, 10L, "EDITOR")).thenReturn(true);
        when(invitationMapper.revoke(99L, 10L, 0)).thenReturn(1);

        service.revokeInvitation(1L, 10L, 99L);

        verify(authorityService).recordAuditEvent(eq(10L), eq(1L), eq("INVITATION_REVOKED"), any(), eq(99L),
                any(), any(), any());
    }

    @Test
    void revokeInvitationRejectsAlreadyProcessedInvitation() {
        TeamInvitation accepted = pendingInvitation(10L, 20L, "EDITOR");
        accepted.setStatus("ACCEPTED");
        when(teamMapper.selectById(10L)).thenReturn(activeTeam(10L, 1L));
        when(invitationMapper.findForTeam(99L, 10L)).thenReturn(accepted);
        when(authorityService.canManageMember(1L, 10L, "EDITOR")).thenReturn(true);

        assertThatThrownBy(() -> service.revokeInvitation(1L, 10L, 99L)).isInstanceOf(BusinessException.class);
        verify(invitationMapper, never()).revoke(anyLong(), anyLong(), any());
    }

    @Test
    void membersCarryProfileAndContributionWithoutSecondRoundTrip() {
        Team team = activeTeam(10L, 1L);
        team.setBlogId(500L);
        when(teamMapper.selectById(10L)).thenReturn(team);
        when(authorityService.isMember(1L, 10L)).thenReturn(true);
        when(memberMapper.findActiveMembers(10L))
                .thenReturn(List.of(activeMember(10L, 1L, "OWNER"), activeMember(10L, 2L, "AUTHOR")));

        CommunityUser owner = activeUser(1L);
        owner.setDisplayName("Owner One");
        owner.setUsername("owner-one");
        owner.setAvatarFileId(4001L);
        when(userMapper.selectBatchIds(any())).thenReturn(List.of(owner));

        LocalDateTime lastActive = LocalDateTime.now(ZoneOffset.UTC).minusDays(2);
        ArticleAuthorCountRow row = new ArticleAuthorCountRow();
        row.setAuthorUserId(1L);
        row.setTotal(7);
        row.setLastActiveAt(lastActive);
        when(articleMapper.countByBlogAndAuthors(eq(500L), any())).thenReturn(List.of(row));

        List<TeamMemberView> members = service.members(1L, 10L);

        assertThat(members).hasSize(2);
        TeamMemberView owned = members.getFirst();
        assertThat(owned.displayName()).isEqualTo("Owner One");
        assertThat(owned.username()).isEqualTo("owner-one");
        assertThat(owned.avatarFileId()).isEqualTo(4001L);
        assertThat(owned.contributionCount()).isEqualTo(7);
        assertThat(owned.lastActiveAt()).isEqualTo(lastActive);
        // A member who has not written anything must read as 0, never null, so the UI can do arithmetic.
        assertThat(members.get(1).contributionCount()).isZero();
        assertThat(members.get(1).lastActiveAt()).isNull();
    }

    @Test
    void membersSkipArticleQueryWhenTeamHasNoBlog() {
        Team team = activeTeam(10L, 1L);
        team.setBlogId(null);
        when(teamMapper.selectById(10L)).thenReturn(team);
        when(authorityService.isMember(1L, 10L)).thenReturn(true);
        when(memberMapper.findActiveMembers(10L)).thenReturn(List.of(activeMember(10L, 1L, "OWNER")));

        assertThat(service.members(1L, 10L)).hasSize(1);
        verify(articleMapper, never()).countByBlogAndAuthors(any(), any());
    }

    @Test
    void pendingInvitationsCarryTeamAndInviterIdentity() {
        when(invitationMapper.findPendingForInvitee(20L))
                .thenReturn(List.of(pendingInvitation(10L, 20L, "EDITOR")));
        Team team = activeTeam(10L, 1L);
        team.setBlogId(500L);
        when(teamMapper.selectBatchIds(any())).thenReturn(List.of(team));

        Blog blog = new Blog();
        blog.setId(500L);
        blog.setName("Frontend Guild");
        blog.setSlug("frontend-guild");
        blog.setAvatarFileId(9001L);
        when(blogMapper.selectBatchIds(any())).thenReturn(List.of(blog));

        CommunityUser inviter = activeUser(1L);
        inviter.setDisplayName("Captain");
        inviter.setUsername("captain");
        when(userMapper.selectBatchIds(any())).thenReturn(List.of(inviter));

        List<TeamInvitationView> views = service.myPendingInvitations(20L);

        assertThat(views).hasSize(1);
        TeamInvitationView view = views.getFirst();
        assertThat(view.teamName()).isEqualTo("Frontend Guild");
        assertThat(view.teamSlug()).isEqualTo("frontend-guild");
        assertThat(view.teamAvatarFileId()).isEqualTo(9001L);
        assertThat(view.inviterUserId()).isEqualTo(1L);
        assertThat(view.inviterDisplayName()).isEqualTo("Captain");
    }

    @Test
    void invitationViewDegradesGracefullyWhenTeamRowIsGone() {
        when(invitationMapper.findPendingForInvitee(20L))
                .thenReturn(List.of(pendingInvitation(10L, 20L, "EDITOR")));
        when(teamMapper.selectBatchIds(any())).thenReturn(List.of());

        List<TeamInvitationView> views = service.myPendingInvitations(20L);

        assertThat(views).hasSize(1);
        assertThat(views.getFirst().teamName()).isNull();
        assertThat(views.getFirst().teamId()).isEqualTo(10L);
    }

    private static Team activeTeam(Long teamId, Long ownerUserId) {
        Team team = new Team();
        team.setId(teamId);
        team.setOwnerUserId(ownerUserId);
        team.setStatus("ACTIVE");
        team.setLockVersion(0);
        return team;
    }

    private static CommunityUser activeUser(Long id) {
        CommunityUser user = new CommunityUser();
        user.setId(id);
        user.setStatus("NORMAL");
        return user;
    }

    private static TeamInvitation pendingInvitation(Long teamId, Long inviteeId, String role) {
        TeamInvitation invitation = new TeamInvitation();
        invitation.setId(99L);
        invitation.setTeamId(teamId);
        invitation.setInviteeUserId(inviteeId);
        invitation.setInvitedByUserId(1L);
        invitation.setRoleCode(role);
        invitation.setStatus("PENDING");
        invitation.setLockVersion(0);
        invitation.setCreatedAt(LocalDateTime.now(ZoneOffset.UTC));
        invitation.setExpiresAt(LocalDateTime.now(ZoneOffset.UTC).plusDays(1));
        return invitation;
    }

    private static TeamMember activeMember(Long teamId, Long userId, String role) {
        TeamMember member = new TeamMember();
        member.setId(77L);
        member.setTeamId(teamId);
        member.setUserId(userId);
        member.setRoleCode(role);
        member.setLockVersion(0);
        return member;
    }

    @Test
    void inviteRejectsWhenAbuseGuardTriggers429() {
        when(teamMapper.selectById(10L)).thenReturn(activeTeam(10L, 1L));
        doThrow(new BusinessException(429, "Too many TEAM_INVITE actions; retry after the policy window"))
                .when(abuseGuard).check("USER:1", "TEAM_INVITE", 5, 300);

        assertThatThrownBy(() -> service.invite(1L, new InviteTeamMemberCommand(10L, 20L, "EDITOR", null)))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getCode()).isEqualTo(429));

        verify(invitationMapper, never()).insert(any());
    }
}

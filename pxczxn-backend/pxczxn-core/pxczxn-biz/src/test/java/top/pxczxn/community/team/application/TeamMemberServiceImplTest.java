package top.pxczxn.community.team.application;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TeamMemberServiceImplTest {

    private TeamInvitationMapper invitationMapper;
    private TeamMemberMapper memberMapper;
    private TeamMapper teamMapper;
    private CommunityUserMapper userMapper;
    private TeamAuthorityService authorityService;
    private ApplicationEventPublisher eventPublisher;
    private TeamMemberServiceImpl service;

    @BeforeEach
    void setUp() {
        invitationMapper = mock(TeamInvitationMapper.class);
        memberMapper = mock(TeamMemberMapper.class);
        teamMapper = mock(TeamMapper.class);
        userMapper = mock(CommunityUserMapper.class);
        authorityService = mock(TeamAuthorityService.class);
        eventPublisher = mock(ApplicationEventPublisher.class);
        service = new TeamMemberServiceImpl(invitationMapper, memberMapper, teamMapper, userMapper,
                authorityService, eventPublisher);
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
}

package top.pxczxn.community.team.application;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TeamAuthorityServiceTest {

    private TeamMapper teamMapper;
    private TeamMemberMapper teamMemberMapper;
    private TeamPermissionMapper teamPermissionMapper;
    private TeamAuditEventMapper teamAuditEventMapper;
    private BlogMapper blogMapper;
    private TeamAuthorityService service;

    @BeforeEach
    void setUp() {
        teamMapper = mock(TeamMapper.class);
        teamMemberMapper = mock(TeamMemberMapper.class);
        teamPermissionMapper = mock(TeamPermissionMapper.class);
        teamAuditEventMapper = mock(TeamAuditEventMapper.class);
        blogMapper = mock(BlogMapper.class);
        service = new TeamAuthorityService(
                teamMapper,
                teamMemberMapper,
                teamPermissionMapper,
                teamAuditEventMapper,
                blogMapper
        );
    }

    @Test
    void hasPermissionReturnsTrueWhenMemberHasPermission() {
        TeamMember member = new TeamMember();
        member.setRoleCode("ADMIN");
        when(teamMemberMapper.findActiveMember(1L, 100L)).thenReturn(member);
        when(teamPermissionMapper.findPermissionsByRole("ADMIN"))
                .thenReturn(List.of("MANAGE_TEAM", "MANAGE_MEMBERS"));

        boolean result = service.hasPermission(100L, 1L, "MANAGE_TEAM");

        assertThat(result).isTrue();
    }

    @Test
    void hasPermissionReturnsFalseWhenMemberLacksPermission() {
        TeamMember member = new TeamMember();
        member.setRoleCode("AUTHOR");
        when(teamMemberMapper.findActiveMember(1L, 100L)).thenReturn(member);
        when(teamPermissionMapper.findPermissionsByRole("AUTHOR"))
                .thenReturn(List.of("EDIT_OWN_ARTICLES"));

        boolean result = service.hasPermission(100L, 1L, "MANAGE_TEAM");

        assertThat(result).isFalse();
    }

    @Test
    void hasPermissionReturnsFalseForCrossTeamAccess() {
        when(teamMemberMapper.findActiveMember(1L, 100L)).thenReturn(null);

        boolean result = service.hasPermission(100L, 1L, "MANAGE_TEAM");

        assertThat(result).isFalse();
    }

    @Test
    void canManageMemberAllowsOwnerToManageAll() {
        TeamMember owner = new TeamMember();
        owner.setRoleCode("OWNER");
        when(teamMemberMapper.findActiveMember(1L, 100L)).thenReturn(owner);

        assertThat(service.canManageMember(100L, 1L, "OWNER")).isTrue();
        assertThat(service.canManageMember(100L, 1L, "ADMIN")).isTrue();
        assertThat(service.canManageMember(100L, 1L, "EDITOR")).isTrue();
        assertThat(service.canManageMember(100L, 1L, "AUTHOR")).isTrue();
    }

    @Test
    void canManageMemberRestrictsAdminToEditorAndAuthor() {
        TeamMember admin = new TeamMember();
        admin.setRoleCode("ADMIN");
        when(teamMemberMapper.findActiveMember(1L, 100L)).thenReturn(admin);

        assertThat(service.canManageMember(100L, 1L, "OWNER")).isFalse();
        assertThat(service.canManageMember(100L, 1L, "ADMIN")).isFalse();
        assertThat(service.canManageMember(100L, 1L, "EDITOR")).isTrue();
        assertThat(service.canManageMember(100L, 1L, "AUTHOR")).isTrue();
    }

    @Test
    void canManageMemberDeniesEditorFromManagingMembers() {
        TeamMember editor = new TeamMember();
        editor.setRoleCode("EDITOR");
        when(teamMemberMapper.findActiveMember(1L, 100L)).thenReturn(editor);

        assertThat(service.canManageMember(100L, 1L, "AUTHOR")).isFalse();
        assertThat(service.canManageMember(100L, 1L, "EDITOR")).isFalse();
    }

    @Test
    void canEditArticleAllowsOwnerAdminEditorForAllArticles() {
        TeamMember owner = new TeamMember();
        owner.setRoleCode("OWNER");
        when(teamMemberMapper.findActiveMember(1L, 100L)).thenReturn(owner);

        assertThat(service.canEditArticle(100L, 1L, 999L)).isTrue();

        TeamMember admin = new TeamMember();
        admin.setRoleCode("ADMIN");
        when(teamMemberMapper.findActiveMember(1L, 200L)).thenReturn(admin);

        assertThat(service.canEditArticle(200L, 1L, 999L)).isTrue();

        TeamMember editor = new TeamMember();
        editor.setRoleCode("EDITOR");
        when(teamMemberMapper.findActiveMember(1L, 300L)).thenReturn(editor);

        assertThat(service.canEditArticle(300L, 1L, 999L)).isTrue();
    }

    @Test
    void canEditArticleRestrictsAuthorToOwnArticles() {
        TeamMember author = new TeamMember();
        author.setRoleCode("AUTHOR");
        when(teamMemberMapper.findActiveMember(1L, 100L)).thenReturn(author);

        assertThat(service.canEditArticle(100L, 1L, 100L)).isTrue();
        assertThat(service.canEditArticle(100L, 1L, 999L)).isFalse();
    }

    @Test
    void canDeleteArticleFollowsSameLogicAsEditArticle() {
        TeamMember author = new TeamMember();
        author.setRoleCode("AUTHOR");
        when(teamMemberMapper.findActiveMember(1L, 100L)).thenReturn(author);

        assertThat(service.canDeleteArticle(100L, 1L, 100L)).isTrue();
        assertThat(service.canDeleteArticle(100L, 1L, 999L)).isFalse();
    }

    @Test
    void getUserRoleReturnsRoleForActiveMember() {
        TeamMember member = new TeamMember();
        member.setRoleCode("EDITOR");
        when(teamMemberMapper.findActiveMember(1L, 100L)).thenReturn(member);

        String role = service.getUserRole(100L, 1L);

        assertThat(role).isEqualTo("EDITOR");
    }

    @Test
    void getUserRoleReturnsNullForNonMember() {
        when(teamMemberMapper.findActiveMember(1L, 100L)).thenReturn(null);

        String role = service.getUserRole(100L, 1L);

        assertThat(role).isNull();
    }

    @Test
    void isMemberReturnsTrueForActiveMember() {
        TeamMember member = new TeamMember();
        when(teamMemberMapper.findActiveMember(1L, 100L)).thenReturn(member);

        assertThat(service.isMember(100L, 1L)).isTrue();
    }

    @Test
    void isMemberReturnsFalseForNonMember() {
        when(teamMemberMapper.findActiveMember(1L, 100L)).thenReturn(null);

        assertThat(service.isMember(100L, 1L)).isFalse();
    }

    @Test
    void getTeamByBlogIdReturnsTeamWhenExists() {
        Team team = new Team();
        team.setId(1L);
        team.setBlogId(100L);
        when(teamMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(team);

        Team result = service.getTeamByBlogId(100L);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
    }

    @Test
    void transferOwnershipUpdatesTeamAndMembersAndBlogInTransaction() {
        Team team = new Team();
        team.setId(1L);
        team.setBlogId(100L);
        team.setOwnerUserId(10L);
        team.setLockVersion(0);
        when(teamMapper.selectById(1L)).thenReturn(team);

        TeamMember currentOwnerMember = new TeamMember();
        currentOwnerMember.setId(1001L);
        currentOwnerMember.setRoleCode("OWNER");
        currentOwnerMember.setLockVersion(0);
        when(teamMemberMapper.findActiveMember(1L, 10L)).thenReturn(currentOwnerMember);

        TeamMember newOwnerMember = new TeamMember();
        newOwnerMember.setId(1002L);
        newOwnerMember.setRoleCode("ADMIN");
        newOwnerMember.setLockVersion(0);
        when(teamMemberMapper.findActiveMember(1L, 20L)).thenReturn(newOwnerMember);

        Blog blog = new Blog();
        blog.setId(100L);
        blog.setBlogType("TEAM");
        blog.setOwnerUserId(10L);
        blog.setLockVersion(0);
        when(blogMapper.selectById(100L)).thenReturn(blog);

        when(teamMapper.updateOwnerWithOptimisticLock(1L, 10L, 20L, 0)).thenReturn(1);
        when(teamMemberMapper.updateRoleWithOptimisticLock(1001L, "ADMIN", 0)).thenReturn(1);
        when(teamMemberMapper.updateRoleWithOptimisticLock(1002L, "OWNER", 0)).thenReturn(1);
        when(blogMapper.updateOwnerWithOptimisticLock(100L, 20L, 0)).thenReturn(1);

        service.transferOwnership(1L, 10L, 20L, "req-123");

        verify(teamMapper).updateOwnerWithOptimisticLock(1L, 10L, 20L, 0);
        verify(teamMemberMapper).updateRoleWithOptimisticLock(1001L, "ADMIN", 0);
        verify(teamMemberMapper).updateRoleWithOptimisticLock(1002L, "OWNER", 0);
        verify(blogMapper).updateOwnerWithOptimisticLock(100L, 20L, 0);
        verify(teamAuditEventMapper, times(2)).insert(any(TeamAuditEvent.class));
    }

    @Test
    void transferOwnershipThrowsWhenCurrentUserIsNotOwner() {
        Team team = new Team();
        team.setId(1L);
        team.setOwnerUserId(10L);
        when(teamMapper.selectById(1L)).thenReturn(team);

        assertThatThrownBy(() -> service.transferOwnership(1L, 99L, 20L, "req-123"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not the owner");

        verify(teamMapper, never()).updateById(any());
    }

    @Test
    void transferOwnershipThrowsWhenNewOwnerIsNotActiveMember() {
        Team team = new Team();
        team.setId(1L);
        team.setBlogId(100L);
        team.setOwnerUserId(10L);
        when(teamMapper.selectById(1L)).thenReturn(team);
        Blog blog = new Blog();
        blog.setId(100L);
        blog.setBlogType("TEAM");
        blog.setBlogType("TEAM");
        when(blogMapper.selectById(100L)).thenReturn(blog);
        when(teamMemberMapper.findActiveMember(1L, 20L)).thenReturn(null);

        assertThatThrownBy(() -> service.transferOwnership(1L, 10L, 20L, "req-123"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not an active team member");

        verify(teamMapper, never()).updateById(any());
    }

    @Test
    void transferOwnershipThrowsOnOptimisticLockConflict() {
        Team team = new Team();
        team.setId(1L);
        team.setBlogId(100L);
        team.setOwnerUserId(10L);
        team.setLockVersion(0);
        when(teamMapper.selectById(1L)).thenReturn(team);

        TeamMember currentOwnerMember = new TeamMember();
        currentOwnerMember.setId(1001L);
        currentOwnerMember.setRoleCode("OWNER");
        currentOwnerMember.setLockVersion(0);
        when(teamMemberMapper.findActiveMember(1L, 10L)).thenReturn(currentOwnerMember);

        TeamMember newOwnerMember = new TeamMember();
        newOwnerMember.setId(1002L);
        newOwnerMember.setRoleCode("ADMIN");
        newOwnerMember.setLockVersion(0);
        when(teamMemberMapper.findActiveMember(1L, 20L)).thenReturn(newOwnerMember);

        Blog blog = new Blog();
        blog.setId(100L);
        blog.setBlogType("TEAM");
        blog.setLockVersion(0);
        when(blogMapper.selectById(100L)).thenReturn(blog);

        when(teamMapper.updateOwnerWithOptimisticLock(1L, 10L, 20L, 0)).thenReturn(0);

        assertThatThrownBy(() -> service.transferOwnership(1L, 10L, 20L, "req-123"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("concurrent modification");

        verify(teamMemberMapper, never()).updateRoleWithOptimisticLock(anyLong(), any(), any());
        verify(blogMapper, never()).updateOwnerWithOptimisticLock(anyLong(), anyLong(), any());
        verify(teamAuditEventMapper, never()).insert(any());
    }

    @Test
    void transferOwnershipThrowsWhenBlogNotFound() {
        Team team = new Team();
        team.setId(1L);
        team.setBlogId(100L);
        team.setOwnerUserId(10L);
        team.setLockVersion(0);
        when(teamMapper.selectById(1L)).thenReturn(team);
        when(blogMapper.selectById(100L)).thenReturn(null);

        assertThatThrownBy(() -> service.transferOwnership(1L, 10L, 20L, "req-123"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Team blog not found");

        verify(teamMapper, never()).updateOwnerWithOptimisticLock(anyLong(), anyLong(), anyLong(), any());
    }

    @Test
    void transferOwnershipRollsBackOnMemberUpdateFailure() {
        Team team = new Team();
        team.setId(1L);
        team.setBlogId(100L);
        team.setOwnerUserId(10L);
        team.setLockVersion(0);
        when(teamMapper.selectById(1L)).thenReturn(team);

        TeamMember currentOwnerMember = new TeamMember();
        currentOwnerMember.setId(1001L);
        currentOwnerMember.setRoleCode("OWNER");
        currentOwnerMember.setLockVersion(0);
        when(teamMemberMapper.findActiveMember(1L, 10L)).thenReturn(currentOwnerMember);

        TeamMember newOwnerMember = new TeamMember();
        newOwnerMember.setId(1002L);
        newOwnerMember.setRoleCode("ADMIN");
        newOwnerMember.setLockVersion(0);
        when(teamMemberMapper.findActiveMember(1L, 20L)).thenReturn(newOwnerMember);

        Blog blog = new Blog();
        blog.setId(100L);
        blog.setBlogType("TEAM");
        blog.setLockVersion(0);
        when(blogMapper.selectById(100L)).thenReturn(blog);

        when(teamMapper.updateOwnerWithOptimisticLock(1L, 10L, 20L, 0)).thenReturn(1);
        when(teamMemberMapper.updateRoleWithOptimisticLock(1001L, "ADMIN", 0)).thenReturn(0);

        assertThatThrownBy(() -> service.transferOwnership(1L, 10L, 20L, "req-123"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Failed to update current owner role");

        verify(teamMemberMapper, never()).updateRoleWithOptimisticLock(1002L, "OWNER", 0);
        verify(blogMapper, never()).updateOwnerWithOptimisticLock(anyLong(), anyLong(), any());
        verify(teamAuditEventMapper, never()).insert(any());
    }

    @Test
    void transferOwnershipRollsBackOnBlogUpdateFailure() {
        Team team = new Team();
        team.setId(1L);
        team.setBlogId(100L);
        team.setOwnerUserId(10L);
        team.setLockVersion(0);
        when(teamMapper.selectById(1L)).thenReturn(team);

        TeamMember currentOwnerMember = new TeamMember();
        currentOwnerMember.setId(1001L);
        currentOwnerMember.setRoleCode("OWNER");
        currentOwnerMember.setLockVersion(0);
        when(teamMemberMapper.findActiveMember(1L, 10L)).thenReturn(currentOwnerMember);

        TeamMember newOwnerMember = new TeamMember();
        newOwnerMember.setId(1002L);
        newOwnerMember.setRoleCode("ADMIN");
        newOwnerMember.setLockVersion(0);
        when(teamMemberMapper.findActiveMember(1L, 20L)).thenReturn(newOwnerMember);

        Blog blog = new Blog();
        blog.setId(100L);
        blog.setBlogType("TEAM");
        blog.setLockVersion(0);
        when(blogMapper.selectById(100L)).thenReturn(blog);

        when(teamMapper.updateOwnerWithOptimisticLock(1L, 10L, 20L, 0)).thenReturn(1);
        when(teamMemberMapper.updateRoleWithOptimisticLock(1001L, "ADMIN", 0)).thenReturn(1);
        when(teamMemberMapper.updateRoleWithOptimisticLock(1002L, "OWNER", 0)).thenReturn(1);
        when(blogMapper.updateOwnerWithOptimisticLock(100L, 20L, 0)).thenReturn(0);

        assertThatThrownBy(() -> service.transferOwnership(1L, 10L, 20L, "req-123"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Failed to update blog owner");

        verify(teamAuditEventMapper, never()).insert(any());
    }

    @Test
    void recordAuditEventInsertsEventWithAllFields() {
        service.recordAuditEvent(
                1L,
                100L,
                "MEMBER_ADDED",
                "TEAM_MEMBER",
                200L,
                "req-123",
                "{\"before\":\"data\"}",
                "{\"after\":\"data\"}"
        );

        ArgumentCaptor<TeamAuditEvent> captor = ArgumentCaptor.forClass(TeamAuditEvent.class);
        verify(teamAuditEventMapper).insert(captor.capture());

        TeamAuditEvent event = captor.getValue();
        assertThat(event.getTeamId()).isEqualTo(1L);
        assertThat(event.getActorUserId()).isEqualTo(100L);
        assertThat(event.getEventType()).isEqualTo("MEMBER_ADDED");
        assertThat(event.getTargetType()).isEqualTo("TEAM_MEMBER");
        assertThat(event.getTargetId()).isEqualTo(200L);
        assertThat(event.getRequestId()).isEqualTo("req-123");
        assertThat(event.getBeforeSnapshot()).isEqualTo("{\"before\":\"data\"}");
        assertThat(event.getAfterSnapshot()).isEqualTo("{\"after\":\"data\"}");
        assertThat(event.getOccurredAt()).isNotNull();
    }
}

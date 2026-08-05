package top.pxczxn.community.team.application;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import top.pxczxn.community.article.model.Article;
import top.pxczxn.community.article.persistence.ArticleMapper;
import top.pxczxn.community.article.persistence.ArticleStatusCountRow;
import top.pxczxn.community.blog.model.Blog;
import top.pxczxn.community.blog.persistence.BlogMapper;
import top.pxczxn.community.series.persistence.TeamSeriesMapper;
import top.pxczxn.community.team.model.Team;
import top.pxczxn.community.team.model.TeamAuditEvent;
import top.pxczxn.community.team.model.TeamMember;
import top.pxczxn.community.team.persistence.TeamAuditEventMapper;
import top.pxczxn.community.team.persistence.TeamCountRow;
import top.pxczxn.community.team.persistence.TeamInvitationMapper;
import top.pxczxn.community.team.persistence.TeamMapper;
import top.pxczxn.community.team.persistence.TeamMemberMapper;
import top.pxczxn.community.team.persistence.TeamPermissionMapper;
import top.pxczxn.community.team.submission.persistence.TeamSubmissionMapper;
import top.pxczxn.community.user.model.CommunityUser;
import top.pxczxn.community.user.persistence.CommunityUserMapper;
import top.pxczxn.platform.common.exception.BusinessException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TeamPortalServiceImplTest {
    private TeamMapper teamMapper;
    private TeamMemberMapper memberMapper;
    private BlogMapper blogMapper;
    private CommunityUserMapper userMapper;
    private TeamPermissionMapper permissionMapper;
    private TeamSubmissionMapper submissionMapper;
    private TeamSeriesMapper seriesMapper;
    private TeamInvitationMapper invitationMapper;
    private TeamAuditEventMapper auditEventMapper;
    private ArticleMapper articleMapper;
    private TeamPortalServiceImpl service;

    @BeforeEach
    void setUp() {
        teamMapper = mock(TeamMapper.class);
        memberMapper = mock(TeamMemberMapper.class);
        blogMapper = mock(BlogMapper.class);
        userMapper = mock(CommunityUserMapper.class);
        permissionMapper = mock(TeamPermissionMapper.class);
        submissionMapper = mock(TeamSubmissionMapper.class);
        seriesMapper = mock(TeamSeriesMapper.class);
        invitationMapper = mock(TeamInvitationMapper.class);
        auditEventMapper = mock(TeamAuditEventMapper.class);
        articleMapper = mock(ArticleMapper.class);
        service = new TeamPortalServiceImpl(teamMapper, memberMapper, blogMapper, userMapper,
                permissionMapper, submissionMapper, seriesMapper, invitationMapper, auditEventMapper, articleMapper);
    }

    @Test
    void publicDirectoryIncludesOnlyActiveTeamBlogs() {
        Team team = activeTeam(1L, 10L, 100L);
        when(teamMapper.selectList(any(Wrapper.class))).thenReturn(List.of(team));
        Blog blog = teamBlog(10L, "team-one");
        when(blogMapper.selectBatchIds(List.of(10L))).thenReturn(List.of(blog));

        List<TeamSummaryView> teams = service.listPublicTeams();

        assertThat(teams).singleElement().extracting(TeamSummaryView::slug).isEqualTo("team-one");
    }

    @Test
    void workspaceDeniesCrossTeamUser() {
        when(teamMapper.selectById(1L)).thenReturn(activeTeam(1L, 10L, 100L));
        when(memberMapper.findActiveMember(1L, 999L)).thenReturn(null);

        assertThatThrownBy(() -> service.workspace(999L, 1L)).isInstanceOf(BusinessException.class);
    }

    @Test
    void authorWorkspaceDoesNotExposeMemberOrSettingsCapabilities() {
        when(teamMapper.selectById(1L)).thenReturn(activeTeam(1L, 10L, 100L));
        TeamMember author = new TeamMember(); author.setTeamId(1L); author.setUserId(100L); author.setRoleCode("AUTHOR");
        when(memberMapper.findActiveMember(1L, 100L)).thenReturn(author);
        when(blogMapper.selectById(10L)).thenReturn(teamBlog(10L, "team-one"));
        when(memberMapper.findActiveMembers(1L)).thenReturn(List.of(author));
        CommunityUser user = new CommunityUser(); user.setId(100L); user.setUsername("author"); user.setDisplayName("Author");
        when(userMapper.selectBatchIds(List.of(100L))).thenReturn(List.of(user));
        when(permissionMapper.findPermissionsByRole("AUTHOR")).thenReturn(List.of("EDIT_OWN_ARTICLES"));

        TeamWorkspaceView workspace = service.workspace(100L, 1L);

        assertThat(workspace.capabilities()).containsExactly("OVERVIEW", "ARTICLES");
        assertThat(workspace.permissions()).containsExactly("EDIT_OWN_ARTICLES");
    }

    @Test
    void myTeamsReturnsEmptyForUserWithoutMemberships() {
        when(memberMapper.findActiveMembershipsByUser(999L)).thenReturn(List.of());

        assertThat(service.myTeams(999L)).isEmpty();
    }

    @Test
    void myTeamsEnrichesMembershipsWithCountersAndPermissions() {
        TeamMember membership = new TeamMember();
        membership.setTeamId(1L); membership.setUserId(100L); membership.setRoleCode("ADMIN");
        membership.setJoinedAt(LocalDateTime.of(2026, 8, 1, 10, 0));
        when(memberMapper.findActiveMembershipsByUser(100L)).thenReturn(List.of(membership));
        Team team = activeTeam(1L, 10L, 100L);
        team.setUpdatedAt(LocalDateTime.of(2026, 8, 5, 12, 0));
        when(teamMapper.selectBatchIds(List.of(1L))).thenReturn(List.of(team));
        Blog blog = teamBlog(10L, "team-one");
        blog.setArticleCount(32L); blog.setFollowerCount(1200L);
        when(blogMapper.selectBatchIds(List.of(10L))).thenReturn(List.of(blog));
        when(memberMapper.countActiveMembersByTeams(List.of(1L))).thenReturn(List.of(countRow(1L, 8)));
        when(seriesMapper.countByTeams(List.of(1L))).thenReturn(List.of(countRow(1L, 4)));
        when(submissionMapper.countPendingByTeams(List.of(1L))).thenReturn(List.of(countRow(1L, 3)));
        when(submissionMapper.countRevisionRequiredForAuthor(1L, 100L)).thenReturn(1);
        when(permissionMapper.findPermissionsByRole("ADMIN")).thenReturn(List.of("TEAM_VIEW", "SUBMISSION_REVIEW"));

        List<MyTeamView> mine = service.myTeams(100L);

        assertThat(mine).singleElement().satisfies(view -> {
            assertThat(view.name()).isEqualTo("Team One");
            assertThat(view.slug()).isEqualTo("team-one");
            assertThat(view.viewerRole()).isEqualTo("ADMIN");
            assertThat(view.capabilities()).contains("OVERVIEW", "MEMBERS", "SETTINGS");
            assertThat(view.permissions()).contains("SUBMISSION_REVIEW");
            assertThat(view.memberCount()).isEqualTo(8);
            assertThat(view.articleCount()).isEqualTo(32);
            assertThat(view.seriesCount()).isEqualTo(4);
            assertThat(view.followerCount()).isEqualTo(1200);
            assertThat(view.pendingSubmissionCount()).isEqualTo(3);
            assertThat(view.revisionRequiredCount()).isEqualTo(1);
        });
    }

    @Test
    void myTeamsSkipsDisbandedTeams() {
        TeamMember membership = new TeamMember(); membership.setTeamId(1L); membership.setUserId(100L); membership.setRoleCode("OWNER");
        when(memberMapper.findActiveMembershipsByUser(100L)).thenReturn(List.of(membership));
        Team disbanded = activeTeam(1L, 10L, 100L);
        disbanded.setStatus("DISBANDED");
        when(teamMapper.selectBatchIds(List.of(1L))).thenReturn(List.of(disbanded));

        assertThat(service.myTeams(100L)).isEmpty();
    }

    @Test
    void dashboardDeniesNonMember() {
        when(teamMapper.selectById(1L)).thenReturn(activeTeam(1L, 10L, 100L));
        when(memberMapper.findActiveMember(1L, 999L)).thenReturn(null);

        assertThatThrownBy(() -> service.dashboard(999L, 1L)).isInstanceOf(BusinessException.class);
    }

    @Test
    void dashboardReturnsStatsRecentArticlesTodosAndActivities() {
        when(teamMapper.selectById(1L)).thenReturn(activeTeam(1L, 10L, 100L));
        TeamMember editor = new TeamMember(); editor.setTeamId(1L); editor.setUserId(100L); editor.setRoleCode("EDITOR");
        when(memberMapper.findActiveMember(1L, 100L)).thenReturn(editor);
        when(blogMapper.selectById(10L)).thenReturn(teamBlog(10L, "team-one"));
        when(permissionMapper.findPermissionsByRole("EDITOR")).thenReturn(List.of("SUBMISSION_REVIEW", "MANAGE_SERIES"));
        when(articleMapper.countByBlogGroupedByPublishStatus(10L)).thenReturn(List.of(
                statusRow("PUBLISHED", 5), statusRow("DRAFT", 2), statusRow("PENDING_REVIEW", 1)));
        when(seriesMapper.countByTeams(List.of(1L))).thenReturn(List.of(countRow(1L, 3)));
        when(memberMapper.findActiveMembers(1L)).thenReturn(List.of(editor));
        when(articleMapper.sumViewCountByBlog(10L)).thenReturn(1200L);
        when(articleMapper.sumInteractionCountByBlog(10L)).thenReturn(300L);
        Article recent = new Article();
        recent.setId(501L); recent.setTitle("Team post"); recent.setSlug("team-post");
        recent.setAuthorUserId(100L); recent.setPublishStatus("PUBLISHED"); recent.setReviewStatus("APPROVED");
        recent.setVisibility("PUBLIC"); recent.setViewCount(10L); recent.setLikeCount(2L); recent.setCommentCount(1L);
        when(articleMapper.findRecentByBlog(10L, 6)).thenReturn(List.of(recent));
        CommunityUser author = new CommunityUser(); author.setId(100L); author.setUsername("editor"); author.setDisplayName("Editor");
        when(userMapper.selectBatchIds(List.of(100L))).thenReturn(List.of(author));
        when(submissionMapper.countByTeamAndStatus(1L, "TEAM_PENDING")).thenReturn(2);
        when(submissionMapper.countRevisionRequiredForAuthor(1L, 100L)).thenReturn(0);
        when(invitationMapper.countPendingByTeam(1L)).thenReturn(1);
        when(seriesMapper.countByTeamAndReviewStatus(1L, "PENDING_REVIEW")).thenReturn(1);
        when(articleMapper.countRiskByBlog(10L)).thenReturn(0);
        TeamAuditEvent event = new TeamAuditEvent();
        event.setId(900L); event.setTeamId(1L); event.setActorUserId(100L);
        event.setEventType("MEMBER_INVITED"); event.setTargetType("TEAM_INVITATION"); event.setTargetId(42L);
        event.setOccurredAt(LocalDateTime.of(2026, 8, 4, 9, 0));
        when(auditEventMapper.findRecentByTeam(1L, 20)).thenReturn(List.of(event));

        TeamDashboardView dashboard = service.dashboard(100L, 1L);

        assertThat(dashboard.viewerRole()).isEqualTo("EDITOR");
        assertThat(dashboard.capabilities()).contains("OVERVIEW", "SERIES").doesNotContain("MEMBERS", "SETTINGS");
        assertThat(dashboard.stats().publishedArticleCount()).isEqualTo(5);
        assertThat(dashboard.stats().draftArticleCount()).isEqualTo(2);
        assertThat(dashboard.stats().reviewingArticleCount()).isEqualTo(1);
        assertThat(dashboard.stats().seriesCount()).isEqualTo(3);
        assertThat(dashboard.stats().memberCount()).isEqualTo(1);
        assertThat(dashboard.stats().followerCount()).isEqualTo(5);
        assertThat(dashboard.stats().totalViewCount()).isEqualTo(1200);
        assertThat(dashboard.recentArticles()).singleElement().satisfies(article -> {
            assertThat(article.title()).isEqualTo("Team post");
            assertThat(article.authorDisplayName()).isEqualTo("Editor");
            assertThat(article.viewCount()).isEqualTo(10);
        });
        assertThat(dashboard.todos().pendingSubmissionCount()).isEqualTo(2);
        assertThat(dashboard.todos().pendingInvitationCount()).isEqualTo(1);
        assertThat(dashboard.todos().pendingSeriesReviewCount()).isEqualTo(1);
        assertThat(dashboard.recentActivities()).singleElement().satisfies(activity -> {
            assertThat(activity.eventType()).isEqualTo("MEMBER_INVITED");
            assertThat(activity.actorDisplayName()).isEqualTo("Editor");
        });
    }

    @Test
    void activitiesDenyNonMember() {
        when(teamMapper.selectById(1L)).thenReturn(activeTeam(1L, 10L, 100L));
        when(memberMapper.findActiveMember(1L, 999L)).thenReturn(null);

        assertThatThrownBy(() -> service.activities(999L, 1L, 10)).isInstanceOf(BusinessException.class);
    }

    @Test
    void activitiesCapLimitAndResolveActorNames() {
        when(teamMapper.selectById(1L)).thenReturn(activeTeam(1L, 10L, 100L));
        TeamMember member = new TeamMember(); member.setTeamId(1L); member.setUserId(100L); member.setRoleCode("AUTHOR");
        when(memberMapper.findActiveMember(1L, 100L)).thenReturn(member);
        TeamAuditEvent event = new TeamAuditEvent();
        event.setId(1L); event.setTeamId(1L); event.setActorUserId(100L);
        event.setEventType("SERIES_CREATED"); event.setTargetType("TEAM_SERIES"); event.setTargetId(7L);
        event.setOccurredAt(LocalDateTime.now());
        when(auditEventMapper.findRecentByTeam(1L, 50)).thenReturn(List.of(event));
        CommunityUser actor = new CommunityUser(); actor.setId(100L); actor.setUsername("author"); actor.setDisplayName("Author");
        when(userMapper.selectBatchIds(List.of(100L))).thenReturn(List.of(actor));

        List<TeamActivityView> activities = service.activities(100L, 1L, 999);

        assertThat(activities).singleElement().satisfies(activity -> {
            assertThat(activity.eventType()).isEqualTo("SERIES_CREATED");
            assertThat(activity.actorDisplayName()).isEqualTo("Author");
        });
    }

    @Test
    void teamArticlesDenyNonMember() {
        when(teamMapper.selectById(1L)).thenReturn(activeTeam(1L, 10L, 100L));
        when(memberMapper.findActiveMember(1L, 999L)).thenReturn(null);

        assertThatThrownBy(() -> service.teamArticles(999L, 1L, null)).isInstanceOf(BusinessException.class);
    }

    @Test
    void teamArticlesFilterByPublishStatus() {        when(teamMapper.selectById(1L)).thenReturn(activeTeam(1L, 10L, 100L));
        TeamMember member = new TeamMember(); member.setTeamId(1L); member.setUserId(100L); member.setRoleCode("AUTHOR");
        when(memberMapper.findActiveMember(1L, 100L)).thenReturn(member);
        when(blogMapper.selectById(10L)).thenReturn(teamBlog(10L, "team-one"));
        Article draft = new Article();
        draft.setId(601L); draft.setTitle("Draft post"); draft.setSlug("draft-post");
        draft.setAuthorUserId(100L); draft.setPublishStatus("DRAFT"); draft.setReviewStatus("NOT_SUBMITTED");
        draft.setVisibility("PRIVATE");
        when(articleMapper.findByBlog(10L, "DRAFT", 200)).thenReturn(List.of(draft));
        when(userMapper.selectBatchIds(List.of(100L))).thenReturn(List.of());

        List<TeamArticleBriefView> articles = service.teamArticles(100L, 1L, "draft");

        assertThat(articles).singleElement().satisfies(article -> {
            assertThat(article.title()).isEqualTo("Draft post");
            assertThat(article.publishStatus()).isEqualTo("DRAFT");
        });
    }

    @Test
    void updateSettingsDeniedWithoutManageTeamPermission() {
        when(teamMapper.selectById(1L)).thenReturn(activeTeam(1L, 10L, 100L));
        TeamMember author = new TeamMember(); author.setTeamId(1L); author.setUserId(100L); author.setRoleCode("AUTHOR");
        when(memberMapper.findActiveMember(1L, 100L)).thenReturn(author);
        when(permissionMapper.findPermissionsByRole("AUTHOR")).thenReturn(List.of("EDIT_OWN_ARTICLES"));

        assertThatThrownBy(() -> service.updateSettings(100L, 1L,
                new UpdateTeamSettingsCommand("New Name", null, null, null)))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void updateSettingsThrowsConflictOnOptimisticLockMiss() {
        when(teamMapper.selectById(1L)).thenReturn(activeTeam(1L, 10L, 100L));
        TeamMember admin = new TeamMember(); admin.setTeamId(1L); admin.setUserId(100L); admin.setRoleCode("ADMIN");
        when(memberMapper.findActiveMember(1L, 100L)).thenReturn(admin);
        when(permissionMapper.findPermissionsByRole("ADMIN")).thenReturn(List.of("MANAGE_TEAM"));
        Blog blog = teamBlog(10L, "team-one");
        when(blogMapper.selectById(10L)).thenReturn(blog);
        when(blogMapper.updateProfileWithOptimisticLock(10L, "New Name", null, null, null, 0)).thenReturn(0);

        assertThatThrownBy(() -> service.updateSettings(100L, 1L,
                new UpdateTeamSettingsCommand("New Name", null, null, null)))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void updateSettingsUpdatesProfileAndRecordsAudit() {
        when(teamMapper.selectById(1L)).thenReturn(activeTeam(1L, 10L, 100L));
        TeamMember admin = new TeamMember(); admin.setTeamId(1L); admin.setUserId(100L); admin.setRoleCode("ADMIN");
        when(memberMapper.findActiveMember(1L, 100L)).thenReturn(admin);
        when(permissionMapper.findPermissionsByRole("ADMIN")).thenReturn(List.of("MANAGE_TEAM"));
        Blog blog = teamBlog(10L, "team-one");
        when(blogMapper.selectById(10L)).thenReturn(blog);
        when(blogMapper.updateProfileWithOptimisticLock(10L, "New Name", "New summary", 55L, 66L, 0)).thenReturn(1);

        TeamSummaryView updated = service.updateSettings(100L, 1L,
                new UpdateTeamSettingsCommand("New Name", "New summary", 55L, 66L));

        assertThat(updated.name()).isEqualTo("New Name");
        org.mockito.Mockito.verify(auditEventMapper).insert(org.mockito.ArgumentMatchers.argThat(event ->
                "TEAM_PROFILE_UPDATED".equals(event.getEventType())
                        && Long.valueOf(1L).equals(event.getTeamId())));
    }

    private static Team activeTeam(Long id, Long blogId, Long ownerId) { Team team = new Team(); team.setId(id); team.setBlogId(blogId); team.setOwnerUserId(ownerId); team.setStatus("ACTIVE"); return team; }
    private static Blog teamBlog(Long id, String slug) { Blog blog = new Blog(); blog.setId(id); blog.setBlogType("TEAM"); blog.setStatus("ACTIVE"); blog.setName("Team One"); blog.setSlug(slug); blog.setArticleCount(3L); blog.setFollowerCount(5L); blog.setLockVersion(0); return blog; }
    private static TeamCountRow countRow(Long teamId, int total) { TeamCountRow row = new TeamCountRow(); row.setTeamId(teamId); row.setTotal(total); return row; }
    private static ArticleStatusCountRow statusRow(String status, int total) { ArticleStatusCountRow row = new ArticleStatusCountRow(); row.setStatus(status); row.setTotal(total); return row; }
}

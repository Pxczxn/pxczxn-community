package top.pxczxn.community.team.application;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import top.pxczxn.community.blog.model.Blog;
import top.pxczxn.community.blog.persistence.BlogMapper;
import top.pxczxn.community.team.model.Team;
import top.pxczxn.community.team.model.TeamMember;
import top.pxczxn.community.team.persistence.TeamMapper;
import top.pxczxn.community.team.persistence.TeamMemberMapper;
import top.pxczxn.community.user.model.CommunityUser;
import top.pxczxn.community.user.persistence.CommunityUserMapper;
import top.pxczxn.platform.common.exception.BusinessException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TeamPortalServiceImplTest {
    private TeamMapper teamMapper;
    private TeamMemberMapper memberMapper;
    private BlogMapper blogMapper;
    private CommunityUserMapper userMapper;
    private TeamPortalServiceImpl service;

    @BeforeEach
    void setUp() {
        teamMapper = mock(TeamMapper.class); memberMapper = mock(TeamMemberMapper.class);
        blogMapper = mock(BlogMapper.class); userMapper = mock(CommunityUserMapper.class);
        service = new TeamPortalServiceImpl(teamMapper, memberMapper, blogMapper, userMapper);
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

        TeamWorkspaceView workspace = service.workspace(100L, 1L);

        assertThat(workspace.capabilities()).containsExactly("OVERVIEW", "ARTICLES");
    }

    private static Team activeTeam(Long id, Long blogId, Long ownerId) { Team team = new Team(); team.setId(id); team.setBlogId(blogId); team.setOwnerUserId(ownerId); team.setStatus("ACTIVE"); return team; }
    private static Blog teamBlog(Long id, String slug) { Blog blog = new Blog(); blog.setId(id); blog.setBlogType("TEAM"); blog.setStatus("ACTIVE"); blog.setName("Team One"); blog.setSlug(slug); blog.setArticleCount(3L); blog.setFollowerCount(5L); return blog; }
}

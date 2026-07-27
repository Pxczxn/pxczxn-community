package top.pxczxn.community.article.permission;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import top.pxczxn.community.blog.model.Blog;
import top.pxczxn.community.team.application.TeamAuthorityService;
import top.pxczxn.community.team.model.Team;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TeamBlogArticleRoleResolverTest {

    private TeamAuthorityService teamAuthorityService;
    private TeamBlogArticleRoleResolver resolver;

    @BeforeEach
    void setUp() {
        teamAuthorityService = mock(TeamAuthorityService.class);
        resolver = new TeamBlogArticleRoleResolver(teamAuthorityService);
    }

    @Test
    void resolveReturnsOwnerRoleForTeamOwner() {
        Blog blog = createTeamBlog(100L);
        Team team = createTeam(1L, 100L);

        when(teamAuthorityService.getTeamByBlogId(100L)).thenReturn(team);
        when(teamAuthorityService.getUserRole(200L, 1L)).thenReturn("OWNER");

        Optional<BlogArticleRole> result = resolver.resolve(200L, blog);

        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(BlogArticleRole.OWNER);
    }

    @Test
    void resolveReturnsAdminRoleForTeamAdmin() {
        Blog blog = createTeamBlog(100L);
        Team team = createTeam(1L, 100L);

        when(teamAuthorityService.getTeamByBlogId(100L)).thenReturn(team);
        when(teamAuthorityService.getUserRole(200L, 1L)).thenReturn("ADMIN");

        Optional<BlogArticleRole> result = resolver.resolve(200L, blog);

        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(BlogArticleRole.ADMIN);
    }

    @Test
    void resolveReturnsEditorRoleForTeamEditor() {
        Blog blog = createTeamBlog(100L);
        Team team = createTeam(1L, 100L);

        when(teamAuthorityService.getTeamByBlogId(100L)).thenReturn(team);
        when(teamAuthorityService.getUserRole(200L, 1L)).thenReturn("EDITOR");

        Optional<BlogArticleRole> result = resolver.resolve(200L, blog);

        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(BlogArticleRole.EDITOR);
    }

    @Test
    void resolveReturnsAuthorRoleForTeamAuthor() {
        Blog blog = createTeamBlog(100L);
        Team team = createTeam(1L, 100L);

        when(teamAuthorityService.getTeamByBlogId(100L)).thenReturn(team);
        when(teamAuthorityService.getUserRole(200L, 1L)).thenReturn("AUTHOR");

        Optional<BlogArticleRole> result = resolver.resolve(200L, blog);

        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(BlogArticleRole.AUTHOR);
    }

    @Test
    void resolveReturnsEmptyForNonMember() {
        Blog blog = createTeamBlog(100L);
        Team team = createTeam(1L, 100L);

        when(teamAuthorityService.getTeamByBlogId(100L)).thenReturn(team);
        when(teamAuthorityService.getUserRole(200L, 1L)).thenReturn(null);

        Optional<BlogArticleRole> result = resolver.resolve(200L, blog);

        assertThat(result).isEmpty();
    }

    @Test
    void resolveReturnsEmptyForNonTeamBlog() {
        Blog blog = new Blog();
        blog.setId(100L);
        blog.setBlogType("PERSONAL");

        Optional<BlogArticleRole> result = resolver.resolve(200L, blog);

        assertThat(result).isEmpty();
        verify(teamAuthorityService, never()).getTeamByBlogId(100L);
    }

    @Test
    void resolveReturnsEmptyWhenTeamNotFound() {
        Blog blog = createTeamBlog(100L);

        when(teamAuthorityService.getTeamByBlogId(100L)).thenReturn(null);

        Optional<BlogArticleRole> result = resolver.resolve(200L, blog);

        assertThat(result).isEmpty();
        verify(teamAuthorityService, never()).getUserRole(200L, 1L);
    }

    @Test
    void resolveReturnsEmptyWhenUserIdIsNull() {
        Blog blog = createTeamBlog(100L);

        Optional<BlogArticleRole> result = resolver.resolve(null, blog);

        assertThat(result).isEmpty();
        verify(teamAuthorityService, never()).getTeamByBlogId(100L);
    }

    @Test
    void resolveReturnsEmptyWhenBlogIsNull() {
        Optional<BlogArticleRole> result = resolver.resolve(200L, null);

        assertThat(result).isEmpty();
        verify(teamAuthorityService, never()).getTeamByBlogId(100L);
    }

    @Test
    void resolveReturnsEmptyForUnknownTeamRole() {
        Blog blog = createTeamBlog(100L);
        Team team = createTeam(1L, 100L);

        when(teamAuthorityService.getTeamByBlogId(100L)).thenReturn(team);
        when(teamAuthorityService.getUserRole(200L, 1L)).thenReturn("UNKNOWN_ROLE");

        Optional<BlogArticleRole> result = resolver.resolve(200L, blog);

        assertThat(result).isEmpty();
    }

    private Blog createTeamBlog(Long id) {
        Blog blog = new Blog();
        blog.setId(id);
        blog.setBlogType("TEAM");
        return blog;
    }

    private Team createTeam(Long id, Long blogId) {
        Team team = new Team();
        team.setId(id);
        team.setBlogId(blogId);
        return team;
    }
}

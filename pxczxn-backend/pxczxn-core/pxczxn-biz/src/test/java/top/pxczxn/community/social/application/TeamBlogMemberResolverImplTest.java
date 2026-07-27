package top.pxczxn.community.social.application;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import top.pxczxn.community.blog.model.Blog;
import top.pxczxn.community.blog.persistence.BlogMapper;
import top.pxczxn.community.team.application.TeamAuthorityService;
import top.pxczxn.community.team.model.Team;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TeamBlogMemberResolverImplTest {

    private TeamAuthorityService teamAuthorityService;
    private BlogMapper blogMapper;
    private TeamBlogMemberResolverImpl resolver;

    @BeforeEach
    void setUp() {
        teamAuthorityService = mock(TeamAuthorityService.class);
        blogMapper = mock(BlogMapper.class);
        resolver = new TeamBlogMemberResolverImpl(teamAuthorityService, blogMapper);
    }

    @Test
    void isMemberReturnsTrueForTeamBlogActiveMember() {
        Blog blog = new Blog();
        blog.setId(100L);
        blog.setBlogType("TEAM");
        when(blogMapper.selectById(100L)).thenReturn(blog);

        Team team = new Team();
        team.setId(1L);
        when(teamAuthorityService.getTeamByBlogId(100L)).thenReturn(team);
        when(teamAuthorityService.isMember(200L, 1L)).thenReturn(true);

        boolean result = resolver.isMember(200L, 100L);

        assertThat(result).isTrue();
        verify(teamAuthorityService).isMember(200L, 1L);
    }

    @Test
    void isMemberReturnsFalseForTeamBlogNonMember() {
        Blog blog = new Blog();
        blog.setId(100L);
        blog.setBlogType("TEAM");
        when(blogMapper.selectById(100L)).thenReturn(blog);

        Team team = new Team();
        team.setId(1L);
        when(teamAuthorityService.getTeamByBlogId(100L)).thenReturn(team);
        when(teamAuthorityService.isMember(200L, 1L)).thenReturn(false);

        boolean result = resolver.isMember(200L, 100L);

        assertThat(result).isFalse();
    }

    @Test
    void isMemberReturnsFalseForNonTeamBlog() {
        Blog blog = new Blog();
        blog.setId(100L);
        blog.setBlogType("PERSONAL");
        when(blogMapper.selectById(100L)).thenReturn(blog);

        boolean result = resolver.isMember(200L, 100L);

        assertThat(result).isFalse();
        verify(teamAuthorityService, never()).getTeamByBlogId(100L);
        verify(teamAuthorityService, never()).isMember(200L, 1L);
    }

    @Test
    void isMemberReturnsFalseWhenBlogNotFound() {
        when(blogMapper.selectById(100L)).thenReturn(null);

        boolean result = resolver.isMember(200L, 100L);

        assertThat(result).isFalse();
    }

    @Test
    void isMemberReturnsFalseWhenTeamNotFoundForBlog() {
        Blog blog = new Blog();
        blog.setId(100L);
        blog.setBlogType("TEAM");
        when(blogMapper.selectById(100L)).thenReturn(blog);
        when(teamAuthorityService.getTeamByBlogId(100L)).thenReturn(null);

        boolean result = resolver.isMember(200L, 100L);

        assertThat(result).isFalse();
        verify(teamAuthorityService, never()).isMember(200L, 1L);
    }

    @Test
    void isMemberReturnsFalseWhenUserIdIsNull() {
        boolean result = resolver.isMember(null, 100L);

        assertThat(result).isFalse();
        verify(blogMapper, never()).selectById(100L);
    }

    @Test
    void isMemberReturnsFalseWhenBlogIdIsNull() {
        boolean result = resolver.isMember(200L, null);

        assertThat(result).isFalse();
        verify(blogMapper, never()).selectById(null);
    }
}

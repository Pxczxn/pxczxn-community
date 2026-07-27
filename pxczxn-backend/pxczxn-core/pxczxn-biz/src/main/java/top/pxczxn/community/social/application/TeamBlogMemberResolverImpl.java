package top.pxczxn.community.social.application;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import top.pxczxn.community.blog.model.Blog;
import top.pxczxn.community.blog.persistence.BlogMapper;
import top.pxczxn.community.team.application.TeamAuthorityService;
import top.pxczxn.community.team.model.Team;

/**
 * Implementation of TeamBlogMemberResolver for TEAM_MEMBERS comment scope.
 * Only team members can comment when scope is TEAM_MEMBERS.
 */
@Component
@RequiredArgsConstructor
public class TeamBlogMemberResolverImpl implements TeamBlogMemberResolver {

    private final TeamAuthorityService teamAuthorityService;
    private final BlogMapper blogMapper;

    @Override
    public boolean isMember(Long userId, Long blogId) {
        if (userId == null || blogId == null) {
            return false;
        }

        Blog blog = blogMapper.selectById(blogId);
        if (blog == null || !"TEAM".equals(blog.getBlogType())) {
            return false;
        }

        Team team = teamAuthorityService.getTeamByBlogId(blogId);
        if (team == null) {
            return false;
        }

        return teamAuthorityService.isMember(userId, team.getId());
    }
}

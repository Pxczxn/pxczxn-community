package top.pxczxn.community.article.permission;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import top.pxczxn.community.blog.model.Blog;
import top.pxczxn.community.team.application.TeamAuthorityService;
import top.pxczxn.community.team.model.Team;

import java.util.Optional;

/**
 * Resolves article roles for team blog members.
 * Only applies to TEAM type blogs.
 */
@Component
@RequiredArgsConstructor
public class TeamBlogArticleRoleResolver implements BlogArticleRoleResolver {

    private final TeamAuthorityService teamAuthorityService;

    @Override
    public Optional<BlogArticleRole> resolve(Long userId, Blog blog) {
        if (userId == null || blog == null || !"TEAM".equals(blog.getBlogType())) {
            return Optional.empty();
        }

        Team team = teamAuthorityService.getTeamByBlogId(blog.getId());
        if (team == null) {
            return Optional.empty();
        }

        String roleCode = teamAuthorityService.getUserRole(userId, team.getId());
        if (roleCode == null) {
            return Optional.empty();
        }

        // Map team role to article role
        switch (roleCode) {
            case "OWNER":
                return Optional.of(BlogArticleRole.OWNER);
            case "ADMIN":
                return Optional.of(BlogArticleRole.ADMIN);
            case "EDITOR":
                return Optional.of(BlogArticleRole.EDITOR);
            case "AUTHOR":
                return Optional.of(BlogArticleRole.AUTHOR);
            default:
                return Optional.empty();
        }
    }
}

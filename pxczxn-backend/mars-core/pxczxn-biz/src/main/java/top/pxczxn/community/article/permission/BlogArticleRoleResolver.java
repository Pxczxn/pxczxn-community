package top.pxczxn.community.article.permission;

import top.pxczxn.community.blog.model.Blog;

import java.util.Optional;

/**
 * Extension point for blog membership. M1 provides the personal-blog resolver;
 * the team module can add a resolver without changing article services.
 */
@FunctionalInterface
public interface BlogArticleRoleResolver {

    Optional<BlogArticleRole> resolve(Long userId, Blog blog);
}

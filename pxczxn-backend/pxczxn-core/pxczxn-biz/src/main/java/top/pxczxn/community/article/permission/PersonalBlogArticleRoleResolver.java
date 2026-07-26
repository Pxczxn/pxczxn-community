package top.pxczxn.community.article.permission;

import org.springframework.stereotype.Component;
import top.pxczxn.community.blog.model.Blog;

import java.util.Optional;

@Component
public class PersonalBlogArticleRoleResolver implements BlogArticleRoleResolver {

    @Override
    public Optional<BlogArticleRole> resolve(Long userId, Blog blog) {
        if (userId == null
                || blog == null
                || !"PERSONAL".equals(blog.getBlogType())
                || !userId.equals(blog.getOwnerUserId())) {
            return Optional.empty();
        }
        return Optional.of(BlogArticleRole.OWNER);
    }
}

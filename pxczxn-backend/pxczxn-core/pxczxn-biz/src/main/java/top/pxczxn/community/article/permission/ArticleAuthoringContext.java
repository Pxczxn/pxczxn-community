package top.pxczxn.community.article.permission;

import top.pxczxn.community.blog.model.Blog;
import top.pxczxn.community.user.model.CommunityUser;

public record ArticleAuthoringContext(
        CommunityUser user,
        Blog blog
) {
}

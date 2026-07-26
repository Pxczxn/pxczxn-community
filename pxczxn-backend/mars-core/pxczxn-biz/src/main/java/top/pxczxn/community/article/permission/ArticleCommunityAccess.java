package top.pxczxn.community.article.permission;

import top.pxczxn.community.article.model.Article;
import top.pxczxn.community.blog.model.Blog;
import top.pxczxn.community.user.model.CommunityUser;

public record ArticleCommunityAccess(
        CommunityUser actor,
        Blog blog,
        Article article,
        BlogArticleRole blogRole
) {
}

package top.pxczxn.community.article.permission;

import top.pxczxn.community.article.model.Article;
import top.pxczxn.community.blog.model.Blog;
import top.pxczxn.community.user.model.CommunityUser;

public record ArticlePublicAccess(
        CommunityUser viewer,
        CommunityUser author,
        Blog blog,
        Article article,
        BlogArticleRole viewerBlogRole
) {
}

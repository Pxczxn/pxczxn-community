package top.pxczxn.community.article.permission;

import top.pxczxn.community.article.model.Article;
import top.pxczxn.community.blog.model.Blog;

public record ArticlePlatformAccess(
        Blog blog,
        Article article
) {
}

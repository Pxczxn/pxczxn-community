package top.pxczxn.community.article.application;

import top.pxczxn.platform.common.exception.BusinessException;
import top.pxczxn.community.article.model.Article;
import top.pxczxn.community.blog.model.Blog;

public final class ArticlePublicationPolicy {

    private ArticlePublicationPolicy() {
    }

    public static boolean publishesImmediately(Article article) {
        return article != null
                && "IMMEDIATE".equals(article.getPublishMethod());
    }

    public static String canonicalPath(Blog blog, Article article) {
        if (blog == null
                || blog.getSlug() == null
                || blog.getSlug().isBlank()
                || article == null
                || article.getId() == null
                || article.getSlug() == null
                || article.getSlug().isBlank()) {
            throw new BusinessException(409, "文章公开地址信息不完整");
        }
        return "/" + blog.getSlug()
                + "/" + article.getId()
                + "/" + article.getSlug();
    }
}

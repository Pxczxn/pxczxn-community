package top.pxczxn.community.article.application;

public record PublicArticleQuery(
        String categorySlug,
        Integer pageNum,
        Integer pageSize
) {
}

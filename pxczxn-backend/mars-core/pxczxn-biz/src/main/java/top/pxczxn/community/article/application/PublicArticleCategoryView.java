package top.pxczxn.community.article.application;

public record PublicArticleCategoryView(
        Long categoryId,
        String name,
        String slug,
        String description,
        boolean defaultCategory
) {
}

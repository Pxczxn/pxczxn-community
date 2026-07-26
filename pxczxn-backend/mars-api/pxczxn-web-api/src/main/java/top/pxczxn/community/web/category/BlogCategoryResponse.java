package top.pxczxn.community.web.category;

import top.pxczxn.community.blog.application.BlogCategoryView;

public record BlogCategoryResponse(
        String categoryId,
        String name,
        String slug,
        String description,
        int sortOrder,
        boolean defaultCategory,
        long articleCount
) {
    static BlogCategoryResponse from(BlogCategoryView category) {
        return new BlogCategoryResponse(
                category.categoryId().toString(),
                category.name(),
                category.slug(),
                category.description(),
                category.sortOrder(),
                category.defaultCategory(),
                category.articleCount()
        );
    }
}

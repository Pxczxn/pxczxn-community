package top.pxczxn.community.blog.application;

public record BlogCategoryView(
        Long categoryId,
        String name,
        String slug,
        String description,
        int sortOrder,
        boolean defaultCategory,
        long articleCount
) {
}

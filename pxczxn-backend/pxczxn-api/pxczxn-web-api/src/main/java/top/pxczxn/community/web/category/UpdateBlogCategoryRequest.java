package top.pxczxn.community.web.category;

public record UpdateBlogCategoryRequest(
        String name,
        String slug,
        String description,
        Integer sortOrder
) {
}

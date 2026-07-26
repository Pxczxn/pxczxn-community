package top.pxczxn.community.blog.application;

public record UpdateBlogCategoryCommand(
        String name,
        String slug,
        String description,
        Integer sortOrder
) {
}

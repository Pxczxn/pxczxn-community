package top.pxczxn.community.blog.application;

public record CreateBlogCategoryCommand(
        String name,
        String slug,
        String description,
        Integer sortOrder
) {
}

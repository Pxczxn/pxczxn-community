package top.pxczxn.community.web.category;

public record CreateBlogCategoryRequest(
        String name,
        String slug,
        String description,
        Integer sortOrder
) {
}

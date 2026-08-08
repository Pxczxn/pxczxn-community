package top.pxczxn.community.web.category;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateBlogCategoryRequest(
        @NotBlank(message = "分类名称不能为空")
        @Size(max = 50, message = "分类名称不能超过 50 个字符")
        String name,
        String slug,
        @Size(max = 200, message = "分类描述不能超过 200 个字符")
        String description,
        Integer sortOrder
) {
}

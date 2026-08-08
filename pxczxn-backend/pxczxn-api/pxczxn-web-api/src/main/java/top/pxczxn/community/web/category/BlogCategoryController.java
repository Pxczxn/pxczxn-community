package top.pxczxn.community.web.category;

import jakarta.validation.Valid;
import top.pxczxn.platform.common.result.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import top.pxczxn.community.blog.application.BlogCategoryService;
import top.pxczxn.community.blog.application.CreateBlogCategoryCommand;
import top.pxczxn.community.blog.application.UpdateBlogCategoryCommand;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/blogs/me/categories")
public class BlogCategoryController {

    private final BlogCategoryService categoryService;

    @GetMapping
    public Result<List<BlogCategoryResponse>> list() {
        return Result.ok(categoryService.listMine()
                .stream()
                .map(BlogCategoryResponse::from)
                .toList());
    }

    @PostMapping
    public Result<BlogCategoryResponse> create(
            @Valid @RequestBody CreateBlogCategoryRequest request
    ) {
        return Result.ok(BlogCategoryResponse.from(categoryService.create(
                new CreateBlogCategoryCommand(
                        request.name(),
                        request.slug(),
                        request.description(),
                        request.sortOrder()
                )
        )));
    }

    @PatchMapping("/{categoryId}")
    public Result<BlogCategoryResponse> update(
            @PathVariable Long categoryId,
            @Valid @RequestBody UpdateBlogCategoryRequest request
    ) {
        return Result.ok(BlogCategoryResponse.from(categoryService.update(
                categoryId,
                new UpdateBlogCategoryCommand(
                        request.name(),
                        request.slug(),
                        request.description(),
                        request.sortOrder()
                )
        )));
    }

    @DeleteMapping("/{categoryId}")
    public Result<Void> delete(@PathVariable Long categoryId) {
        categoryService.delete(categoryId);
        return Result.ok();
    }
}

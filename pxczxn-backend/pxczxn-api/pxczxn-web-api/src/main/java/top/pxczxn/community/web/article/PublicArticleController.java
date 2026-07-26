package top.pxczxn.community.web.article;

import top.pxczxn.platform.common.result.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import top.pxczxn.community.article.application.PublicArticleQuery;
import top.pxczxn.community.article.application.PublicArticleService;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/public")
public class PublicArticleController {

    private final PublicArticleService articleService;

    @GetMapping("/articles/{articleId}")
    public Result<PublicArticleDetailResponse> detail(
            @PathVariable Long articleId
    ) {
        return Result.ok(PublicArticleDetailResponse.from(
                articleService.detail(articleId)
        ));
    }

    @GetMapping("/articles")
    public Result<PublicArticlePageResponse> discover(
            @RequestParam(required = false) Integer pageNum,
            @RequestParam(required = false) Integer pageSize
    ) {
        return Result.ok(PublicArticlePageResponse.from(
                articleService.discover(new PublicArticleQuery(null, pageNum, pageSize))
        ));
    }

    @GetMapping("/blogs/{blogSlug}/articles")
    public Result<PublicArticlePageResponse> page(
            @PathVariable String blogSlug,
            @RequestParam(required = false) String categorySlug,
            @RequestParam(required = false) Integer pageNum,
            @RequestParam(required = false) Integer pageSize
    ) {
        return Result.ok(PublicArticlePageResponse.from(
                articleService.page(
                        blogSlug,
                        new PublicArticleQuery(
                                categorySlug,
                                pageNum,
                                pageSize
                        )
                )
        ));
    }

    @GetMapping("/blogs/{blogSlug}/categories")
    public Result<List<PublicArticleCategoryResponse>> categories(
            @PathVariable String blogSlug
    ) {
        return Result.ok(articleService.categories(blogSlug).stream()
                .map(PublicArticleCategoryResponse::from)
                .toList());
    }
}

package top.pxczxn.community.admin.query;

import cn.dev33.satoken.annotation.SaCheckPermission;
import top.pxczxn.platform.common.exception.BusinessException;
import top.pxczxn.platform.common.result.PageResult;
import top.pxczxn.platform.common.result.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.util.StringUtils;
import top.pxczxn.community.admin.application.AdminCommunityPageView;
import top.pxczxn.community.admin.application.AdminCommunityQueryService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin-api/community")
public class AdminCommunityQueryController {

    private final AdminCommunityQueryService queryService;

    @GetMapping("/dashboard")
    @SaCheckPermission("community:dashboard:view")
    public Result<AdminCommunityDashboardResponse> dashboard() {
        return Result.ok(AdminCommunityDashboardResponse.from(
                queryService.dashboard()
        ));
    }

    @GetMapping("/users")
    @SaCheckPermission("community:user:list")
    public Result<PageResult<AdminCommunityUserResponse>> users(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String verificationStatus,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize
    ) {
        var page = queryService.users(
                keyword,
                status,
                verificationStatus,
                pageNum,
                pageSize
        );
        return Result.ok(mapPage(
                page,
                page.list().stream()
                        .map(AdminCommunityUserResponse::from)
                        .toList()
        ));
    }

    @GetMapping("/blogs")
    @SaCheckPermission("community:blog:list")
    public Result<PageResult<AdminCommunityBlogResponse>> blogs(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String blogType,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize
    ) {
        var page = queryService.blogs(
                keyword,
                blogType,
                status,
                pageNum,
                pageSize
        );
        return Result.ok(mapPage(
                page,
                page.list().stream()
                        .map(AdminCommunityBlogResponse::from)
                        .toList()
        ));
    }

    @GetMapping("/articles")
    @SaCheckPermission("community:article:list")
    public Result<PageResult<AdminCommunityArticleResponse>> articles(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String publishStatus,
            @RequestParam(required = false) String reviewStatus,
            @RequestParam(required = false) String visibility,
            @RequestParam(required = false) String blogId,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize
    ) {
        var page = queryService.articles(
                keyword,
                publishStatus,
                reviewStatus,
                visibility,
                optionalId(blogId, "博客"),
                pageNum,
                pageSize
        );
        return Result.ok(mapPage(
                page,
                page.list().stream()
                        .map(AdminCommunityArticleResponse::from)
                        .toList()
        ));
    }

    @GetMapping("/articles/{articleId}")
    @SaCheckPermission("community:article:query")
    public Result<AdminCommunityArticleResponse> article(
            @PathVariable Long articleId
    ) {
        return Result.ok(AdminCommunityArticleResponse.from(
                queryService.article(articleId)
        ));
    }

    private static <S, T> PageResult<T> mapPage(
            AdminCommunityPageView<S> page,
            java.util.List<T> list
    ) {
        return PageResult.of(
                list,
                page.total(),
                page.pageNum(),
                page.pageSize()
        );
    }

    private static Long optionalId(String value, String label) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            long parsed = Long.parseLong(value.trim());
            if (parsed <= 0) {
                throw new NumberFormatException();
            }
            return parsed;
        } catch (NumberFormatException exception) {
            throw new BusinessException(400, label + " ID 无效");
        }
    }
}

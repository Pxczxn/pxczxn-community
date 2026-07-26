package top.pxczxn.community.admin.moderation;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.stp.StpUtil;
import top.pxczxn.platform.common.result.PageResult;
import top.pxczxn.platform.common.result.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import top.pxczxn.community.moderation.application.AdminArticleReviewQuery;
import top.pxczxn.community.moderation.application.AdminArticleReviewService;
import top.pxczxn.community.moderation.application.ClaimArticleReviewCommand;
import top.pxczxn.community.moderation.application.DecideArticleReviewCommand;

import java.time.LocalDateTime;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin-api/community/reviews")
public class AdminArticleReviewController {

    private final AdminArticleReviewService reviewService;

    @GetMapping
    @SaCheckPermission("community:review:list")
    public Result<PageResult<AdminArticleReviewListItemResponse>> page(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String riskLevel,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime submittedFrom,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime submittedTo,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize
    ) {
        var view = reviewService.page(new AdminArticleReviewQuery(
                status,
                riskLevel,
                submittedFrom,
                submittedTo,
                pageNum == null ? 1 : pageNum,
                pageSize == null ? 20 : pageSize
        ));
        return Result.ok(PageResult.of(
                view.list().stream()
                        .map(AdminArticleReviewListItemResponse::from)
                        .toList(),
                view.total(),
                view.pageNum(),
                view.pageSize()
        ));
    }

    @GetMapping("/{taskId}")
    @SaCheckPermission("community:review:query")
    public Result<AdminArticleReviewDetailResponse> detail(
            @PathVariable Long taskId
    ) {
        return Result.ok(AdminArticleReviewDetailResponse.from(
                reviewService.detail(taskId)
        ));
    }

    @PostMapping("/{taskId}/claim")
    @SaCheckPermission("community:review:claim")
    public Result<AdminArticleReviewDetailResponse> claim(
            @PathVariable Long taskId,
            @RequestBody ClaimAdminArticleReviewRequest request
    ) {
        return Result.ok(AdminArticleReviewDetailResponse.from(
                reviewService.claim(
                        taskId,
                        StpUtil.getLoginIdAsLong(),
                        new ClaimArticleReviewCommand(
                                request == null
                                        ? null
                                        : request.expectedTaskLockVersion()
                        )
                )
        ));
    }

    @PostMapping("/{taskId}/approve")
    @SaCheckPermission("community:review:approve")
    public Result<AdminArticleReviewDetailResponse> approve(
            @PathVariable Long taskId,
            @RequestBody DecideAdminArticleReviewRequest request
    ) {
        return Result.ok(AdminArticleReviewDetailResponse.from(
                reviewService.approve(
                        taskId,
                        StpUtil.getLoginIdAsLong(),
                        toCommand(request)
                )
        ));
    }

    @PostMapping("/{taskId}/revision")
    @SaCheckPermission("community:review:revision")
    public Result<AdminArticleReviewDetailResponse> requestRevision(
            @PathVariable Long taskId,
            @RequestBody DecideAdminArticleReviewRequest request
    ) {
        return Result.ok(AdminArticleReviewDetailResponse.from(
                reviewService.requestRevision(
                        taskId,
                        StpUtil.getLoginIdAsLong(),
                        toCommand(request)
                )
        ));
    }

    @PostMapping("/{taskId}/reject")
    @SaCheckPermission("community:review:reject")
    public Result<AdminArticleReviewDetailResponse> reject(
            @PathVariable Long taskId,
            @RequestBody DecideAdminArticleReviewRequest request
    ) {
        return Result.ok(AdminArticleReviewDetailResponse.from(
                reviewService.reject(
                        taskId,
                        StpUtil.getLoginIdAsLong(),
                        toCommand(request)
                )
        ));
    }

    private static DecideArticleReviewCommand toCommand(
            DecideAdminArticleReviewRequest request
    ) {
        return request == null
                ? null
                : new DecideArticleReviewCommand(
                        request.expectedTaskLockVersion(),
                        request.reason()
                );
    }
}

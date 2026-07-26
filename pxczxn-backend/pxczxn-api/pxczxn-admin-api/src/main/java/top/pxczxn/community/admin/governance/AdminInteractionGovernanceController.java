package top.pxczxn.community.admin.governance;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.stp.StpUtil;
import top.pxczxn.platform.common.exception.BusinessException;
import top.pxczxn.platform.common.result.PageResult;
import top.pxczxn.platform.common.result.Result;
import top.pxczxn.platform.system.annotation.Log;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import top.pxczxn.community.admin.application.AdminCommunityPageView;
import top.pxczxn.community.admin.application.AdminGovernanceTarget;
import top.pxczxn.community.admin.application.AdminInteractionGovernanceService;

import java.util.List;
import java.util.Locale;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin-api/community")
public class AdminInteractionGovernanceController {

    private final AdminInteractionGovernanceService service;

    @GetMapping("/comments")
    @SaCheckPermission("community:comment:list")
    public Result<PageResult<AdminCommunityCommentResponse>> comments(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String targetType,
            @RequestParam(required = false) String targetId,
            @RequestParam(required = false) String authorUserId,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize
    ) {
        var page = service.comments(
                keyword,
                status,
                targetType,
                optionalId(targetId, "目标"),
                optionalId(authorUserId, "作者"),
                pageNum,
                pageSize
        );
        return Result.ok(mapPage(
                page,
                page.list().stream()
                        .map(AdminCommunityCommentResponse::from)
                        .toList()
        ));
    }

    @GetMapping("/comments/{commentId}")
    @SaCheckPermission("community:comment:query")
    public Result<AdminCommunityCommentDetailResponse> comment(
            @PathVariable Long commentId
    ) {
        return Result.ok(AdminCommunityCommentDetailResponse.from(
                service.comment(commentId)
        ));
    }

    @GetMapping("/moments")
    @SaCheckPermission("community:moment:list")
    public Result<PageResult<AdminCommunityMomentResponse>> moments(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String visibility,
            @RequestParam(required = false) String momentType,
            @RequestParam(required = false) String blogId,
            @RequestParam(required = false) String actorUserId,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize
    ) {
        var page = service.moments(
                keyword,
                status,
                visibility,
                momentType,
                optionalId(blogId, "博客"),
                optionalId(actorUserId, "发布者"),
                pageNum,
                pageSize
        );
        return Result.ok(mapPage(
                page,
                page.list().stream()
                        .map(AdminCommunityMomentResponse::from)
                        .toList()
        ));
    }

    @GetMapping("/moments/{momentId}")
    @SaCheckPermission("community:moment:query")
    public Result<AdminCommunityMomentDetailResponse> moment(
            @PathVariable Long momentId
    ) {
        return Result.ok(AdminCommunityMomentDetailResponse.from(
                service.moment(momentId)
        ));
    }

    @GetMapping("/interactions")
    @SaCheckPermission("community:interaction:list")
    public Result<PageResult<AdminCommunityInteractionResponse>>
    interactions(
            @RequestParam String interactionType,
            @RequestParam(required = false) String targetType,
            @RequestParam(required = false) String targetId,
            @RequestParam(required = false) String actorUserId,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize
    ) {
        var page = service.interactions(
                interactionType,
                targetType,
                optionalId(targetId, "目标"),
                optionalId(actorUserId, "用户"),
                pageNum,
                pageSize
        );
        return Result.ok(mapPage(
                page,
                page.list().stream()
                        .map(AdminCommunityInteractionResponse::from)
                        .toList()
        ));
    }

    @PostMapping("/comments/{commentId}/approve")
    @SaCheckPermission("community:comment:approve")
    @Log(
            title = "通过评论审核",
            businessType = Log.BusinessType.UPDATE
    )
    public Result<AdminGovernanceResultResponse> approveComment(
            @PathVariable Long commentId,
            @RequestBody(required = false) AdminGovernanceRequest request
    ) {
        return commentAction(commentId, "APPROVE", request);
    }

    @PostMapping("/comments/{commentId}/reject")
    @SaCheckPermission("community:comment:reject")
    @Log(
            title = "驳回评论审核",
            businessType = Log.BusinessType.UPDATE
    )
    public Result<AdminGovernanceResultResponse> rejectComment(
            @PathVariable Long commentId,
            @RequestBody(required = false) AdminGovernanceRequest request
    ) {
        return commentAction(commentId, "REJECT", request);
    }

    @PostMapping("/comments/{commentId}/take-down")
    @SaCheckPermission("community:comment:takeDown")
    @Log(
            title = "下架评论",
            businessType = Log.BusinessType.UPDATE
    )
    public Result<AdminGovernanceResultResponse> takeDownComment(
            @PathVariable Long commentId,
            @RequestBody(required = false) AdminGovernanceRequest request
    ) {
        return commentAction(commentId, "TAKE_DOWN", request);
    }

    @PostMapping("/comments/{commentId}/restore")
    @SaCheckPermission("community:comment:restore")
    @Log(
            title = "恢复评论",
            businessType = Log.BusinessType.UPDATE
    )
    public Result<AdminGovernanceResultResponse> restoreComment(
            @PathVariable Long commentId,
            @RequestBody(required = false) AdminGovernanceRequest request
    ) {
        return commentAction(commentId, "RESTORE", request);
    }

    @PostMapping("/moments/{momentId}/approve")
    @SaCheckPermission("community:moment:approve")
    @Log(
            title = "通过动态审核",
            businessType = Log.BusinessType.UPDATE
    )
    public Result<AdminGovernanceResultResponse> approveMoment(
            @PathVariable Long momentId,
            @RequestBody(required = false) AdminGovernanceRequest request
    ) {
        return momentAction(momentId, "APPROVE", request);
    }

    @PostMapping("/moments/{momentId}/reject")
    @SaCheckPermission("community:moment:reject")
    @Log(
            title = "驳回动态审核",
            businessType = Log.BusinessType.UPDATE
    )
    public Result<AdminGovernanceResultResponse> rejectMoment(
            @PathVariable Long momentId,
            @RequestBody(required = false) AdminGovernanceRequest request
    ) {
        return momentAction(momentId, "REJECT", request);
    }

    @PostMapping("/moments/{momentId}/take-down")
    @SaCheckPermission("community:moment:takeDown")
    @Log(
            title = "下架动态",
            businessType = Log.BusinessType.UPDATE
    )
    public Result<AdminGovernanceResultResponse> takeDownMoment(
            @PathVariable Long momentId,
            @RequestBody(required = false) AdminGovernanceRequest request
    ) {
        return momentAction(momentId, "TAKE_DOWN", request);
    }

    @PostMapping("/moments/{momentId}/restore")
    @SaCheckPermission("community:moment:restore")
    @Log(
            title = "恢复动态",
            businessType = Log.BusinessType.UPDATE
    )
    public Result<AdminGovernanceResultResponse> restoreMoment(
            @PathVariable Long momentId,
            @RequestBody(required = false) AdminGovernanceRequest request
    ) {
        return momentAction(momentId, "RESTORE", request);
    }

    @PostMapping("/comments/batch/{action}/execute")
    @SaCheckPermission("community:comment:batch")
    @Log(
            title = "批量治理评论",
            businessType = Log.BusinessType.UPDATE
    )
    public Result<List<AdminGovernanceResultResponse>> batchComments(
            @PathVariable String action,
            @RequestBody(required = false)
            AdminGovernanceBatchRequest request
    ) {
        String normalizedAction = action(action);
        StpUtil.checkPermission(
                "community:comment:" + permissionAction(normalizedAction)
        );
        List<AdminGovernanceTarget> targets = targets(request);
        return Result.ok(service.moderateComments(
                targets,
                StpUtil.getLoginIdAsLong(),
                normalizedAction,
                request == null ? null : request.reason()
        ).stream().map(AdminGovernanceResultResponse::from).toList());
    }

    @PostMapping("/moments/batch/{action}/execute")
    @SaCheckPermission("community:moment:batch")
    @Log(
            title = "批量治理动态",
            businessType = Log.BusinessType.UPDATE
    )
    public Result<List<AdminGovernanceResultResponse>> batchMoments(
            @PathVariable String action,
            @RequestBody(required = false)
            AdminGovernanceBatchRequest request
    ) {
        String normalizedAction = action(action);
        StpUtil.checkPermission(
                "community:moment:" + permissionAction(normalizedAction)
        );
        List<AdminGovernanceTarget> targets = targets(request);
        return Result.ok(service.moderateMoments(
                targets,
                StpUtil.getLoginIdAsLong(),
                normalizedAction,
                request == null ? null : request.reason()
        ).stream().map(AdminGovernanceResultResponse::from).toList());
    }

    private Result<AdminGovernanceResultResponse> commentAction(
            Long commentId,
            String action,
            AdminGovernanceRequest request
    ) {
        return Result.ok(AdminGovernanceResultResponse.from(
                service.moderateComment(
                        commentId,
                        StpUtil.getLoginIdAsLong(),
                        action,
                        request == null
                                ? null
                                : request.expectedLockVersion(),
                        request == null ? null : request.reason()
                )
        ));
    }

    private Result<AdminGovernanceResultResponse> momentAction(
            Long momentId,
            String action,
            AdminGovernanceRequest request
    ) {
        return Result.ok(AdminGovernanceResultResponse.from(
                service.moderateMoment(
                        momentId,
                        StpUtil.getLoginIdAsLong(),
                        action,
                        request == null
                                ? null
                                : request.expectedLockVersion(),
                        request == null ? null : request.reason()
                )
        ));
    }

    private static List<AdminGovernanceTarget> targets(
            AdminGovernanceBatchRequest request
    ) {
        if (request == null || request.targets() == null) {
            return null;
        }
        return request.targets().stream()
                .map(target -> {
                    if (target == null) {
                        return null;
                    }
                    return new AdminGovernanceTarget(
                            requiredId(target.id(), "治理目标"),
                            target.expectedLockVersion()
                    );
                })
                .toList();
    }

    private static String action(String raw) {
        if (!StringUtils.hasText(raw)) {
            throw new BusinessException(400, "治理动作不能为空");
        }
        String value = raw.trim()
                .replace('-', '_')
                .toUpperCase(Locale.ROOT);
        if (!List.of(
                "APPROVE", "REJECT", "TAKE_DOWN", "RESTORE"
        ).contains(value)) {
            throw new BusinessException(400, "治理动作无效");
        }
        return value;
    }

    private static String permissionAction(String action) {
        return switch (action) {
            case "APPROVE" -> "approve";
            case "REJECT" -> "reject";
            case "TAKE_DOWN" -> "takeDown";
            case "RESTORE" -> "restore";
            default -> throw new BusinessException(400, "治理动作无效");
        };
    }

    private static <S, T> PageResult<T> mapPage(
            AdminCommunityPageView<S> page,
            List<T> list
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
        return requiredId(value, label);
    }

    private static Long requiredId(String value, String label) {
        try {
            long parsed = Long.parseLong(value == null ? "" : value.trim());
            if (parsed <= 0) {
                throw new NumberFormatException();
            }
            return parsed;
        } catch (NumberFormatException exception) {
            throw new BusinessException(400, label + " ID 无效");
        }
    }
}

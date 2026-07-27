package top.pxczxn.community.admin.team;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.stp.StpUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import top.pxczxn.community.team.application.TeamApplicationReviewService;
import top.pxczxn.community.team.application.TeamApplicationView;
import top.pxczxn.platform.common.result.Result;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Admin controller for team application review.
 * Requires 'community:team:review' permission.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/admin-api/community/team-applications")
public class AdminTeamApplicationController {

    private final TeamApplicationReviewService reviewService;

    /**
     * List all pending team applications.
     */
    @GetMapping
    @SaCheckPermission("community:team:review")
    public Result<List<AdminTeamApplicationResponse>> listPending() {
        List<TeamApplicationView> views = reviewService.listPendingApplications();
        return Result.ok(views.stream()
                .map(AdminTeamApplicationResponse::from)
                .collect(Collectors.toList()));
    }

    /**
     * Get application detail for review.
     */
    @GetMapping("/{applicationId}")
    @SaCheckPermission("community:team:review")
    public Result<AdminTeamApplicationResponse> getDetail(
            @PathVariable Long applicationId
    ) {
        TeamApplicationView view = reviewService.getApplicationForReview(applicationId);
        return Result.ok(AdminTeamApplicationResponse.from(view));
    }

    /**
     * Approve team application.
     */
    @PostMapping("/{applicationId}/approve")
    @SaCheckPermission("community:team:review")
    public Result<AdminTeamApplicationApprovalResponse> approve(
            @PathVariable Long applicationId,
            @RequestBody AdminTeamApplicationReviewRequest request
    ) {
        Long reviewerUserId = StpUtil.getLoginIdAsLong();
        String requestId = UUID.randomUUID().toString();

        Long teamId = reviewService.approveApplication(
                applicationId,
                reviewerUserId,
                request.reviewComment(),
                requestId
        );

        return Result.ok(new AdminTeamApplicationApprovalResponse(teamId));
    }

    /**
     * Reject team application.
     */
    @PostMapping("/{applicationId}/reject")
    @SaCheckPermission("community:team:review")
    public Result<Void> reject(
            @PathVariable Long applicationId,
            @RequestBody AdminTeamApplicationReviewRequest request
    ) {
        Long reviewerUserId = StpUtil.getLoginIdAsLong();
        String requestId = UUID.randomUUID().toString();

        reviewService.rejectApplication(
                applicationId,
                reviewerUserId,
                request.reviewComment(),
                requestId
        );

        return Result.ok();
    }
}

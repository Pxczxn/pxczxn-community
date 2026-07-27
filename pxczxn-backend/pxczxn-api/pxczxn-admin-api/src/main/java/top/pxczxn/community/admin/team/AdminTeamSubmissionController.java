package top.pxczxn.community.admin.team;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.stp.StpUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import top.pxczxn.community.team.submission.application.DecideTeamSubmissionCommand;
import top.pxczxn.community.team.submission.application.TeamSubmissionService;
import top.pxczxn.platform.common.result.Result;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin-api/community/team-submissions")
public class AdminTeamSubmissionController {
    private final TeamSubmissionService submissionService;
    @GetMapping
    @SaCheckPermission("community:team:review")
    public Result<List<AdminTeamSubmissionResponse>> queue() { return Result.ok(submissionService.platformQueue().stream().map(AdminTeamSubmissionResponse::from).toList()); }
    @PostMapping("/{submissionId}/approve")
    @SaCheckPermission("community:team:review")
    public Result<AdminTeamSubmissionResponse> approve(@PathVariable Long submissionId, @Valid @RequestBody AdminTeamSubmissionDecisionRequest request) { return decision(submissionId, request, "approve"); }
    @PostMapping("/{submissionId}/revision")
    @SaCheckPermission("community:team:review")
    public Result<AdminTeamSubmissionResponse> revision(@PathVariable Long submissionId, @Valid @RequestBody AdminTeamSubmissionDecisionRequest request) { return decision(submissionId, request, "revision"); }
    @PostMapping("/{submissionId}/reject")
    @SaCheckPermission("community:team:review")
    public Result<AdminTeamSubmissionResponse> reject(@PathVariable Long submissionId, @Valid @RequestBody AdminTeamSubmissionDecisionRequest request) { return decision(submissionId, request, "reject"); }
    private Result<AdminTeamSubmissionResponse> decision(Long id, AdminTeamSubmissionDecisionRequest request, String action) {
        var command = new DecideTeamSubmissionCommand(request.expectedLockVersion(), request.comment()); Long admin = StpUtil.getLoginIdAsLong();
        var view = switch (action) { case "approve" -> submissionService.platformApprove(admin, id, command); case "revision" -> submissionService.platformRequestRevision(admin, id, command); default -> submissionService.platformReject(admin, id, command); };
        return Result.ok(AdminTeamSubmissionResponse.from(view));
    }
}

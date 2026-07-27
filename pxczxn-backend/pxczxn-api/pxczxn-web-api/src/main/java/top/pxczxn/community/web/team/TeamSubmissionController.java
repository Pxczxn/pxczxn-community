package top.pxczxn.community.web.team;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import top.pxczxn.community.shared.auth.CommunityAuth;
import top.pxczxn.community.team.submission.application.CreateTeamSubmissionCommand;
import top.pxczxn.community.team.submission.application.DecideTeamSubmissionCommand;
import top.pxczxn.community.team.submission.application.TeamSubmissionService;
import top.pxczxn.platform.common.result.Result;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/team-submissions")
public class TeamSubmissionController {
    private final TeamSubmissionService submissionService;
    private final CommunityAuth communityAuth;

    @PostMapping
    public Result<TeamSubmissionResponse> submit(@Valid @RequestBody TeamSubmissionRequest request) {
        return Result.ok(TeamSubmissionResponse.from(submissionService.submit(communityAuth.getLoginUserId(),
                new CreateTeamSubmissionCommand(request.sourceArticleId(), request.targetTeamId(), request.supersedesSubmissionId(), request.idempotencyKey()))));
    }
    @GetMapping("/me")
    public Result<List<TeamSubmissionResponse>> mine() { return Result.ok(submissionService.mine(communityAuth.getLoginUserId()).stream().map(TeamSubmissionResponse::from).toList()); }
    @GetMapping("/teams/{teamId}")
    public Result<List<TeamSubmissionResponse>> teamQueue(@PathVariable Long teamId) { return Result.ok(submissionService.teamQueue(communityAuth.getLoginUserId(), teamId).stream().map(TeamSubmissionResponse::from).toList()); }
    @PostMapping("/{submissionId}/team/approve")
    public Result<TeamSubmissionResponse> teamApprove(@PathVariable Long submissionId, @Valid @RequestBody TeamSubmissionDecisionRequest request) { return teamDecision(submissionId, request, "approve"); }
    @PostMapping("/{submissionId}/team/revision")
    public Result<TeamSubmissionResponse> teamRevision(@PathVariable Long submissionId, @Valid @RequestBody TeamSubmissionDecisionRequest request) { return teamDecision(submissionId, request, "revision"); }
    @PostMapping("/{submissionId}/team/reject")
    public Result<TeamSubmissionResponse> teamReject(@PathVariable Long submissionId, @Valid @RequestBody TeamSubmissionDecisionRequest request) { return teamDecision(submissionId, request, "reject"); }
    private Result<TeamSubmissionResponse> teamDecision(Long id, TeamSubmissionDecisionRequest request, String action) {
        var command = new DecideTeamSubmissionCommand(request.expectedLockVersion(), request.comment()); Long actor = communityAuth.getLoginUserId();
        var view = switch (action) { case "approve" -> submissionService.teamApprove(actor, id, command); case "revision" -> submissionService.teamRequestRevision(actor, id, command); default -> submissionService.teamReject(actor, id, command); };
        return Result.ok(TeamSubmissionResponse.from(view));
    }
}

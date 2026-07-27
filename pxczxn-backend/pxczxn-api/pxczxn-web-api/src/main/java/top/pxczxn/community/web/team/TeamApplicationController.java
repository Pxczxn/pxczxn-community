package top.pxczxn.community.web.team;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import top.pxczxn.community.shared.auth.CommunityAuth;
import top.pxczxn.community.team.application.SubmitTeamApplicationCommand;
import top.pxczxn.community.team.application.TeamApplicationService;
import top.pxczxn.community.team.application.TeamApplicationView;
import top.pxczxn.platform.common.result.Result;

/**
 * Team application controller for user-side team creation requests.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/team-applications")
public class TeamApplicationController {

    private final TeamApplicationService teamApplicationService;
    private final CommunityAuth communityAuth;

    /**
     * Submit a new team application.
     */
    @PostMapping
    public Result<TeamApplicationResponse> submitApplication(
            @RequestBody SubmitTeamApplicationRequest request
    ) {
        Long currentUserId = communityAuth.getLoginUserId();

        SubmitTeamApplicationCommand command = new SubmitTeamApplicationCommand();
        command.setApplicantUserId(currentUserId);
        command.setTeamName(request.teamName());
        command.setTeamSlug(request.teamSlug());
        command.setDescription(request.description());
        command.setIdempotencyKey(request.idempotencyKey());

        Long applicationId = teamApplicationService.submitApplication(command);
        TeamApplicationView view = teamApplicationService.getApplication(applicationId, currentUserId);

        return Result.ok(TeamApplicationResponse.from(view));
    }

    /**
     * Get current user's application.
     */
    @GetMapping("/me")
    public Result<TeamApplicationResponse> getMyApplication() {
        Long currentUserId = communityAuth.getLoginUserId();
        TeamApplicationView view = teamApplicationService.getMyApplication(currentUserId);

        if (view == null) {
            return Result.ok(null);
        }

        return Result.ok(TeamApplicationResponse.from(view));
    }

    /**
     * Get application by ID (applicant only).
     */
    @GetMapping("/{applicationId}")
    public Result<TeamApplicationResponse> getApplication(
            @PathVariable Long applicationId
    ) {
        Long currentUserId = communityAuth.getLoginUserId();
        TeamApplicationView view = teamApplicationService.getApplication(applicationId, currentUserId);

        return Result.ok(TeamApplicationResponse.from(view));
    }

    /**
     * Cancel application (applicant only, PENDING status only).
     */
    @DeleteMapping("/{applicationId}")
    public Result<Void> cancelApplication(
            @PathVariable Long applicationId
    ) {
        Long currentUserId = communityAuth.getLoginUserId();
        teamApplicationService.cancelApplication(applicationId, currentUserId);

        return Result.ok();
    }
}

package top.pxczxn.community.web.team;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import top.pxczxn.community.shared.auth.CommunityAuth;
import top.pxczxn.community.team.application.MyTeamView;
import top.pxczxn.community.team.application.TeamActivityView;
import top.pxczxn.community.team.application.TeamArticleBriefView;
import top.pxczxn.community.team.application.TeamDashboardView;
import top.pxczxn.community.team.application.TeamPortalService;
import top.pxczxn.community.team.application.TeamPortalView;
import top.pxczxn.community.team.application.TeamSummaryView;
import top.pxczxn.community.team.application.TeamWorkspaceView;
import top.pxczxn.community.team.application.UpdateTeamSettingsCommand;
import top.pxczxn.platform.common.result.Result;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/teams")
public class TeamPortalController {
    private final TeamPortalService portalService;
    private final CommunityAuth communityAuth;

    @GetMapping
    public Result<List<TeamSummaryView>> list() { return Result.ok(portalService.listPublicTeams()); }

    @GetMapping("/slug/{teamSlug}")
    public Result<TeamPortalView> detail(@PathVariable String teamSlug) { return Result.ok(portalService.publicTeam(teamSlug)); }

    /** All active memberships of the current user; drives the smart default tab and the team switcher. */
    @GetMapping("/me")
    public Result<List<MyTeamView>> myTeams() {
        return Result.ok(portalService.myTeams(communityAuth.getLoginUserId()));
    }

    @GetMapping("/{teamId}/workspace")
    public Result<TeamWorkspaceView> workspace(@PathVariable Long teamId) {
        return Result.ok(portalService.workspace(communityAuth.getLoginUserId(), teamId));
    }

    /** Single round trip powering the workspace overview page. Members only. */
    @GetMapping("/{teamId}/dashboard")
    public Result<TeamDashboardView> dashboard(@PathVariable Long teamId) {
        return Result.ok(portalService.dashboard(communityAuth.getLoginUserId(), teamId));
    }

    /** Collaboration activity feed of a team. Members only. */
    @GetMapping("/{teamId}/activities")
    public Result<List<TeamActivityView>> activities(@PathVariable Long teamId,
                                                     @RequestParam(defaultValue = "20") int limit) {
        return Result.ok(portalService.activities(communityAuth.getLoginUserId(), teamId, limit));
    }

    /** All team articles regardless of status, optionally filtered by publish_status. Members only. */
    @GetMapping("/{teamId}/articles")
    public Result<List<TeamArticleBriefView>> articles(@PathVariable Long teamId,
                                                       @RequestParam(required = false) String publishStatus) {
        return Result.ok(portalService.teamArticles(communityAuth.getLoginUserId(), teamId, publishStatus));
    }

    /** Update team profile (name / summary / avatar / background). Requires MANAGE_TEAM. */
    @PatchMapping("/{teamId}")
    public Result<TeamSummaryView> updateSettings(@PathVariable Long teamId,
                                                  @RequestBody UpdateTeamSettingsCommand command) {
        return Result.ok(portalService.updateSettings(communityAuth.getLoginUserId(), teamId, command));
    }
}

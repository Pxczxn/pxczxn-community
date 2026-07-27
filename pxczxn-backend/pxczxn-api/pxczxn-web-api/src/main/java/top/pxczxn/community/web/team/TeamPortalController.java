package top.pxczxn.community.web.team;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import top.pxczxn.community.shared.auth.CommunityAuth;
import top.pxczxn.community.team.application.TeamPortalService;
import top.pxczxn.community.team.application.TeamPortalView;
import top.pxczxn.community.team.application.TeamSummaryView;
import top.pxczxn.community.team.application.TeamWorkspaceView;
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

    @GetMapping("/{teamId}/workspace")
    public Result<TeamWorkspaceView> workspace(@PathVariable Long teamId) {
        return Result.ok(portalService.workspace(communityAuth.getLoginUserId(), teamId));
    }
}

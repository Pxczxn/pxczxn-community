package top.pxczxn.community.team.application;

import java.util.List;

public interface TeamPortalService {

    List<TeamSummaryView> listPublicTeams();

    TeamPortalView publicTeam(String teamSlug);

    TeamWorkspaceView workspace(Long viewerUserId, Long teamId);
}

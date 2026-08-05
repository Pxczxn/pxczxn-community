package top.pxczxn.community.team.application;

import java.util.List;

public interface TeamPortalService {

    List<TeamSummaryView> listPublicTeams();

    TeamPortalView publicTeam(String teamSlug);

    TeamWorkspaceView workspace(Long viewerUserId, Long teamId);

    /**
     * All active memberships of one user, enriched with per-team counters.
     * Drives the smart default tab on {@code /teams}, the "我的团队" cards and the workspace team switcher.
     */
    List<MyTeamView> myTeams(Long viewerUserId);

    /** Single round trip powering the workspace overview page. Members only. */
    TeamDashboardView dashboard(Long viewerUserId, Long teamId);

    /** Collaboration activity feed of a team. Members only. */
    List<TeamActivityView> activities(Long viewerUserId, Long teamId, int limit);

    /**
     * All articles of the team blog regardless of publish status, optionally filtered by one
     * {@code publish_status}. Members only; backs the workspace content page.
     */
    List<TeamArticleBriefView> teamArticles(Long viewerUserId, Long teamId, String publishStatus);

    /**
     * Update team profile (name / summary / avatar / background). Requires {@code MANAGE_TEAM}.
     * Team slug is immutable in V1.
     */
    TeamSummaryView updateSettings(Long viewerUserId, Long teamId, UpdateTeamSettingsCommand command);
}

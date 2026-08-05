package top.pxczxn.community.team.application;

import java.util.List;

/**
 * Everything the workspace overview page needs in a single round trip.
 *
 * <p>{@code capabilities} carries the navigation sections (OVERVIEW / ARTICLES / MEMBERS / ...) while
 * {@code permissions} carries the authoritative {@code team_permission.permission_code} list.
 * Exposing both keeps the legacy menu contract intact and lets the UI gate actions on real permissions.</p>
 */
public record TeamDashboardView(
        TeamSummaryView team,
        String viewerRole,
        List<String> capabilities,
        List<String> permissions,
        TeamStatsView stats,
        List<TeamArticleBriefView> recentArticles,
        TeamTodoView todos,
        List<TeamActivityView> recentActivities) {
}

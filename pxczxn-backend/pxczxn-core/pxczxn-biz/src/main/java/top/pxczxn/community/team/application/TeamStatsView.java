package top.pxczxn.community.team.application;

/**
 * Workspace overview stat cards. Every field is a non-null {@code Integer} so the UI can render
 * a plain {@code 0} instead of {@code undefined} / {@code NaN} when a team has no data yet.
 */
public record TeamStatsView(
        Integer publishedArticleCount,
        Integer draftArticleCount,
        Integer reviewingArticleCount,
        Integer seriesCount,
        Integer memberCount,
        Integer followerCount,
        Integer totalViewCount,
        Integer totalInteractionCount) {
}

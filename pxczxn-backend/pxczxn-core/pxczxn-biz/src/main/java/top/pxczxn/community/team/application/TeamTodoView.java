package top.pxczxn.community.team.application;

/**
 * Workspace todo counters. The backend deliberately returns codes and numbers only -
 * Chinese labels and target routes belong to the frontend so copy changes never require a redeploy.
 *
 * <p>Counters the viewer has no permission to act on are returned as {@code 0} rather than omitted,
 * keeping the payload shape stable across roles.</p>
 */
public record TeamTodoView(
        Integer pendingSubmissionCount,
        Integer revisionRequiredCount,
        Integer pendingInvitationCount,
        Integer pendingSeriesReviewCount,
        Integer contentRiskCount) {
}

package top.pxczxn.community.notification.application;

/**
 * Event fired after team application review decision is committed.
 * Used to notify the applicant of approval or rejection.
 */
public record TeamApplicationReviewDecisionEvent(
        Long applicationId,
        Long applicantUserId,
        String teamName,
        String decision,  // APPROVED or REJECTED
        String reviewComment,
        Long teamId  // Only present when approved
) {
}

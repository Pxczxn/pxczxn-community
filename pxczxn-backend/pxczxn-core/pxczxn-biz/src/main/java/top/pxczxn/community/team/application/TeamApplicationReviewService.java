package top.pxczxn.community.team.application;

import java.util.List;

/**
 * Team application review service for platform operators.
 * Handles approval/rejection of team creation requests.
 * Requires 'community:team:review' permission.
 */
public interface TeamApplicationReviewService {

    /**
     * Query all pending applications (platform review only).
     *
     * @return List of pending applications
     */
    List<TeamApplicationView> listPendingApplications();

    /**
     * Query application by ID (platform review only).
     *
     * @param applicationId Application ID
     * @return Application view
     * @throws IllegalArgumentException if application not found
     */
    TeamApplicationView getApplicationForReview(Long applicationId);

    /**
     * Approve team application and atomically create team blog, team, owner member, default settings, and audit events.
     * Uses conditional update to ensure only one reviewer can approve.
     *
     * @param applicationId Application ID
     * @param reviewerUserId Reviewer user ID
     * @param reviewComment Optional review comment
     * @param requestId Request trace ID for audit
     * @return Created team ID
     * @throws IllegalStateException if application is not in PENDING status or concurrent approval occurs
     */
    Long approveApplication(Long applicationId, Long reviewerUserId, String reviewComment, String requestId);

    /**
     * Reject team application.
     * Uses conditional update to ensure only one reviewer can reject.
     *
     * @param applicationId Application ID
     * @param reviewerUserId Reviewer user ID
     * @param reviewComment Review comment (required for rejection)
     * @param requestId Request trace ID for audit
     * @throws IllegalStateException if application is not in PENDING status or concurrent rejection occurs
     * @throws IllegalArgumentException if review comment is null or empty
     */
    void rejectApplication(Long applicationId, Long reviewerUserId, String reviewComment, String requestId);
}

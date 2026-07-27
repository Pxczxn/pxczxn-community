package top.pxczxn.community.team.application;

/**
 * Team application service for user-side team creation requests.
 * Handles submission, query, and cancellation by applicants only.
 */
public interface TeamApplicationService {

    /**
     * Submit a new team application.
     * Validates user status, slug uniqueness, duplicate pending applications, and idempotency key.
     *
     * @param command Application submission command
     * @return Application ID
     * @throws IllegalStateException if user is banned, has pending application, slug is taken, or idempotency key exists
     */
    Long submitApplication(SubmitTeamApplicationCommand command);

    /**
     * Query application by ID (applicant only).
     *
     * @param applicationId Application ID
     * @param applicantUserId Applicant user ID
     * @return Application view
     * @throws IllegalArgumentException if application not found or user is not the applicant
     */
    TeamApplicationView getApplication(Long applicationId, Long applicantUserId);

    /**
     * Query applicant's own application (returns null if no pending application).
     *
     * @param applicantUserId Applicant user ID
     * @return Application view or null
     */
    TeamApplicationView getMyApplication(Long applicantUserId);

    /**
     * Cancel application by applicant (only PENDING status can be cancelled).
     *
     * @param applicationId Application ID
     * @param applicantUserId Applicant user ID
     * @throws IllegalStateException if application is not in PENDING status
     * @throws IllegalArgumentException if application not found or user is not the applicant
     */
    void cancelApplication(Long applicationId, Long applicantUserId);
}

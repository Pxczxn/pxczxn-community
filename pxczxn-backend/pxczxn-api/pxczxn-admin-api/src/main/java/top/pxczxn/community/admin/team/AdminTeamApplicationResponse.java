package top.pxczxn.community.admin.team;

import top.pxczxn.community.team.application.TeamApplicationView;

import java.time.LocalDateTime;

/**
 * Admin response for team application.
 */
public record AdminTeamApplicationResponse(
        Long id,
        Long applicantUserId,
        String teamName,
        String teamSlug,
        String description,
        String status,
        Long reviewerUserId,
        String reviewComment,
        LocalDateTime reviewedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static AdminTeamApplicationResponse from(TeamApplicationView view) {
        return new AdminTeamApplicationResponse(
                view.getId(),
                view.getApplicantUserId(),
                view.getTeamName(),
                view.getTeamSlug(),
                view.getDescription(),
                view.getStatus(),
                view.getReviewerUserId(),
                view.getReviewComment(),
                view.getReviewedAt(),
                view.getCreatedAt(),
                view.getUpdatedAt()
        );
    }
}

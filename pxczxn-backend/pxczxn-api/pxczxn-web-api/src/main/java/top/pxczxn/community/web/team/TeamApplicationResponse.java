package top.pxczxn.community.web.team;

import top.pxczxn.community.team.application.TeamApplicationView;

import java.time.LocalDateTime;

/**
 * Response for team application.
 */
public record TeamApplicationResponse(
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
    public static TeamApplicationResponse from(TeamApplicationView view) {
        return new TeamApplicationResponse(
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

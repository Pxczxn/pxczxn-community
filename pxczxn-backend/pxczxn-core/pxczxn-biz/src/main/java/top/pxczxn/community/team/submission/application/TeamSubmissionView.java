package top.pxczxn.community.team.submission.application;

import top.pxczxn.community.team.submission.model.TeamSubmission;

import java.time.LocalDateTime;

public record TeamSubmissionView(
        Long id, Long sourceArticleId, String sourceArticleTitle, Long fixedSourceVersionId,
        Long targetTeamId, Long submittedByUserId, Long supersedesSubmissionId, String status,
        Long teamReviewerUserId, String teamReviewComment, LocalDateTime teamReviewedAt,
        Long platformReviewerAdminId, String platformReviewComment, LocalDateTime platformReviewedAt,
        Long publishedTeamArticleId, Integer lockVersion, LocalDateTime createdAt, LocalDateTime updatedAt
) {
    public static TeamSubmissionView from(TeamSubmission submission, String sourceArticleTitle) {
        return new TeamSubmissionView(submission.getId(), submission.getSourceArticleId(), sourceArticleTitle,
                submission.getFixedSourceVersionId(), submission.getTargetTeamId(), submission.getSubmittedByUserId(),
                submission.getSupersedesSubmissionId(), submission.getStatus(), submission.getTeamReviewerUserId(),
                submission.getTeamReviewComment(), submission.getTeamReviewedAt(), submission.getPlatformReviewerAdminId(),
                submission.getPlatformReviewComment(), submission.getPlatformReviewedAt(), submission.getPublishedTeamArticleId(),
                submission.getLockVersion(), submission.getCreatedAt(), submission.getUpdatedAt());
    }
}

package top.pxczxn.community.admin.team;

import top.pxczxn.community.team.submission.application.TeamSubmissionView;

import java.time.LocalDateTime;

public record AdminTeamSubmissionResponse(
        String id, String sourceArticleId, String sourceArticleTitle, String fixedSourceVersionId,
        String targetTeamId, String submittedByUserId, String supersedesSubmissionId, String status,
        String teamReviewerUserId, String teamReviewComment, LocalDateTime teamReviewedAt,
        String platformReviewerAdminId, String platformReviewComment, LocalDateTime platformReviewedAt,
        String publishedTeamArticleId, Integer lockVersion, LocalDateTime createdAt, LocalDateTime updatedAt
) {
    public static AdminTeamSubmissionResponse from(TeamSubmissionView v) {
        return new AdminTeamSubmissionResponse(id(v.id()), id(v.sourceArticleId()), v.sourceArticleTitle(), id(v.fixedSourceVersionId()),
                id(v.targetTeamId()), id(v.submittedByUserId()), id(v.supersedesSubmissionId()), v.status(),
                id(v.teamReviewerUserId()), v.teamReviewComment(), v.teamReviewedAt(), id(v.platformReviewerAdminId()),
                v.platformReviewComment(), v.platformReviewedAt(), id(v.publishedTeamArticleId()), v.lockVersion(), v.createdAt(), v.updatedAt());
    }
    private static String id(Long value) { return value == null ? null : value.toString(); }
}

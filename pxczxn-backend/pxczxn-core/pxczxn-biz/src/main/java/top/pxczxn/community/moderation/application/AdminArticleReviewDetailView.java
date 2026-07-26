package top.pxczxn.community.moderation.application;

import java.time.LocalDateTime;

public record AdminArticleReviewDetailView(
        Long taskId,
        Long articleId,
        Long fixedVersionId,
        String articleTitle,
        String articleSummary,
        String visibility,
        String publishStatus,
        String reviewStatus,
        Long authorUserId,
        String authorUsername,
        String authorDisplayName,
        Long blogId,
        String blogName,
        String blogSlug,
        String taskStatus,
        String riskLevel,
        String reviewStage,
        String reviewType,
        Long submittedByUserId,
        Long assigneeAdminId,
        String resultCode,
        String resultReason,
        int taskLockVersion,
        int articleLockVersion,
        LocalDateTime submittedAt,
        LocalDateTime claimedAt,
        LocalDateTime completedAt,
        AdminArticleReviewContentView content
) {
}

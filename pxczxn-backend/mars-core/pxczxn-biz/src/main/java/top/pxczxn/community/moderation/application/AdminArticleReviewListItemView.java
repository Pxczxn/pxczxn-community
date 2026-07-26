package top.pxczxn.community.moderation.application;

import java.time.LocalDateTime;

public record AdminArticleReviewListItemView(
        Long taskId,
        Long articleId,
        Long fixedVersionId,
        String articleTitle,
        Long authorUserId,
        String authorDisplayName,
        Long blogId,
        String blogName,
        String status,
        String riskLevel,
        String reviewStage,
        String reviewType,
        Long assigneeAdminId,
        String resultCode,
        int taskLockVersion,
        LocalDateTime submittedAt,
        LocalDateTime claimedAt,
        LocalDateTime completedAt
) {
}

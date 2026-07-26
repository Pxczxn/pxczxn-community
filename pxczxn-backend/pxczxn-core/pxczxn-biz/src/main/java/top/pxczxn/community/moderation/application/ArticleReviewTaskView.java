package top.pxczxn.community.moderation.application;

import java.time.LocalDateTime;

public record ArticleReviewTaskView(
        Long taskId,
        Long fixedVersionId,
        String reviewStage,
        String reviewType,
        String status,
        String riskLevel,
        String resultCode,
        String resultReason,
        int lockVersion,
        LocalDateTime submittedAt,
        LocalDateTime claimedAt,
        LocalDateTime completedAt
) {
}

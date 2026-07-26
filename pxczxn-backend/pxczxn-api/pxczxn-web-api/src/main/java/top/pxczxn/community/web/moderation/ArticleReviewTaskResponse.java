package top.pxczxn.community.web.moderation;

import top.pxczxn.community.moderation.application.ArticleReviewTaskView;

import java.time.LocalDateTime;

public record ArticleReviewTaskResponse(
        String taskId,
        String fixedVersionId,
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

    static ArticleReviewTaskResponse from(ArticleReviewTaskView view) {
        if (view == null) {
            return null;
        }
        return new ArticleReviewTaskResponse(
                id(view.taskId()),
                id(view.fixedVersionId()),
                view.reviewStage(),
                view.reviewType(),
                view.status(),
                view.riskLevel(),
                view.resultCode(),
                view.resultReason(),
                view.lockVersion(),
                view.submittedAt(),
                view.claimedAt(),
                view.completedAt()
        );
    }

    private static String id(Long value) {
        return value == null ? null : value.toString();
    }
}

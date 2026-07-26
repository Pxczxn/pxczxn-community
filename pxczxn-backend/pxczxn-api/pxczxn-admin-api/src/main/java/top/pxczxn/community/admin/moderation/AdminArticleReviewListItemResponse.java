package top.pxczxn.community.admin.moderation;

import top.pxczxn.community.moderation.application.AdminArticleReviewListItemView;

import java.time.LocalDateTime;

public record AdminArticleReviewListItemResponse(
        String taskId,
        String articleId,
        String fixedVersionId,
        String articleTitle,
        String authorUserId,
        String authorDisplayName,
        String blogId,
        String blogName,
        String status,
        String riskLevel,
        String reviewStage,
        String reviewType,
        String assigneeAdminId,
        String resultCode,
        int taskLockVersion,
        LocalDateTime submittedAt,
        LocalDateTime claimedAt,
        LocalDateTime completedAt
) {

    static AdminArticleReviewListItemResponse from(
            AdminArticleReviewListItemView view
    ) {
        return new AdminArticleReviewListItemResponse(
                id(view.taskId()),
                id(view.articleId()),
                id(view.fixedVersionId()),
                view.articleTitle(),
                id(view.authorUserId()),
                view.authorDisplayName(),
                id(view.blogId()),
                view.blogName(),
                view.status(),
                view.riskLevel(),
                view.reviewStage(),
                view.reviewType(),
                id(view.assigneeAdminId()),
                view.resultCode(),
                view.taskLockVersion(),
                view.submittedAt(),
                view.claimedAt(),
                view.completedAt()
        );
    }

    private static String id(Long value) {
        return value == null ? null : value.toString();
    }
}

package top.pxczxn.community.admin.moderation;

import top.pxczxn.community.moderation.application.AdminArticleReviewDetailView;

import java.time.LocalDateTime;

public record AdminArticleReviewDetailResponse(
        String taskId,
        String articleId,
        String fixedVersionId,
        String articleTitle,
        String articleSummary,
        String visibility,
        String publishStatus,
        String reviewStatus,
        String authorUserId,
        String authorUsername,
        String authorDisplayName,
        String blogId,
        String blogName,
        String blogSlug,
        String taskStatus,
        String riskLevel,
        String reviewStage,
        String reviewType,
        String submittedByUserId,
        String assigneeAdminId,
        String resultCode,
        String resultReason,
        int taskLockVersion,
        int articleLockVersion,
        LocalDateTime submittedAt,
        LocalDateTime claimedAt,
        LocalDateTime completedAt,
        AdminArticleReviewContentResponse content
) {

    static AdminArticleReviewDetailResponse from(
            AdminArticleReviewDetailView view
    ) {
        return new AdminArticleReviewDetailResponse(
                id(view.taskId()),
                id(view.articleId()),
                id(view.fixedVersionId()),
                view.articleTitle(),
                view.articleSummary(),
                view.visibility(),
                view.publishStatus(),
                view.reviewStatus(),
                id(view.authorUserId()),
                view.authorUsername(),
                view.authorDisplayName(),
                id(view.blogId()),
                view.blogName(),
                view.blogSlug(),
                view.taskStatus(),
                view.riskLevel(),
                view.reviewStage(),
                view.reviewType(),
                id(view.submittedByUserId()),
                id(view.assigneeAdminId()),
                view.resultCode(),
                view.resultReason(),
                view.taskLockVersion(),
                view.articleLockVersion(),
                view.submittedAt(),
                view.claimedAt(),
                view.completedAt(),
                AdminArticleReviewContentResponse.from(view.content())
        );
    }

    private static String id(Long value) {
        return value == null ? null : value.toString();
    }
}

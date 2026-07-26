package top.pxczxn.community.notification.application;

public record ArticleReviewDecisionNotificationEvent(
        Long taskId,
        Long articleId,
        Long recipientUserId,
        String articleTitle,
        String decision,
        String reason
) {
}

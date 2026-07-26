package top.pxczxn.community.moderation.application;

public record ArticleReviewStatusView(
        Long articleId,
        String publishStatus,
        String reviewStatus,
        Long currentVersionId,
        Long reviewVersionId,
        int lockVersion,
        ArticleReviewTaskView latestTask
) {
}

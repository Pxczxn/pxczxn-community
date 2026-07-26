package top.pxczxn.community.web.moderation;

import top.pxczxn.community.moderation.application.ArticleReviewStatusView;

public record ArticleReviewStatusResponse(
        String articleId,
        String publishStatus,
        String reviewStatus,
        String currentVersionId,
        String reviewVersionId,
        int lockVersion,
        ArticleReviewTaskResponse latestTask
) {

    static ArticleReviewStatusResponse from(ArticleReviewStatusView view) {
        return new ArticleReviewStatusResponse(
                id(view.articleId()),
                view.publishStatus(),
                view.reviewStatus(),
                id(view.currentVersionId()),
                id(view.reviewVersionId()),
                view.lockVersion(),
                ArticleReviewTaskResponse.from(view.latestTask())
        );
    }

    private static String id(Long value) {
        return value == null ? null : value.toString();
    }
}

package top.pxczxn.community.web.article;

import top.pxczxn.community.article.application.ArticlePublishView;

import java.time.LocalDateTime;

public record ArticlePublishResponse(
        String articleId,
        String previousPublishedVersionId,
        String publishedVersionId,
        String publishStatus,
        String reviewStatus,
        String canonicalPath,
        LocalDateTime scheduledPublishAt,
        LocalDateTime publishedAt,
        int lockVersion,
        boolean idempotentReplay
) {

    static ArticlePublishResponse from(ArticlePublishView view) {
        return new ArticlePublishResponse(
                id(view.articleId()),
                id(view.previousPublishedVersionId()),
                id(view.publishedVersionId()),
                view.publishStatus(),
                view.reviewStatus(),
                view.canonicalPath(),
                view.scheduledPublishAt(),
                view.publishedAt(),
                view.lockVersion(),
                view.idempotentReplay()
        );
    }

    private static String id(Long value) {
        return value == null ? null : value.toString();
    }
}

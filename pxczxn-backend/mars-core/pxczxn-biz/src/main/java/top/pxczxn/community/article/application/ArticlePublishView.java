package top.pxczxn.community.article.application;

import java.time.LocalDateTime;

public record ArticlePublishView(
        Long articleId,
        Long previousPublishedVersionId,
        Long publishedVersionId,
        String publishStatus,
        String reviewStatus,
        String canonicalPath,
        LocalDateTime scheduledPublishAt,
        LocalDateTime publishedAt,
        int lockVersion,
        boolean idempotentReplay
) {

    public ArticlePublishView(
            Long articleId,
            Long previousPublishedVersionId,
            Long publishedVersionId,
            String publishStatus,
            String reviewStatus,
            String canonicalPath,
            LocalDateTime publishedAt,
            int lockVersion,
            boolean idempotentReplay
    ) {
        this(
                articleId,
                previousPublishedVersionId,
                publishedVersionId,
                publishStatus,
                reviewStatus,
                canonicalPath,
                null,
                publishedAt,
                lockVersion,
                idempotentReplay
        );
    }
}

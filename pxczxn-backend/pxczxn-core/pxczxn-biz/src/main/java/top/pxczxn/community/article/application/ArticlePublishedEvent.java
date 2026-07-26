package top.pxczxn.community.article.application;

import java.time.LocalDateTime;

public record ArticlePublishedEvent(
        Long articleId,
        Long blogId,
        Long authorUserId,
        Long previousPublishedVersionId,
        Long publishedVersionId,
        String canonicalPath,
        LocalDateTime publishedAt,
        String trigger
) {
}

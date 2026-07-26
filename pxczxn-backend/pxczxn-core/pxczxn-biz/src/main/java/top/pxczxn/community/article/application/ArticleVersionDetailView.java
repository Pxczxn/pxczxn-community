package top.pxczxn.community.article.application;

import java.time.LocalDateTime;
import java.util.List;

public record ArticleVersionDetailView(
        Long versionId,
        Long articleId,
        int versionNo,
        String contentMode,
        String richTextJson,
        String markdownContent,
        String renderedHtml,
        String plainText,
        String tocJson,
        String contentHash,
        int wordCount,
        int readingTimeMinutes,
        Long createdByUserId,
        String creationType,
        List<Long> contentFileIds,
        boolean currentVersion,
        boolean publishedVersion,
        LocalDateTime createdAt
) {
}

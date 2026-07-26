package top.pxczxn.community.article.application;

import java.time.LocalDateTime;

public record ArticleVersionSummaryView(
        Long versionId,
        int versionNo,
        String contentMode,
        String contentHash,
        int wordCount,
        int readingTimeMinutes,
        Long createdByUserId,
        String creationType,
        boolean currentVersion,
        boolean publishedVersion,
        LocalDateTime createdAt
) {
}

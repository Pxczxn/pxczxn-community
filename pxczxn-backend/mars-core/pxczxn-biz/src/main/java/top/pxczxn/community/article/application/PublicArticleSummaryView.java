package top.pxczxn.community.article.application;

import java.time.LocalDateTime;
import java.util.List;

public record PublicArticleSummaryView(
        Long articleId,
        String title,
        String slug,
        String summary,
        Long coverFileId,
        String contentMode,
        PublicArticleAuthorView author,
        PublicArticleCategoryView category,
        List<PublicArticleTagView> tags,
        LocalDateTime publishedAt,
        LocalDateTime updatedAt,
        int wordCount,
        int readingTimeMinutes,
        long viewCount,
        long likeCount,
        long favoriteCount,
        long commentCount,
        String canonicalPath
) {
}

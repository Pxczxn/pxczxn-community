package top.pxczxn.community.article.application;

import java.time.LocalDateTime;
import java.util.List;

public record PublicArticleDetailView(
        Long articleId,
        String title,
        String slug,
        String summary,
        Long coverFileId,
        String contentMode,
        String visibility,
        String renderedHtml,
        String tocJson,
        int wordCount,
        int readingTimeMinutes,
        LocalDateTime publishedAt,
        LocalDateTime updatedAt,
        long viewCount,
        long likeCount,
        long favoriteCount,
        long commentCount,
        String canonicalPath,
        PublicArticleAuthorView author,
        PublicArticleBlogView blog,
        PublicArticleCategoryView category,
        List<PublicArticleTagView> tags,
        PublicArticleSeoView seo
) {
}

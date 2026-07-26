package top.pxczxn.community.article.application;

import java.time.LocalDateTime;
import java.util.List;

public record ArticleEditorView(
        Long articleId,
        Long blogId,
        Long authorUserId,
        Long categoryId,
        String title,
        String slug,
        String summary,
        Long coverFileId,
        String contentMode,
        String visibility,
        String publishMethod,
        String publishStatus,
        String reviewStatus,
        Long currentVersionId,
        Long publishedVersionId,
        Long reviewVersionId,
        int lockVersion,
        List<Long> tagIds,
        List<Long> contentFileIds,
        String richTextJson,
        String markdownContent,
        String renderedHtml,
        String plainText,
        String tocJson,
        String contentHash,
        int wordCount,
        int readingTimeMinutes,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime versionCreatedAt
) {
}

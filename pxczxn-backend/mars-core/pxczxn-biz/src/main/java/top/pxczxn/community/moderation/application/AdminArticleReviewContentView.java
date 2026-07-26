package top.pxczxn.community.moderation.application;

public record AdminArticleReviewContentView(
        Long versionId,
        int versionNo,
        String contentMode,
        String richTextJson,
        String markdownContent,
        String renderedHtml,
        String plainText,
        String tocJson,
        String contentHash,
        int wordCount,
        int readingTimeMinutes
) {
}

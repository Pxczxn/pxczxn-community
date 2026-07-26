package top.pxczxn.community.article.application;

record RenderedArticleContent(
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

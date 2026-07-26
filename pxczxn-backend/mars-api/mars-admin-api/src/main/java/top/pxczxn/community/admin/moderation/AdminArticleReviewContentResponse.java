package top.pxczxn.community.admin.moderation;

import top.pxczxn.community.moderation.application.AdminArticleReviewContentView;

public record AdminArticleReviewContentResponse(
        String versionId,
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

    static AdminArticleReviewContentResponse from(
            AdminArticleReviewContentView view
    ) {
        return new AdminArticleReviewContentResponse(
                view.versionId().toString(),
                view.versionNo(),
                view.contentMode(),
                view.richTextJson(),
                view.markdownContent(),
                view.renderedHtml(),
                view.plainText(),
                view.tocJson(),
                view.contentHash(),
                view.wordCount(),
                view.readingTimeMinutes()
        );
    }
}

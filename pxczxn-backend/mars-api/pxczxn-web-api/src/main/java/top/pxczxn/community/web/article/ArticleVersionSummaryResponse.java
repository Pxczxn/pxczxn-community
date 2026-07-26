package top.pxczxn.community.web.article;

import top.pxczxn.community.article.application.ArticleVersionSummaryView;

import java.time.LocalDateTime;

public record ArticleVersionSummaryResponse(
        String versionId,
        int versionNo,
        String contentMode,
        String contentHash,
        int wordCount,
        int readingTimeMinutes,
        String createdByUserId,
        String creationType,
        boolean currentVersion,
        boolean publishedVersion,
        LocalDateTime createdAt
) {

    static ArticleVersionSummaryResponse from(ArticleVersionSummaryView view) {
        return new ArticleVersionSummaryResponse(
                view.versionId().toString(),
                view.versionNo(),
                view.contentMode(),
                view.contentHash(),
                view.wordCount(),
                view.readingTimeMinutes(),
                view.createdByUserId().toString(),
                view.creationType(),
                view.currentVersion(),
                view.publishedVersion(),
                view.createdAt()
        );
    }
}

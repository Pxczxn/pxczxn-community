package top.pxczxn.community.web.article;

import top.pxczxn.community.article.application.ArticleVersionDetailView;

import java.time.LocalDateTime;
import java.util.List;

public record ArticleVersionDetailResponse(
        String versionId,
        String articleId,
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
        String createdByUserId,
        String creationType,
        List<String> contentFileIds,
        boolean currentVersion,
        boolean publishedVersion,
        LocalDateTime createdAt
) {

    static ArticleVersionDetailResponse from(ArticleVersionDetailView view) {
        return new ArticleVersionDetailResponse(
                view.versionId().toString(),
                view.articleId().toString(),
                view.versionNo(),
                view.contentMode(),
                view.richTextJson(),
                view.markdownContent(),
                view.renderedHtml(),
                view.plainText(),
                view.tocJson(),
                view.contentHash(),
                view.wordCount(),
                view.readingTimeMinutes(),
                view.createdByUserId().toString(),
                view.creationType(),
                view.contentFileIds().stream().map(String::valueOf).toList(),
                view.currentVersion(),
                view.publishedVersion(),
                view.createdAt()
        );
    }
}

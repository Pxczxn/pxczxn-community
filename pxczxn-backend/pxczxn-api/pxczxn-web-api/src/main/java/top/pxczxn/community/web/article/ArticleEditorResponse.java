package top.pxczxn.community.web.article;

import top.pxczxn.community.article.application.ArticleEditorView;

import java.time.LocalDateTime;
import java.util.List;

public record ArticleEditorResponse(
        String articleId,
        String blogId,
        String authorUserId,
        String categoryId,
        String title,
        String slug,
        String summary,
        String coverFileId,
        String contentMode,
        String visibility,
        String publishMethod,
        String publishStatus,
        String reviewStatus,
        String currentVersionId,
        String publishedVersionId,
        String reviewVersionId,
        int lockVersion,
        List<String> tagIds,
        List<String> contentFileIds,
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

    static ArticleEditorResponse from(ArticleEditorView view) {
        return new ArticleEditorResponse(
                id(view.articleId()),
                id(view.blogId()),
                id(view.authorUserId()),
                id(view.categoryId()),
                view.title(),
                view.slug(),
                view.summary(),
                id(view.coverFileId()),
                view.contentMode(),
                view.visibility(),
                view.publishMethod(),
                view.publishStatus(),
                view.reviewStatus(),
                id(view.currentVersionId()),
                id(view.publishedVersionId()),
                id(view.reviewVersionId()),
                view.lockVersion(),
                ids(view.tagIds()),
                ids(view.contentFileIds()),
                view.richTextJson(),
                view.markdownContent(),
                view.renderedHtml(),
                view.plainText(),
                view.tocJson(),
                view.contentHash(),
                view.wordCount(),
                view.readingTimeMinutes(),
                view.createdAt(),
                view.updatedAt(),
                view.versionCreatedAt()
        );
    }

    private static String id(Long value) {
        return value == null ? null : value.toString();
    }

    private static List<String> ids(List<Long> values) {
        return values == null
                ? List.of()
                : values.stream().map(String::valueOf).toList();
    }
}

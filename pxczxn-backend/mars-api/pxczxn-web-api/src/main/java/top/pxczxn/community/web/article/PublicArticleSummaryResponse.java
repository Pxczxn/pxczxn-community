package top.pxczxn.community.web.article;

import top.pxczxn.community.article.application.PublicArticleSummaryView;

import java.time.LocalDateTime;
import java.util.List;

public record PublicArticleSummaryResponse(
        String articleId,
        String title,
        String slug,
        String summary,
        String coverFileId,
        String contentMode,
        PublicArticleAuthorResponse author,
        PublicArticleCategoryResponse category,
        List<PublicArticleTagResponse> tags,
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

    static PublicArticleSummaryResponse from(PublicArticleSummaryView view) {
        return new PublicArticleSummaryResponse(
                PublicArticleAuthorResponse.id(view.articleId()),
                view.title(),
                view.slug(),
                view.summary(),
                PublicArticleAuthorResponse.id(view.coverFileId()),
                view.contentMode(),
                PublicArticleAuthorResponse.from(view.author()),
                PublicArticleCategoryResponse.from(view.category()),
                view.tags().stream()
                        .map(PublicArticleTagResponse::from)
                        .toList(),
                view.publishedAt(),
                view.updatedAt(),
                view.wordCount(),
                view.readingTimeMinutes(),
                view.viewCount(),
                view.likeCount(),
                view.favoriteCount(),
                view.commentCount(),
                view.canonicalPath()
        );
    }
}

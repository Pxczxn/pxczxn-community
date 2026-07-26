package top.pxczxn.community.web.article;

import top.pxczxn.community.article.application.PublicArticleDetailView;

import java.time.LocalDateTime;
import java.util.List;

public record PublicArticleDetailResponse(
        String articleId,
        String title,
        String slug,
        String summary,
        String coverFileId,
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
        PublicArticleAuthorResponse author,
        PublicArticleBlogResponse blog,
        PublicArticleCategoryResponse category,
        List<PublicArticleTagResponse> tags,
        PublicArticleSeoResponse seo
) {

    static PublicArticleDetailResponse from(PublicArticleDetailView view) {
        return new PublicArticleDetailResponse(
                PublicArticleAuthorResponse.id(view.articleId()),
                view.title(),
                view.slug(),
                view.summary(),
                PublicArticleAuthorResponse.id(view.coverFileId()),
                view.contentMode(),
                view.visibility(),
                view.renderedHtml(),
                view.tocJson(),
                view.wordCount(),
                view.readingTimeMinutes(),
                view.publishedAt(),
                view.updatedAt(),
                view.viewCount(),
                view.likeCount(),
                view.favoriteCount(),
                view.commentCount(),
                view.canonicalPath(),
                PublicArticleAuthorResponse.from(view.author()),
                PublicArticleBlogResponse.from(view.blog()),
                PublicArticleCategoryResponse.from(view.category()),
                view.tags().stream()
                        .map(PublicArticleTagResponse::from)
                        .toList(),
                PublicArticleSeoResponse.from(view.seo())
        );
    }
}

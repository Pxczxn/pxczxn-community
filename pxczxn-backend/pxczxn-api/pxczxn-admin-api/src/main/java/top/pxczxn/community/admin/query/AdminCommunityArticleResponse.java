package top.pxczxn.community.admin.query;

import top.pxczxn.community.admin.application.AdminCommunityArticleView;

import java.time.LocalDateTime;
import java.util.List;

public record AdminCommunityArticleResponse(
        String id,
        String blogId,
        String blogName,
        String blogSlug,
        String authorUserId,
        String authorUsername,
        String categoryId,
        String categoryName,
        String title,
        String slug,
        String summary,
        String contentMode,
        String visibility,
        String publishMethod,
        String publishStatus,
        String reviewStatus,
        String currentVersionId,
        String publishedVersionId,
        String reviewVersionId,
        Integer currentVersionNo,
        String renderedHtml,
        String tocJson,
        Integer wordCount,
        Integer readingTimeMinutes,
        List<String> tags,
        LocalDateTime scheduledPublishAt,
        LocalDateTime publishedAt,
        String canonicalPath,
        long viewCount,
        long likeCount,
        long favoriteCount,
        long commentCount,
        int lockVersion,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    static AdminCommunityArticleResponse from(
            AdminCommunityArticleView view
    ) {
        return new AdminCommunityArticleResponse(
                id(view.id()),
                id(view.blogId()),
                view.blogName(),
                view.blogSlug(),
                id(view.authorUserId()),
                view.authorUsername(),
                id(view.categoryId()),
                view.categoryName(),
                view.title(),
                view.slug(),
                view.summary(),
                view.contentMode(),
                view.visibility(),
                view.publishMethod(),
                view.publishStatus(),
                view.reviewStatus(),
                id(view.currentVersionId()),
                id(view.publishedVersionId()),
                id(view.reviewVersionId()),
                view.currentVersionNo(),
                view.renderedHtml(),
                view.tocJson(),
                view.wordCount(),
                view.readingTimeMinutes(),
                view.tags(),
                view.scheduledPublishAt(),
                view.publishedAt(),
                view.canonicalPath(),
                view.viewCount(),
                view.likeCount(),
                view.favoriteCount(),
                view.commentCount(),
                view.lockVersion(),
                view.createdAt(),
                view.updatedAt()
        );
    }

    private static String id(Long value) {
        return value == null ? null : value.toString();
    }
}

package top.pxczxn.community.admin.application;

import java.time.LocalDateTime;
import java.util.List;

public record AdminCommunityArticleView(
        Long id,
        Long blogId,
        String blogName,
        String blogSlug,
        Long authorUserId,
        String authorUsername,
        Long categoryId,
        String categoryName,
        String title,
        String slug,
        String summary,
        String contentMode,
        String visibility,
        String publishMethod,
        String publishStatus,
        String reviewStatus,
        Long currentVersionId,
        Long publishedVersionId,
        Long reviewVersionId,
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
}

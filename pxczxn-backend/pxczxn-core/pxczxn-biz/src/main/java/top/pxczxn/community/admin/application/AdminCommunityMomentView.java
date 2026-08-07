package top.pxczxn.community.admin.application;

import java.time.LocalDateTime;

public record AdminCommunityMomentView(
        Long id,
        Long actorUserId,
        String actorUsername,
        String actorDisplayName,
        Long blogId,
        String blogName,
        String blogSlug,
        String momentType,
        String textContent,
        String renderedHtml,
        String linkUrl,
        Long articleId,
        Long repostMomentId,
        String visibility,
        String status,
        long likeCount,
        long favoriteCount,
        long commentCount,
        long repostCount,
        int lockVersion,
        long eventCount,
        long reportCount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime deletedAt
) {
}

package top.pxczxn.community.admin.application;

import java.time.LocalDateTime;

public record AdminCommunityCommentView(
        Long id,
        Long authorUserId,
        String authorUsername,
        String authorDisplayName,
        String targetType,
        Long targetId,
        String targetTitle,
        Long rootCommentId,
        Long parentCommentId,
        Long replyToUserId,
        String contentText,
        String renderedHtml,
        String status,
        long likeCount,
        int lockVersion,
        long eventCount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime deletedAt
) {
}

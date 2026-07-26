package top.pxczxn.community.social.application;

import java.time.LocalDateTime;

public record CommentView(
        Long commentId,
        String targetType,
        Long targetId,
        Long rootCommentId,
        Long parentCommentId,
        Long replyToUserId,
        CommentAuthorView author,
        String contentText,
        String renderedHtml,
        String status,
        boolean deleted,
        long likeCount,
        boolean liked,
        boolean moderationWarning,
        String moderationResult,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}

package top.pxczxn.community.web.social;

import top.pxczxn.community.social.application.CommentView;

import java.time.LocalDateTime;

public record CommentResponse(
        String commentId,
        String targetType,
        String targetId,
        String rootCommentId,
        String parentCommentId,
        String replyToUserId,
        CommentAuthorResponse author,
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

    static CommentResponse from(CommentView view) {
        return new CommentResponse(
                id(view.commentId()),
                view.targetType(),
                id(view.targetId()),
                id(view.rootCommentId()),
                id(view.parentCommentId()),
                id(view.replyToUserId()),
                CommentAuthorResponse.from(view.author()),
                view.contentText(),
                view.renderedHtml(),
                view.status(),
                view.deleted(),
                view.likeCount(),
                view.liked(),
                view.moderationWarning(),
                view.moderationResult(),
                view.createdAt(),
                view.updatedAt()
        );
    }

    private static String id(Long value) {
        return value == null ? null : value.toString();
    }
}

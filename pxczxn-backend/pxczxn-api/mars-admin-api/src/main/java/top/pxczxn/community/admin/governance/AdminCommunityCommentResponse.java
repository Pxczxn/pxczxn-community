package top.pxczxn.community.admin.governance;

import top.pxczxn.community.admin.application.AdminCommunityCommentView;

import java.time.LocalDateTime;

public record AdminCommunityCommentResponse(
        String id,
        String authorUserId,
        String authorUsername,
        String authorDisplayName,
        String targetType,
        String targetId,
        String targetTitle,
        String rootCommentId,
        String parentCommentId,
        String replyToUserId,
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

    static AdminCommunityCommentResponse from(
            AdminCommunityCommentView view
    ) {
        return new AdminCommunityCommentResponse(
                id(view.id()),
                id(view.authorUserId()),
                view.authorUsername(),
                view.authorDisplayName(),
                view.targetType(),
                id(view.targetId()),
                view.targetTitle(),
                id(view.rootCommentId()),
                id(view.parentCommentId()),
                id(view.replyToUserId()),
                view.contentText(),
                view.renderedHtml(),
                view.status(),
                view.likeCount(),
                view.lockVersion(),
                view.eventCount(),
                view.createdAt(),
                view.updatedAt(),
                view.deletedAt()
        );
    }

    private static String id(Long value) {
        return value == null ? null : value.toString();
    }
}

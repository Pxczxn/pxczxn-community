package top.pxczxn.community.admin.governance;

import top.pxczxn.community.admin.application.AdminCommunityMomentView;

import java.time.LocalDateTime;

public record AdminCommunityMomentResponse(
        String id,
        String actorUserId,
        String actorUsername,
        String actorDisplayName,
        String blogId,
        String blogName,
        String blogSlug,
        String momentType,
        String textContent,
        String renderedHtml,
        String linkUrl,
        String articleId,
        String repostMomentId,
        String visibility,
        String status,
        long likeCount,
        long favoriteCount,
        long commentCount,
        long repostCount,
        int lockVersion,
        long eventCount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime deletedAt
) {

    static AdminCommunityMomentResponse from(
            AdminCommunityMomentView view
    ) {
        return new AdminCommunityMomentResponse(
                id(view.id()),
                id(view.actorUserId()),
                view.actorUsername(),
                view.actorDisplayName(),
                id(view.blogId()),
                view.blogName(),
                view.blogSlug(),
                view.momentType(),
                view.textContent(),
                view.renderedHtml(),
                view.linkUrl(),
                id(view.articleId()),
                id(view.repostMomentId()),
                view.visibility(),
                view.status(),
                view.likeCount(),
                view.favoriteCount(),
                view.commentCount(),
                view.repostCount(),
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

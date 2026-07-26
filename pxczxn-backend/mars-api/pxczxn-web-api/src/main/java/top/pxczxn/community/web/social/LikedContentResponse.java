package top.pxczxn.community.web.social;

import top.pxczxn.community.social.application.LikedContentView;

import java.time.LocalDateTime;

public record LikedContentResponse(
        String likeId,
        String targetType,
        String targetId,
        String authorUserId,
        String blogId,
        String title,
        String excerpt,
        String coverFileId,
        String canonicalPath,
        long likeCount,
        LocalDateTime likedAt
) {

    static LikedContentResponse from(LikedContentView view) {
        return new LikedContentResponse(
                id(view.likeId()),
                view.targetType(),
                id(view.targetId()),
                id(view.authorUserId()),
                id(view.blogId()),
                view.title(),
                view.excerpt(),
                id(view.coverFileId()),
                view.canonicalPath(),
                view.likeCount(),
                view.likedAt()
        );
    }

    private static String id(Long value) {
        return value == null ? null : value.toString();
    }
}

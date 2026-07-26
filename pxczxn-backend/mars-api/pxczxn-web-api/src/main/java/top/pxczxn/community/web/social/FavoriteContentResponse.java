package top.pxczxn.community.web.social;

import top.pxczxn.community.social.application.FavoriteContentView;

import java.time.LocalDateTime;

public record FavoriteContentResponse(
        String favoriteItemId,
        String targetType,
        String targetId,
        String authorUserId,
        String blogId,
        String title,
        String excerpt,
        String coverFileId,
        String canonicalPath,
        long favoriteCount,
        LocalDateTime favoritedAt
) {

    static FavoriteContentResponse from(FavoriteContentView view) {
        return new FavoriteContentResponse(
                id(view.favoriteItemId()),
                view.targetType(),
                id(view.targetId()),
                id(view.authorUserId()),
                id(view.blogId()),
                view.title(),
                view.excerpt(),
                id(view.coverFileId()),
                view.canonicalPath(),
                view.favoriteCount(),
                view.favoritedAt()
        );
    }

    private static String id(Long value) {
        return value == null ? null : value.toString();
    }
}

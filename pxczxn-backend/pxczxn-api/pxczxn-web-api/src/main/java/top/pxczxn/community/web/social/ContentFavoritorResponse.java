package top.pxczxn.community.web.social;

import top.pxczxn.community.social.application.ContentFavoritorView;

import java.time.LocalDateTime;

public record ContentFavoritorResponse(
        String userId,
        String username,
        String displayName,
        String avatarFileId,
        LocalDateTime favoritedAt
) {

    static ContentFavoritorResponse from(ContentFavoritorView view) {
        return new ContentFavoritorResponse(
                id(view.userId()),
                view.username(),
                view.displayName(),
                id(view.avatarFileId()),
                view.favoritedAt()
        );
    }

    private static String id(Long value) {
        return value == null ? null : value.toString();
    }
}

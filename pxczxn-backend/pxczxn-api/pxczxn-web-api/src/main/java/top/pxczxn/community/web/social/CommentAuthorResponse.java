package top.pxczxn.community.web.social;

import top.pxczxn.community.social.application.CommentAuthorView;

public record CommentAuthorResponse(
        String userId,
        String username,
        String displayName,
        String avatarFileId
) {

    static CommentAuthorResponse from(CommentAuthorView view) {
        if (view == null) {
            return null;
        }
        return new CommentAuthorResponse(
                id(view.userId()),
                view.username(),
                view.displayName(),
                id(view.avatarFileId())
        );
    }

    private static String id(Long value) {
        return value == null ? null : value.toString();
    }
}

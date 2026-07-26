package top.pxczxn.community.web.social;

import top.pxczxn.community.social.application.MomentAuthorView;

public record MomentAuthorResponse(
        String userId,
        String username,
        String displayName,
        String avatarFileId
) {

    static MomentAuthorResponse from(MomentAuthorView view) {
        if (view == null) {
            return null;
        }
        return new MomentAuthorResponse(
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

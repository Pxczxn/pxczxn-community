package top.pxczxn.community.web.article;

import top.pxczxn.community.article.application.PublicArticleAuthorView;

public record PublicArticleAuthorResponse(
        String userId,
        String username,
        String displayName,
        String bio,
        String avatarFileId
) {

    static PublicArticleAuthorResponse from(PublicArticleAuthorView view) {
        return new PublicArticleAuthorResponse(
                id(view.userId()),
                view.username(),
                view.displayName(),
                view.bio(),
                id(view.avatarFileId())
        );
    }

    static String id(Long value) {
        return value == null ? null : value.toString();
    }
}

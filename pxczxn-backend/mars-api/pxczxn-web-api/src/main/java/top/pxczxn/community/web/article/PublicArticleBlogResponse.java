package top.pxczxn.community.web.article;

import top.pxczxn.community.article.application.PublicArticleBlogView;

public record PublicArticleBlogResponse(
        String blogId,
        String blogType,
        String name,
        String slug,
        String summary,
        String avatarFileId,
        String backgroundFileId,
        String themeKey,
        String themeConfigJson
) {

    static PublicArticleBlogResponse from(PublicArticleBlogView view) {
        return new PublicArticleBlogResponse(
                PublicArticleAuthorResponse.id(view.blogId()),
                view.blogType(),
                view.name(),
                view.slug(),
                view.summary(),
                PublicArticleAuthorResponse.id(view.avatarFileId()),
                PublicArticleAuthorResponse.id(view.backgroundFileId()),
                view.themeKey(),
                view.themeConfigJson()
        );
    }
}

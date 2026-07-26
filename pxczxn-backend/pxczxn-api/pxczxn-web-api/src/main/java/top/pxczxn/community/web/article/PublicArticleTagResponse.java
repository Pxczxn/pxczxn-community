package top.pxczxn.community.web.article;

import top.pxczxn.community.article.application.PublicArticleTagView;

public record PublicArticleTagResponse(
        String tagId,
        String name,
        String slug
) {

    static PublicArticleTagResponse from(PublicArticleTagView view) {
        return new PublicArticleTagResponse(
                PublicArticleAuthorResponse.id(view.tagId()),
                view.name(),
                view.slug()
        );
    }
}

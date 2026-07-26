package top.pxczxn.community.web.article;

import top.pxczxn.community.article.application.PublicArticleSeoView;

public record PublicArticleSeoResponse(
        String title,
        String description,
        String canonicalPath
) {

    static PublicArticleSeoResponse from(PublicArticleSeoView view) {
        return new PublicArticleSeoResponse(
                view.title(),
                view.description(),
                view.canonicalPath()
        );
    }
}

package top.pxczxn.community.web.article;

import top.pxczxn.community.article.application.PublicArticleCategoryView;

public record PublicArticleCategoryResponse(
        String categoryId,
        String name,
        String slug,
        String description,
        boolean defaultCategory
) {

    static PublicArticleCategoryResponse from(PublicArticleCategoryView view) {
        return view == null
                ? null
                : new PublicArticleCategoryResponse(
                        PublicArticleAuthorResponse.id(view.categoryId()),
                        view.name(),
                        view.slug(),
                        view.description(),
                        view.defaultCategory()
                );
    }
}

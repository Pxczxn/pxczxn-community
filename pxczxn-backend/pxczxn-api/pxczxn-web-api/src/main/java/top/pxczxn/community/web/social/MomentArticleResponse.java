package top.pxczxn.community.web.social;

import top.pxczxn.community.social.application.MomentArticleView;

public record MomentArticleResponse(
        String articleId,
        boolean available,
        String title,
        String summary,
        String coverFileId,
        String canonicalPath
) {

    static MomentArticleResponse from(MomentArticleView view) {
        if (view == null) {
            return null;
        }
        return new MomentArticleResponse(
                id(view.articleId()),
                view.available(),
                view.title(),
                view.summary(),
                id(view.coverFileId()),
                view.canonicalPath()
        );
    }

    private static String id(Long value) {
        return value == null ? null : value.toString();
    }
}

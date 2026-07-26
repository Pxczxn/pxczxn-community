package top.pxczxn.community.web.social;

import top.pxczxn.community.social.application.MomentSourceView;

import java.time.LocalDateTime;

public record MomentSourceResponse(
        String momentId,
        boolean available,
        String momentType,
        MomentAuthorResponse author,
        MomentBlogResponse blog,
        String textContent,
        String renderedHtml,
        String linkUrl,
        MomentArticleResponse article,
        String repostMomentId,
        LocalDateTime createdAt
) {

    static MomentSourceResponse from(MomentSourceView view) {
        if (view == null) {
            return null;
        }
        return new MomentSourceResponse(
                id(view.momentId()),
                view.available(),
                view.momentType(),
                MomentAuthorResponse.from(view.author()),
                MomentBlogResponse.from(view.blog()),
                view.textContent(),
                view.renderedHtml(),
                view.linkUrl(),
                MomentArticleResponse.from(view.article()),
                id(view.repostMomentId()),
                view.createdAt()
        );
    }

    private static String id(Long value) {
        return value == null ? null : value.toString();
    }
}

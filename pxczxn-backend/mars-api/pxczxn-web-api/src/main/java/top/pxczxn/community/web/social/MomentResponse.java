package top.pxczxn.community.web.social;

import top.pxczxn.community.social.application.MomentView;

import java.time.LocalDateTime;

public record MomentResponse(
        String momentId,
        String momentType,
        MomentAuthorResponse author,
        MomentBlogResponse blog,
        String textContent,
        String renderedHtml,
        String linkUrl,
        MomentArticleResponse article,
        MomentSourceResponse repostSource,
        String visibility,
        String status,
        long likeCount,
        long favoriteCount,
        long commentCount,
        long repostCount,
        boolean liked,
        boolean favorited,
        String canonicalPath,
        int lockVersion,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    static MomentResponse from(MomentView view) {
        return new MomentResponse(
                view.momentId().toString(),
                view.momentType(),
                MomentAuthorResponse.from(view.author()),
                MomentBlogResponse.from(view.blog()),
                view.textContent(),
                view.renderedHtml(),
                view.linkUrl(),
                MomentArticleResponse.from(view.article()),
                MomentSourceResponse.from(view.repostSource()),
                view.visibility(),
                view.status(),
                view.likeCount(),
                view.favoriteCount(),
                view.commentCount(),
                view.repostCount(),
                view.liked(),
                view.favorited(),
                view.canonicalPath(),
                view.lockVersion(),
                view.createdAt(),
                view.updatedAt()
        );
    }
}

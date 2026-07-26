package top.pxczxn.community.social.application;

import java.time.LocalDateTime;

public record MomentSourceView(
        Long momentId,
        boolean available,
        String momentType,
        MomentAuthorView author,
        MomentBlogView blog,
        String textContent,
        String renderedHtml,
        String linkUrl,
        MomentArticleView article,
        Long repostMomentId,
        LocalDateTime createdAt
) {
}

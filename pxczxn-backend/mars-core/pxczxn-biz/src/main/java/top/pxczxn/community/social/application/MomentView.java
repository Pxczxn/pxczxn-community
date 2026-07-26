package top.pxczxn.community.social.application;

import java.time.LocalDateTime;

public record MomentView(
        Long momentId,
        String momentType,
        MomentAuthorView author,
        MomentBlogView blog,
        String textContent,
        String renderedHtml,
        String linkUrl,
        MomentArticleView article,
        MomentSourceView repostSource,
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
}

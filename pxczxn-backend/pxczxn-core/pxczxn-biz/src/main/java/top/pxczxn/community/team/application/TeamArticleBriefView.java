package top.pxczxn.community.team.application;

import java.time.LocalDateTime;

/** A row of the workspace "最近文章" list. Drafts are included, so status must always be carried. */
public record TeamArticleBriefView(
        Long articleId,
        String title,
        String slug,
        String publishStatus,
        String reviewStatus,
        String visibility,
        Long authorUserId,
        String authorDisplayName,
        Integer viewCount,
        Integer likeCount,
        Integer commentCount,
        LocalDateTime updatedAt,
        LocalDateTime publishedAt) {
}

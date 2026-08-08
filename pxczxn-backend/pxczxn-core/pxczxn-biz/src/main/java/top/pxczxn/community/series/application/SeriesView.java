package top.pxczxn.community.series.application;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Public representation of a series. A series always belongs to one blog, never directly to a team.
 * The blog/creator fields let the frontend render the "who maintains this" block without re-joining.
 * The {@code viewer*} fields describe the current reader and are empty for anonymous visitors.
 */
public record SeriesView(
        Long id,
        Long blogId, String blogName, String blogSlug, String blogType,
        Long createdByUserId, String creatorDisplayName, String creatorUsername, Long creatorAvatarFileId,
        String title, String slug, String summary, Long coverFileId,
        String serializationStatus, String reviewStatus, String reviewComment,
        Integer chapterCount, Long followerCount, Boolean viewerFollowing,
        Long viewerLastReadArticleId, Integer viewerReadChapterCount,
        Integer lockVersion, LocalDateTime publishedAt, LocalDateTime updatedAt,
        List<SeriesChapterView> chapters) {
}

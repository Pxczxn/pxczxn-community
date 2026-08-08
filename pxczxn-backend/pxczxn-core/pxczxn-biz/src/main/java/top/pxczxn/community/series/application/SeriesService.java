package top.pxczxn.community.series.application;

import java.util.List;

public interface SeriesService {
    SeriesView create(Long actorUserId, CreateSeriesCommand command);
    SeriesView update(Long actorUserId, Long seriesId, UpdateSeriesCommand command);
    SeriesView replaceChapters(Long actorUserId, Long seriesId, List<Long> articleIds, Integer expectedLockVersion);
    SeriesView submitReview(Long actorUserId, Long seriesId, Integer expectedLockVersion);

    /** Manage list for one blog (drafts / review / published), requires manage permission. */
    List<SeriesView> blogSeries(Long actorUserId, Long blogId);
    List<SeriesChapterView> eligibleArticles(Long actorUserId, Long blogId);

    /**
     * Public reads. {@code viewerUserId} is null for anonymous visitors and only decides whether the
     * follow flag and reading progress are filled in; it never changes which series are visible.
     */
    List<SeriesView> publicSeries(Long viewerUserId);
    SeriesView publicSeries(Long seriesId, Long viewerUserId);

    /** Approved series of one blog, shown on the public blog portal; no membership required. */
    List<SeriesView> publicBlogSeries(Long blogId, Long viewerUserId);

    /**
     * Series context for one article: the public series it belongs to, its ordered published chapters
     * (so the reader can navigate to the previous / next chapter), and the current viewer's state.
     * {@code null} when the article is not part of any publicly visible series.
     */
    ArticleSeriesContextView articleSeriesContext(Long articleId, Long viewerUserId);

    /** Series the reader follows, newest follow first. */
    List<SeriesView> followedSeries(Long viewerUserId);

    /** Series the reader recently opened, newest read first; powers the "continue reading" shelf. */
    List<SeriesView> recentlyReadSeries(Long viewerUserId, int limit);

    List<SeriesView> reviewQueue();
    SeriesView approve(Long adminId, Long seriesId, SeriesReviewDecisionCommand command);
    SeriesView reject(Long adminId, Long seriesId, SeriesReviewDecisionCommand command);
}

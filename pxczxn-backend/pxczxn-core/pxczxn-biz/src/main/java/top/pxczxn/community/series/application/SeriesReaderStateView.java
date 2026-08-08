package top.pxczxn.community.series.application;

/**
 * Everything the reader-side UI needs after following, unfollowing or reading a chapter.
 * Returned by every {@link SeriesReaderService} write so the frontend never has to refetch the series.
 */
public record SeriesReaderStateView(
        Long seriesId,
        boolean following,
        long followerCount,
        Long lastReadArticleId,
        int readChapterCount,
        int chapterCount) {
}

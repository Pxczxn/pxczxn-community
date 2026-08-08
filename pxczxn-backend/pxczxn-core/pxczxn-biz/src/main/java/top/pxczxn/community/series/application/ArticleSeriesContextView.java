package top.pxczxn.community.series.application;

import java.util.List;

/**
 * Series context for a single article: which public series it belongs to, the ordered
 * published chapters of that series (so the reader can move to the previous / next chapter),
 * and the current reader's follow / progress state. Returned as {@code null} when the article
 * is not part of any publicly visible series, so the article page simply renders no chapter nav.
 */
public record ArticleSeriesContextView(
        Long seriesId,
        String seriesSlug,
        String seriesTitle,
        Integer chapterOrder,
        long followerCount,
        boolean following,
        Long lastReadArticleId,
        int readChapterCount,
        List<SeriesChapterView> chapters) {
}

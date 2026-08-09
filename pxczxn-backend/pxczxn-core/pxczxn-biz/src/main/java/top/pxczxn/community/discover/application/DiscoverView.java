package top.pxczxn.community.discover.application;

import top.pxczxn.community.editorial.application.EditorialCollectionView;
import top.pxczxn.community.article.application.PublicArticleSummaryView;
import top.pxczxn.community.blog.persistence.BlogPopularRow;
import top.pxczxn.community.series.persistence.SeriesPopularRow;
import top.pxczxn.community.social.persistence.MomentLatestRow;
import top.pxczxn.community.taxonomy.application.PlatformTagView;

import java.util.List;

/**
 * Discover Hub聚合返回结构。各领域由独立的Mapper/Service完成排序和过滤，
 * DiscoverService只负责调用、限量和组装。
 */
public record DiscoverView(
        List<EditorialCollectionView> editorials,
        List<PublicArticleSummaryView> hotArticles,
        List<SeriesPopularRow> popularSeries,
        List<BlogPopularRow> popularCreators,
        List<BlogPopularRow> popularTeams,
        List<MomentLatestRow> latestMoments,
        List<PlatformTagView> trendingTags
) {
}
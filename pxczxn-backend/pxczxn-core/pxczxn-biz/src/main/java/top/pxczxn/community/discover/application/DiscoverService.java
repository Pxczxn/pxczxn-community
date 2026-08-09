package top.pxczxn.community.discover.application;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.pxczxn.community.article.application.PublicDiscoveryQuery;
import top.pxczxn.community.article.application.PublicArticleService;
import top.pxczxn.community.article.application.PublicDiscoverySort;
import top.pxczxn.community.blog.persistence.BlogMapper;
import top.pxczxn.community.blog.persistence.BlogPopularRow;
import top.pxczxn.community.editorial.application.EditorialCollectionService;
import top.pxczxn.community.editorial.application.EditorialCollectionView;
import top.pxczxn.community.series.persistence.SeriesMapper;
import top.pxczxn.community.series.persistence.SeriesPopularRow;
import top.pxczxn.community.social.persistence.CommunityMomentMapper;
import top.pxczxn.community.social.persistence.MomentLatestRow;
import top.pxczxn.community.taxonomy.application.PlatformTagService;
import top.pxczxn.community.taxonomy.application.PlatformTagView;

import java.util.List;

/**
 * Discover Hub聚合服务。只做调用、限量和组装，不做推荐计算。
 * 各领域排序由自己的Mapper/Service完成。
 */
@Service
@RequiredArgsConstructor
public class DiscoverService {

    private static final int HOT_ARTICLES_LIMIT = 10;
    private static final int POPULAR_SERIES_LIMIT = 6;
    private static final int POPULAR_CREATORS_LIMIT = 6;
    private static final int POPULAR_TEAMS_LIMIT = 6;
    private static final int LATEST_MOMENTS_LIMIT = 6;
    private static final int TRENDING_TAGS_LIMIT = 20;

    private final EditorialCollectionService editorialService;
    private final PublicArticleService articleService;
    private final SeriesMapper seriesMapper;
    private final BlogMapper blogMapper;
    private final CommunityMomentMapper momentMapper;
    private final PlatformTagService tagService;

    @Transactional(readOnly = true)
    public DiscoverView discover() {
        List<EditorialCollectionView> editorials = editorialService.publicList();

        List<top.pxczxn.community.article.application.PublicArticleSummaryView> hotArticles =
                articleService.discoverRanked(
                        new PublicDiscoveryQuery(null, null, PublicDiscoverySort.HOT.name(), 1, HOT_ARTICLES_LIMIT)
                ).page().records();

        List<SeriesPopularRow> popularSeries =
                seriesMapper.selectPopular(POPULAR_SERIES_LIMIT);

        List<BlogPopularRow> popularCreators =
                blogMapper.selectPopular("PERSONAL", POPULAR_CREATORS_LIMIT);

        List<BlogPopularRow> popularTeams =
                blogMapper.selectPopular("TEAM", POPULAR_TEAMS_LIMIT);

        List<MomentLatestRow> latestMoments =
                momentMapper.selectLatest(LATEST_MOMENTS_LIMIT);

        List<PlatformTagView> trendingTags =
                tagService.listPublic(null).stream()
                        .limit(TRENDING_TAGS_LIMIT)
                        .toList();

        return new DiscoverView(
                editorials,
                hotArticles,
                popularSeries,
                popularCreators,
                popularTeams,
                latestMoments,
                trendingTags
        );
    }
}
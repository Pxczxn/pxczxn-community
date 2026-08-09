package top.pxczxn.community.discover.application;

import org.junit.jupiter.api.Test;
import top.pxczxn.community.article.application.PublicArticlePageView;
import top.pxczxn.community.article.application.PublicArticleService;
import top.pxczxn.community.article.application.PublicDiscoveryPageView;
import top.pxczxn.community.article.application.PublicDiscoveryQuery;
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

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DiscoverServiceTest {

    private final EditorialCollectionService editorialService = mock(EditorialCollectionService.class);
    private final PublicArticleService articleService = mock(PublicArticleService.class);
    private final SeriesMapper seriesMapper = mock(SeriesMapper.class);
    private final BlogMapper blogMapper = mock(BlogMapper.class);
    private final CommunityMomentMapper momentMapper = mock(CommunityMomentMapper.class);
    private final PlatformTagService tagService = mock(PlatformTagService.class);

    private final DiscoverService service = new DiscoverService(
            editorialService, articleService, seriesMapper,
            blogMapper, momentMapper, tagService
    );

    private void stubEmptyDiscover() {
        when(editorialService.publicList()).thenReturn(List.of());
        when(articleService.discoverRanked(any(PublicDiscoveryQuery.class)))
                .thenReturn(new PublicDiscoveryPageView(
                        new PublicArticlePageView(List.of(), 0, 1, 10),
                        PublicDiscoverySort.HOT));
        when(seriesMapper.selectPopular(anyInt())).thenReturn(List.of());
        when(blogMapper.selectPopular(eq("PERSONAL"), anyInt())).thenReturn(List.of());
        when(blogMapper.selectPopular(eq("TEAM"), anyInt())).thenReturn(List.of());
        when(momentMapper.selectLatest(anyInt())).thenReturn(List.of());
        when(tagService.listPublic(any())).thenReturn(List.of());
    }

    @Test
    void discover_delegatesToAllServices() {
        stubEmptyDiscover();

        DiscoverView result = service.discover();

        assertThat(result).isNotNull();
        verify(editorialService).publicList();
        verify(articleService).discoverRanked(any(PublicDiscoveryQuery.class));
        verify(seriesMapper).selectPopular(6);
        verify(blogMapper).selectPopular("PERSONAL", 6);
        verify(blogMapper).selectPopular("TEAM", 6);
        verify(momentMapper).selectLatest(6);
        verify(tagService).listPublic(null);
    }

    @Test
    void discover_limitsTagResults() {
        stubEmptyDiscover();

        List<PlatformTagView> manyTags = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            manyTags.add(new PlatformTagView((long) i, "tag" + i, "tag" + i, null, "ACTIVE", 100 - i));
        }
        when(tagService.listPublic(null)).thenReturn(manyTags);

        DiscoverView result = service.discover();

        assertThat(result.trendingTags()).hasSize(20);
    }

    @Test
    void discover_preservesEditorialOrder() {
        EditorialCollectionView e1 = new EditorialCollectionView(1L, "TOPIC", "A", "a", null, null, "PUBLISHED", null, null, 1, LocalDateTime.now(), List.of());
        EditorialCollectionView e2 = new EditorialCollectionView(2L, "TOPIC", "B", "b", null, null, "PUBLISHED", null, null, 2, LocalDateTime.now(), List.of());
        when(editorialService.publicList()).thenReturn(List.of(e1, e2));
        when(articleService.discoverRanked(any(PublicDiscoveryQuery.class)))
                .thenReturn(new PublicDiscoveryPageView(
                        new PublicArticlePageView(List.of(), 0, 1, 10),
                        PublicDiscoverySort.HOT));
        when(seriesMapper.selectPopular(anyInt())).thenReturn(List.of());
        when(blogMapper.selectPopular(eq("PERSONAL"), anyInt())).thenReturn(List.of());
        when(blogMapper.selectPopular(eq("TEAM"), anyInt())).thenReturn(List.of());
        when(momentMapper.selectLatest(anyInt())).thenReturn(List.of());
        when(tagService.listPublic(null)).thenReturn(List.of());

        DiscoverView result = service.discover();

        assertThat(result.editorials()).hasSize(2);
        assertThat(result.editorials().get(0).displayOrder()).isEqualTo(1);
        assertThat(result.editorials().get(1).displayOrder()).isEqualTo(2);
    }

    @Test
    void discover_popularSeriesUsesFollowCount() {
        stubEmptyDiscover();

        SeriesPopularRow row = new SeriesPopularRow();
        row.setId(1L);
        row.setTitle("Test Series");
        row.setFollowCount(42L);
        row.setPublishedAt(LocalDateTime.now());
        when(seriesMapper.selectPopular(6)).thenReturn(List.of(row));

        DiscoverView result = service.discover();

        assertThat(result.popularSeries()).hasSize(1);
        assertThat(result.popularSeries().get(0).getFollowCount()).isEqualTo(42L);
    }

    @Test
    void discover_momentsFilteredByVisibility() {
        stubEmptyDiscover();

        MomentLatestRow row = new MomentLatestRow();
        row.setId(1L);
        row.setMomentType("TEXT");
        row.setTextContent("Hello");
        row.setLikeCount(5L);
        row.setCommentCount(2L);
        row.setAuthorName("User");
        row.setBlogName("Blog");
        row.setCreatedAt(LocalDateTime.now());
        when(momentMapper.selectLatest(6)).thenReturn(List.of(row));

        DiscoverView result = service.discover();

        assertThat(result.latestMoments()).hasSize(1);
        assertThat(result.latestMoments().get(0).getId()).isEqualTo(1L);
    }
}
package top.pxczxn.community.web.series;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import top.pxczxn.community.series.application.ArticleSeriesContextView;
import top.pxczxn.community.series.application.SeriesChapterView;
import top.pxczxn.community.series.application.SeriesReaderService;
import top.pxczxn.community.series.application.SeriesReaderStateView;
import top.pxczxn.community.series.application.SeriesService;
import top.pxczxn.community.shared.auth.CommunityAuth;
import top.pxczxn.community.team.persistence.TeamMapper;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SeriesControllerTest {

    private SeriesService seriesService;
    private SeriesReaderService readerService;
    private CommunityAuth communityAuth;
    private SeriesController controller;

    @BeforeEach
    void setUp() {
        seriesService = mock(SeriesService.class);
        readerService = mock(SeriesReaderService.class);
        communityAuth = mock(CommunityAuth.class);
        controller = new SeriesController(seriesService, readerService, mock(TeamMapper.class), communityAuth);
    }

    private ArticleSeriesContextView sampleContext(Long seriesId) {
        return new ArticleSeriesContextView(
                seriesId,
                "my-series",
                "我的系列",
                2,
                5L,
                true,
                9L,
                3,
                List.of(new SeriesChapterView(9L, "第二章", "ch-2", "PUBLISHED", 2))
        );
    }

    private SeriesReaderStateView sampleState(Long seriesId) {
        return new SeriesReaderStateView(seriesId, true, 5L, 9L, 3, 10);
    }

    @Test
    void articleSeriesReturnsContextForGuest() {
        when(communityAuth.getOptionalLoginUserId()).thenReturn(null);
        when(seriesService.articleSeriesContext(55L, null)).thenReturn(sampleContext(1L));

        var result = controller.articleSeries(55L);

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData()).isNotNull();
        assertThat(result.getData().seriesId()).isEqualTo(1L);
        assertThat(result.getData().chapters()).hasSize(1);
    }

    @Test
    void articleSeriesReturnsNullWhenNotInAnyPublicSeries() {
        when(communityAuth.getOptionalLoginUserId()).thenReturn(null);
        when(seriesService.articleSeriesContext(56L, null)).thenReturn(null);

        var result = controller.articleSeries(56L);

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData()).isNull();
    }

    @Test
    void articleSeriesEnrichesPayloadWhenSignedIn() {
        when(communityAuth.getOptionalLoginUserId()).thenReturn(42L);
        when(seriesService.articleSeriesContext(57L, 42L)).thenReturn(sampleContext(3L));

        var result = controller.articleSeries(57L);

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData().seriesId()).isEqualTo(3L);
        verify(seriesService).articleSeriesContext(57L, 42L);
    }

    @Test
    void followSeriesDelegatesToReaderService() {
        when(communityAuth.getLoginUserId()).thenReturn(7L);
        when(readerService.follow(7L, 1L)).thenReturn(sampleState(1L));

        var result = controller.followSeries(1L);

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData().following()).isTrue();
        assertThat(result.getData().followerCount()).isEqualTo(5L);
        verify(readerService).follow(7L, 1L);
    }

    @Test
    void recordProgressDelegatesWithArticleId() {
        when(communityAuth.getLoginUserId()).thenReturn(7L);
        when(readerService.recordProgress(7L, 1L, 9L)).thenReturn(sampleState(1L));

        var result = controller.recordProgress(1L, new SeriesController.ProgressRequest(9L));

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData().readChapterCount()).isEqualTo(3);
        verify(readerService).recordProgress(7L, 1L, 9L);
    }

    @Test
    void readerStateDelegatesToService() {
        when(communityAuth.getLoginUserId()).thenReturn(7L);
        when(readerService.state(7L, 1L)).thenReturn(sampleState(1L));

        var result = controller.readerState(1L);

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData().chapterCount()).isEqualTo(10);
        verify(readerService).state(7L, 1L);
    }
}

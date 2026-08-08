package top.pxczxn.community.series.application;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import top.pxczxn.community.article.model.Article;
import top.pxczxn.community.article.persistence.ArticleMapper;
import top.pxczxn.community.series.model.Series;
import top.pxczxn.community.series.model.SeriesArticle;
import top.pxczxn.community.series.model.SeriesReadingProgress;
import top.pxczxn.community.series.persistence.SeriesArticleMapper;
import top.pxczxn.community.series.persistence.SeriesMapper;
import top.pxczxn.community.series.persistence.SeriesReadingProgressMapper;
import top.pxczxn.community.social.model.CommunityFollow;
import top.pxczxn.community.social.persistence.CommunityFollowMapper;
import top.pxczxn.platform.common.exception.BusinessException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SeriesReaderServiceTest {
    private static final long SERIES = 77L;
    private static final long BLOG = 10L;
    private static final long READER = 555L;

    private SeriesMapper seriesMapper;
    private SeriesArticleMapper chapterMapper;
    private SeriesReadingProgressMapper progressMapper;
    private CommunityFollowMapper followMapper;
    private ArticleMapper articleMapper;
    private SeriesReaderService service;

    @BeforeEach
    void setUp() {
        seriesMapper = mock(SeriesMapper.class);
        chapterMapper = mock(SeriesArticleMapper.class);
        progressMapper = mock(SeriesReadingProgressMapper.class);
        followMapper = mock(CommunityFollowMapper.class);
        articleMapper = mock(ArticleMapper.class);
        service = new SeriesReaderService(seriesMapper, chapterMapper, progressMapper, followMapper, articleMapper);

        when(seriesMapper.findPublicById(SERIES)).thenReturn(approvedSeries());
        when(chapterMapper.findBySeries(SERIES)).thenReturn(List.of(chapter(601L, 1), chapter(602L, 2)));
        when(followMapper.insert(any(CommunityFollow.class))).thenReturn(1);
    }

    // --- 关注 ---

    @Test
    void followingAPublishedSeriesCreatesTheRelation() {
        SeriesReaderStateView state = service.follow(READER, SERIES);

        verify(followMapper).insert(any(CommunityFollow.class));
        assertThat(state.seriesId()).isEqualTo(SERIES);
        assertThat(state.chapterCount()).isEqualTo(2);
    }

    @Test
    void followingTwiceDoesNotInsertASecondRelation() {
        when(followMapper.findRelation(READER, "SERIES", SERIES)).thenReturn(new CommunityFollow());

        SeriesReaderStateView state = service.follow(READER, SERIES);

        verify(followMapper, never()).insert(any(CommunityFollow.class));
        assertThat(state.following()).isTrue();
    }

    @Test
    void unfollowingASeriesNeverFollowedIsANoOp() {
        SeriesReaderStateView state = service.unfollow(READER, SERIES);

        verify(followMapper).deleteRelation(READER, "SERIES", SERIES);
        assertThat(state.following()).isFalse();
    }

    @Test
    void unpublishedSeriesCannotBeFollowed() {
        when(seriesMapper.findPublicById(SERIES)).thenReturn(null);

        assertThatThrownBy(() -> service.follow(READER, SERIES))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("尚未公开");
    }

    @Test
    void anonymousVisitorCannotFollow() {
        assertThatThrownBy(() -> service.follow(null, SERIES))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("请先登录");
    }

    // --- 阅读进度 ---

    @Test
    void firstReadInsertsProgressRow() {
        givenPublishedChapter(602L, 2);
        when(progressMapper.advance(eq(READER), eq(SERIES), eq(602L), eq(2), any())).thenReturn(0);

        service.recordProgress(READER, SERIES, 602L);

        verify(progressMapper).insert(any(SeriesReadingProgress.class));
    }

    @Test
    void subsequentReadOnlyAdvancesTheExistingRow() {
        givenPublishedChapter(602L, 2);
        when(progressMapper.advance(eq(READER), eq(SERIES), eq(602L), eq(2), any())).thenReturn(1);

        service.recordProgress(READER, SERIES, 602L);

        verify(progressMapper, never()).insert(any(SeriesReadingProgress.class));
        verify(progressMapper, times(1)).advance(eq(READER), eq(SERIES), eq(602L), eq(2), any());
    }

    @Test
    void rereadingAnEarlierChapterKeepsTheFurthestProgress() {
        givenPublishedChapter(601L, 1);
        when(progressMapper.advance(eq(READER), eq(SERIES), eq(601L), eq(1), any())).thenReturn(1);
        SeriesReadingProgress stored = new SeriesReadingProgress();
        stored.setLastArticleId(601L); stored.setLastChapterOrder(1); stored.setMaxChapterOrder(2);
        when(progressMapper.findByUserAndSeries(READER, SERIES)).thenReturn(stored);

        SeriesReaderStateView state = service.recordProgress(READER, SERIES, 601L);

        assertThat(state.lastReadArticleId()).isEqualTo(601L);
        assertThat(state.readChapterCount()).isEqualTo(2);
    }

    @Test
    void articleFromAnotherSeriesIsRejected() {
        SeriesArticle foreign = chapter(999L, 1);
        foreign.setSeriesId(88L);
        when(chapterMapper.findByArticle(999L)).thenReturn(foreign);

        assertThatThrownBy(() -> service.recordProgress(READER, SERIES, 999L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不属于此系列");
    }

    @Test
    void unpublishedChapterCannotAdvanceProgress() {
        when(chapterMapper.findByArticle(602L)).thenReturn(chapter(602L, 2));
        when(articleMapper.selectById(602L)).thenReturn(article(602L, "DRAFT"));

        assertThatThrownBy(() -> service.recordProgress(READER, SERIES, 602L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("尚未发布");
    }

    @Test
    void progressRequiresAnArticle() {
        assertThatThrownBy(() -> service.recordProgress(READER, SERIES, null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("章节文章不能为空");
    }

    // --- helpers ---

    private void givenPublishedChapter(Long articleId, int order) {
        when(chapterMapper.findByArticle(articleId)).thenReturn(chapter(articleId, order));
        when(articleMapper.selectById(articleId)).thenReturn(article(articleId, "PUBLISHED"));
    }

    private static Series approvedSeries() {
        Series series = new Series();
        series.setId(SERIES); series.setBlogId(BLOG); series.setTitle("Spring Boot 实战");
        series.setSlug("spring-boot"); series.setReviewStatus("APPROVED");
        series.setSerializationStatus("ONGOING"); series.setLockVersion(0);
        return series;
    }

    private static SeriesArticle chapter(Long articleId, int order) {
        SeriesArticle chapter = new SeriesArticle();
        chapter.setSeriesId(SERIES); chapter.setBlogId(BLOG);
        chapter.setArticleId(articleId); chapter.setChapterOrder(order);
        return chapter;
    }

    private static Article article(Long id, String publishStatus) {
        Article article = new Article();
        article.setId(id); article.setBlogId(BLOG); article.setTitle("Chapter " + id);
        article.setSlug("chapter-" + id); article.setPublishStatus(publishStatus);
        return article;
    }
}

package top.pxczxn.community.series.application;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import top.pxczxn.community.article.model.Article;
import top.pxczxn.community.article.persistence.ArticleMapper;
import top.pxczxn.community.blog.model.Blog;
import top.pxczxn.community.blog.persistence.BlogMapper;
import top.pxczxn.community.series.model.Series;
import top.pxczxn.community.series.model.SeriesArticle;
import top.pxczxn.community.series.model.SeriesReadingProgress;
import top.pxczxn.community.series.persistence.SeriesArticleMapper;
import top.pxczxn.community.series.persistence.SeriesMapper;
import top.pxczxn.community.series.persistence.SeriesReadingProgressMapper;
import top.pxczxn.community.social.model.CommunityFollow;
import top.pxczxn.community.social.persistence.CommunityFollowMapper;
import top.pxczxn.community.team.application.TeamAuthorityService;
import top.pxczxn.community.team.model.Team;
import top.pxczxn.community.team.persistence.TeamAuditEventMapper;
import top.pxczxn.community.team.persistence.TeamMapper;
import top.pxczxn.community.user.persistence.CommunityUserMapper;
import top.pxczxn.platform.common.exception.BusinessException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SeriesServiceImplTest {
    private static final long PERSONAL_BLOG = 10L;
    private static final long TEAM_BLOG = 20L;
    private static final long OWNER = 100L;
    private static final long STRANGER = 999L;
    private static final long READER = 555L;

    private SeriesMapper seriesMapper;
    private SeriesArticleMapper chapterMapper;
    private BlogMapper blogMapper;
    private ArticleMapper articleMapper;
    private CommunityUserMapper userMapper;
    private TeamAuthorityService authorityService;
    private TeamAuditEventMapper auditMapper;
    private TeamMapper teamMapper;
    private CommunityFollowMapper followMapper;
    private SeriesReadingProgressMapper progressMapper;
    private SeriesServiceImpl service;

    @BeforeEach
    void setUp() {
        seriesMapper = mock(SeriesMapper.class);
        chapterMapper = mock(SeriesArticleMapper.class);
        blogMapper = mock(BlogMapper.class);
        articleMapper = mock(ArticleMapper.class);
        userMapper = mock(CommunityUserMapper.class);
        authorityService = mock(TeamAuthorityService.class);
        auditMapper = mock(TeamAuditEventMapper.class);
        teamMapper = mock(TeamMapper.class);
        followMapper = mock(CommunityFollowMapper.class);
        progressMapper = mock(SeriesReadingProgressMapper.class);
        service = new SeriesServiceImpl(seriesMapper, chapterMapper, blogMapper, articleMapper,
                userMapper, authorityService, auditMapper, teamMapper, followMapper, progressMapper);

        when(blogMapper.selectById(PERSONAL_BLOG)).thenReturn(blog(PERSONAL_BLOG, "PERSONAL", "我的博客", "owner"));
        when(blogMapper.selectById(TEAM_BLOG)).thenReturn(blog(TEAM_BLOG, "TEAM", "Team One", "team-one"));
        when(teamMapper.findByBlogId(TEAM_BLOG)).thenReturn(activeTeam(1L, TEAM_BLOG));
        when(seriesMapper.insert(any(Series.class))).thenReturn(1);
        when(chapterMapper.findBySeries(any())).thenReturn(List.of());
    }

    // --- 权限：个人博客 ---

    @Test
    void personalBlogOwnerCreatesDraftSeries() {
        SeriesView view = service.create(OWNER, new CreateSeriesCommand(
                PERSONAL_BLOG, "Spring Boot 实战", "spring-boot", null, null, null));

        assertThat(view.reviewStatus()).isEqualTo("DRAFT");
        assertThat(view.blogId()).isEqualTo(PERSONAL_BLOG);
        assertThat(view.blogType()).isEqualTo("PERSONAL");
        assertThat(view.slug()).isEqualTo("spring-boot");
    }

    @Test
    void personalSeriesCreationRejectedForNonOwner() {
        assertThatThrownBy(() -> service.create(STRANGER, new CreateSeriesCommand(
                PERSONAL_BLOG, "Spring Boot 实战", "spring-boot", null, null, null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("没有管理该系列权限");
    }

    @Test
    void personalSeriesFallsBackToIdSlugWhenTitleHasNoAsciiChars() {
        SeriesView view = service.create(OWNER, new CreateSeriesCommand(
                PERSONAL_BLOG, "从零开始学算法", null, null, null, null));

        assertThat(view.slug()).startsWith("series-");
    }

    // --- 权限：团队博客 ---

    @Test
    void teamSeriesCreationRequiresManageSeriesPermission() {
        when(authorityService.hasPermission(OWNER, 1L, "MANAGE_SERIES")).thenReturn(true);

        SeriesView view = service.create(OWNER, new CreateSeriesCommand(
                TEAM_BLOG, "Team Playbook", "team-playbook", null, null, null));

        assertThat(view.blogType()).isEqualTo("TEAM");
        assertThat(view.reviewStatus()).isEqualTo("DRAFT");
    }

    @Test
    void teamSeriesCreationRejectedWithoutManageSeriesPermission() {
        when(authorityService.hasPermission(STRANGER, 1L, "MANAGE_SERIES")).thenReturn(false);

        assertThatThrownBy(() -> service.create(STRANGER, new CreateSeriesCommand(
                TEAM_BLOG, "Team Playbook", "team-playbook", null, null, null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("没有管理该系列权限");
    }

    // --- 章节编排 ---

    @Test
    void chaptersCannotBorrowArticlesFromAnotherBlog() {
        Series series = draftSeries(77L, PERSONAL_BLOG);
        when(seriesMapper.selectById(77L)).thenReturn(series);
        Article foreign = article(601L, TEAM_BLOG, "PUBLISHED");
        when(articleMapper.selectById(601L)).thenReturn(foreign);

        assertThatThrownBy(() -> service.replaceChapters(OWNER, 77L, List.of(601L), 0))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("只能编排本博客的有效文章");
    }

    @Test
    void chaptersCannotStealArticleOwnedByAnotherSeries() {
        Series series = draftSeries(77L, PERSONAL_BLOG);
        when(seriesMapper.selectById(77L)).thenReturn(series);
        when(articleMapper.selectById(601L)).thenReturn(article(601L, PERSONAL_BLOG, "PUBLISHED"));
        SeriesArticle taken = new SeriesArticle();
        taken.setArticleId(601L);
        taken.setSeriesId(88L);
        when(chapterMapper.findByArticle(601L)).thenReturn(taken);

        assertThatThrownBy(() -> service.replaceChapters(OWNER, 77L, List.of(601L), 0))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("文章已属于另一个系列");
    }

    // --- 审核与公开视图 ---

    @Test
    void submitReviewRefusedWhenAChapterIsNotPublished() {
        Series series = draftSeries(77L, PERSONAL_BLOG);
        when(seriesMapper.selectById(77L)).thenReturn(series);
        SeriesArticle chapter = new SeriesArticle();
        chapter.setSeriesId(77L);
        chapter.setArticleId(601L);
        chapter.setChapterOrder(1);
        when(chapterMapper.findBySeries(77L)).thenReturn(List.of(chapter));
        when(articleMapper.selectById(601L)).thenReturn(article(601L, PERSONAL_BLOG, "DRAFT"));

        assertThatThrownBy(() -> service.submitReview(OWNER, 77L, 0))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("所有章节必须已经发布");
    }

    @Test
    void submitReviewRefusedForEmptySeries() {
        Series series = draftSeries(77L, PERSONAL_BLOG);
        when(seriesMapper.selectById(77L)).thenReturn(series);
        when(chapterMapper.findBySeries(77L)).thenReturn(List.of());

        assertThatThrownBy(() -> service.submitReview(OWNER, 77L, 0))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("至少需要一篇文章");
    }

    @Test
    void publicViewOmitsUnpublishedChapters() {
        Series series = draftSeries(77L, PERSONAL_BLOG);
        series.setReviewStatus("APPROVED");
        when(seriesMapper.findPublicById(77L)).thenReturn(series);
        SeriesArticle published = new SeriesArticle();
        published.setSeriesId(77L); published.setArticleId(601L); published.setChapterOrder(1);
        SeriesArticle draft = new SeriesArticle();
        draft.setSeriesId(77L); draft.setArticleId(602L); draft.setChapterOrder(2);
        when(chapterMapper.findBySeries(77L)).thenReturn(List.of(published, draft));
        when(articleMapper.selectById(601L)).thenReturn(article(601L, PERSONAL_BLOG, "PUBLISHED"));
        when(articleMapper.selectById(602L)).thenReturn(article(602L, PERSONAL_BLOG, "DRAFT"));

        SeriesView view = service.publicSeries(77L, null);

        assertThat(view.chapters()).singleElement()
                .satisfies(chapter -> assertThat(chapter.articleId()).isEqualTo(601L));
        assertThat(view.chapterCount()).isEqualTo(1);
    }

    @Test
    void publicSeriesNotFoundWhenNotApproved() {
        when(seriesMapper.findPublicById(77L)).thenReturn(null);

        assertThatThrownBy(() -> service.publicSeries(77L, null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("尚未公开");
    }

    // --- 读者上下文：关注数与阅读进度 ---

    @Test
    void publicViewFillsFollowerCountAndReaderProgress() {
        approvedSeriesWithOnePublishedChapter();
        when(followMapper.countFollowers("SERIES", 77L)).thenReturn(12L);
        when(followMapper.findRelation(READER, "SERIES", 77L)).thenReturn(new CommunityFollow());
        when(progressMapper.findByUserAndSeries(READER, 77L)).thenReturn(progress(601L, 3));

        SeriesView view = service.publicSeries(77L, READER);

        assertThat(view.followerCount()).isEqualTo(12L);
        assertThat(view.viewerFollowing()).isTrue();
        assertThat(view.viewerLastReadArticleId()).isEqualTo(601L);
        assertThat(view.viewerReadChapterCount()).isEqualTo(3);
    }

    @Test
    void anonymousViewStillShowsFollowerCountButNoPersonalState() {
        approvedSeriesWithOnePublishedChapter();
        when(followMapper.countFollowers("SERIES", 77L)).thenReturn(12L);

        SeriesView view = service.publicSeries(77L, null);

        assertThat(view.followerCount()).isEqualTo(12L);
        assertThat(view.viewerFollowing()).isFalse();
        assertThat(view.viewerLastReadArticleId()).isNull();
        assertThat(view.viewerReadChapterCount()).isZero();
        verify(progressMapper, never()).findByUserAndSeries(any(), any());
        verify(followMapper, never()).findRelation(any(), any(), any());
    }

    @Test
    void readerWithoutProgressGetsZeroInsteadOfNull() {
        approvedSeriesWithOnePublishedChapter();

        SeriesView view = service.publicSeries(77L, READER);

        assertThat(view.viewerReadChapterCount()).isZero();
        assertThat(view.viewerLastReadArticleId()).isNull();
    }

    @Test
    void recentlyReadRejectsOutOfRangeLimit() {
        assertThatThrownBy(() -> service.recentlyReadSeries(READER, 0))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("1-50");
    }

    @Test
    void followedSeriesRequiresSignedInReader() {
        assertThatThrownBy(() -> service.followedSeries(null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("请先登录");
    }

    // --- 文章上下文：系列内章节导航（#16） ---

    @Test
    void articleSeriesContextReturnsOrderedPublishedChaptersAndCurrentOrder() {
        articleInSeriesWithMixedPublishStatus();

        ArticleSeriesContextView ctx = service.articleSeriesContext(601L, null);

        assertThat(ctx).isNotNull();
        assertThat(ctx.seriesId()).isEqualTo(77L);
        assertThat(ctx.chapterOrder()).isEqualTo(1);
        assertThat(ctx.chapters()).extracting(SeriesChapterView::articleId).containsExactly(601L, 603L);
        assertThat(ctx.chapters()).extracting(SeriesChapterView::chapterOrder).containsExactly(1, 3);
    }

    @Test
    void articleSeriesContextReturnsNullWhenArticleNotInAnySeries() {
        when(chapterMapper.findByArticle(700L)).thenReturn(null);

        assertThat(service.articleSeriesContext(700L, null)).isNull();
    }

    @Test
    void articleSeriesContextReturnsNullWhenSeriesNotPublic() {
        SeriesArticle membership = new SeriesArticle();
        membership.setSeriesId(77L); membership.setArticleId(601L); membership.setChapterOrder(1);
        when(chapterMapper.findByArticle(601L)).thenReturn(membership);
        when(seriesMapper.findPublicById(77L)).thenReturn(null);

        assertThat(service.articleSeriesContext(601L, null)).isNull();
    }

    @Test
    void articleSeriesContextFillsReaderStateForSignedInViewer() {
        articleInSeriesWithMixedPublishStatus();
        when(followMapper.countFollowers("SERIES", 77L)).thenReturn(7L);
        when(followMapper.findRelation(READER, "SERIES", 77L)).thenReturn(new CommunityFollow());
        when(progressMapper.findByUserAndSeries(READER, 77L)).thenReturn(progress(601L, 2));

        ArticleSeriesContextView ctx = service.articleSeriesContext(601L, READER);

        assertThat(ctx.followerCount()).isEqualTo(7L);
        assertThat(ctx.following()).isTrue();
        assertThat(ctx.readChapterCount()).isEqualTo(2);
        assertThat(ctx.lastReadArticleId()).isEqualTo(601L);
    }

    // --- helpers ---

    private void approvedSeriesWithOnePublishedChapter() {
        Series series = draftSeries(77L, PERSONAL_BLOG);
        series.setReviewStatus("APPROVED");
        when(seriesMapper.findPublicById(77L)).thenReturn(series);
        SeriesArticle chapter = new SeriesArticle();
        chapter.setSeriesId(77L); chapter.setArticleId(601L); chapter.setChapterOrder(1);
        when(chapterMapper.findBySeries(77L)).thenReturn(List.of(chapter));
        when(articleMapper.selectById(601L)).thenReturn(article(601L, PERSONAL_BLOG, "PUBLISHED"));
    }

    private static SeriesReadingProgress progress(Long lastArticleId, int maxChapterOrder) {
        SeriesReadingProgress progress = new SeriesReadingProgress();
        progress.setUserId(READER); progress.setSeriesId(77L);
        progress.setLastArticleId(lastArticleId);
        progress.setLastChapterOrder(maxChapterOrder);
        progress.setMaxChapterOrder(maxChapterOrder);
        return progress;
    }

    private void articleInSeriesWithMixedPublishStatus() {
        Series series = draftSeries(77L, PERSONAL_BLOG);
        series.setReviewStatus("APPROVED");
        when(seriesMapper.findPublicById(77L)).thenReturn(series);
        SeriesArticle m1 = new SeriesArticle(); m1.setSeriesId(77L); m1.setArticleId(601L); m1.setChapterOrder(1);
        SeriesArticle m2 = new SeriesArticle(); m2.setSeriesId(77L); m2.setArticleId(602L); m2.setChapterOrder(2);
        SeriesArticle m3 = new SeriesArticle(); m3.setSeriesId(77L); m3.setArticleId(603L); m3.setChapterOrder(3);
        when(chapterMapper.findByArticle(601L)).thenReturn(m1);
        when(chapterMapper.findBySeries(77L)).thenReturn(List.of(m1, m2, m3));
        when(articleMapper.selectById(601L)).thenReturn(article(601L, PERSONAL_BLOG, "PUBLISHED"));
        when(articleMapper.selectById(602L)).thenReturn(article(602L, PERSONAL_BLOG, "DRAFT"));
        when(articleMapper.selectById(603L)).thenReturn(article(603L, PERSONAL_BLOG, "PUBLISHED"));
    }

    private static Blog blog(Long id, String type, String name, String slug) {
        Blog blog = new Blog();
        blog.setId(id); blog.setBlogType(type); blog.setName(name); blog.setSlug(slug);
        blog.setStatus("ACTIVE"); blog.setOwnerUserId(OWNER); blog.setLockVersion(0);
        return blog;
    }

    private static Team activeTeam(Long id, Long blogId) {
        Team team = new Team();
        team.setId(id); team.setBlogId(blogId); team.setOwnerUserId(OWNER); team.setStatus("ACTIVE");
        return team;
    }

    private static Series draftSeries(Long id, Long blogId) {
        Series series = new Series();
        series.setId(id); series.setBlogId(blogId); series.setCreatedByUserId(OWNER);
        series.setTitle("Spring Boot 实战"); series.setSlug("spring-boot");
        series.setSerializationStatus("ONGOING"); series.setReviewStatus("DRAFT"); series.setLockVersion(0);
        return series;
    }

    private static Article article(Long id, Long blogId, String publishStatus) {
        Article article = new Article();
        article.setId(id); article.setBlogId(blogId); article.setTitle("Chapter " + id);
        article.setSlug("chapter-" + id); article.setPublishStatus(publishStatus);
        return article;
    }
}

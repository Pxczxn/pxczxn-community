package top.pxczxn.community.series.application;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import top.pxczxn.community.article.model.Article;
import top.pxczxn.community.article.persistence.ArticleMapper;
import top.pxczxn.community.series.model.TeamSeries;
import top.pxczxn.community.series.model.TeamSeriesArticle;
import top.pxczxn.community.series.persistence.TeamSeriesArticleMapper;
import top.pxczxn.community.series.persistence.TeamSeriesMapper;
import top.pxczxn.community.team.application.TeamAuthorityService;
import top.pxczxn.community.team.model.Team;
import top.pxczxn.community.team.persistence.TeamAuditEventMapper;
import top.pxczxn.community.team.persistence.TeamMapper;
import top.pxczxn.platform.common.exception.BusinessException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class TeamSeriesServiceImplTest {
    private TeamSeriesMapper seriesMapper;
    private TeamSeriesArticleMapper chapterMapper;
    private TeamMapper teamMapper;
    private ArticleMapper articleMapper;
    private TeamAuthorityService authorityService;
    private TeamSeriesServiceImpl service;

    @BeforeEach
    void setUp() {
        seriesMapper = mock(TeamSeriesMapper.class); chapterMapper = mock(TeamSeriesArticleMapper.class);
        teamMapper = mock(TeamMapper.class); articleMapper = mock(ArticleMapper.class); authorityService = mock(TeamAuthorityService.class);
        service = new TeamSeriesServiceImpl(seriesMapper, chapterMapper, teamMapper, articleMapper, authorityService, mock(TeamAuditEventMapper.class));
    }

    @Test
    void createsDraftOnlyForTeamSeriesManagers() {
        when(teamMapper.selectById(20L)).thenReturn(activeTeam(20L, 200L));
        when(authorityService.hasPermission(3L, 20L, "MANAGE_SERIES")).thenReturn(true);
        when(seriesMapper.findByTeamAndSlug(20L, "release-notes")).thenReturn(null);
        when(seriesMapper.insert(any())).thenReturn(1);

        TeamSeriesView result = service.create(3L, new CreateTeamSeriesCommand(20L, "Release Notes", "release-notes", "summary", null, "ONGOING"));

        assertThat(result.reviewStatus()).isEqualTo("DRAFT");
        assertThat(result.teamId()).isEqualTo(20L);
        verify(seriesMapper).insert(any(TeamSeries.class));
    }

    @Test
    void rejectsTeamSeriesCreationWithoutResourcePermission() {
        when(teamMapper.selectById(20L)).thenReturn(activeTeam(20L, 200L));
        when(authorityService.hasPermission(3L, 20L, "MANAGE_SERIES")).thenReturn(false);

        assertThatThrownBy(() -> service.create(3L, new CreateTeamSeriesCommand(20L, "Release", "release", null, null, "ONGOING")))
                .isInstanceOf(BusinessException.class);
        verify(seriesMapper, never()).insert(any());
    }

    @Test
    void publicViewOmitsUnpublishedChapters() {
        TeamSeries series = new TeamSeries(); series.setId(70L); series.setTeamId(20L); series.setTitle("Public"); series.setSlug("public"); series.setReviewStatus("APPROVED"); series.setSerializationStatus("ONGOING"); series.setLockVersion(1);
        var chapter = new top.pxczxn.community.series.model.TeamSeriesArticle(); chapter.setArticleId(101L); chapter.setChapterOrder(1);
        Article article = new Article(); article.setId(101L); article.setTitle("Draft article"); article.setSlug("draft"); article.setPublishStatus("DRAFT");
        when(seriesMapper.findPublic()).thenReturn(List.of(series)); when(chapterMapper.findBySeries(70L)).thenReturn(List.of(chapter)); when(articleMapper.selectById(101L)).thenReturn(article);

        assertThat(service.publicSeries()).singleElement().satisfies(view -> assertThat(view.chapters()).isEmpty());
    }

    @Test
    void refusesReviewWhenAChapterIsNotPublished() {
        TeamSeries series = draftSeries(70L, 20L, 1); TeamSeriesArticle chapter = new TeamSeriesArticle(); chapter.setArticleId(101L);
        Article article = new Article(); article.setId(101L); article.setPublishStatus("DRAFT");
        when(seriesMapper.selectById(70L)).thenReturn(series); when(teamMapper.selectById(20L)).thenReturn(activeTeam(20L, 200L)); when(authorityService.hasPermission(3L, 20L, "MANAGE_SERIES")).thenReturn(true); when(chapterMapper.findBySeries(70L)).thenReturn(List.of(chapter)); when(articleMapper.selectById(101L)).thenReturn(article);

        assertThatThrownBy(() -> service.submitReview(3L, 70L, 1)).isInstanceOf(BusinessException.class);
        verify(seriesMapper, never()).submitReview(any(), any(), any());
    }

    private static Team activeTeam(Long id, Long blogId) { Team team = new Team(); team.setId(id); team.setBlogId(blogId); team.setStatus("ACTIVE"); return team; }
    private static TeamSeries draftSeries(Long id, Long teamId, Integer lock) { TeamSeries series = new TeamSeries(); series.setId(id); series.setTeamId(teamId); series.setReviewStatus("DRAFT"); series.setSerializationStatus("ONGOING"); series.setLockVersion(lock); return series; }
}

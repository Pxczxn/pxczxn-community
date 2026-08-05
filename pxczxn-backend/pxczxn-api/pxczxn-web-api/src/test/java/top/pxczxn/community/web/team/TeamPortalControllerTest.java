package top.pxczxn.community.web.team;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import top.pxczxn.community.shared.auth.CommunityAuth;
import top.pxczxn.community.team.application.MyTeamView;
import top.pxczxn.community.team.application.TeamActivityView;
import top.pxczxn.community.team.application.TeamArticleBriefView;
import top.pxczxn.community.team.application.TeamDashboardView;
import top.pxczxn.community.team.application.TeamPortalService;
import top.pxczxn.community.team.application.TeamStatsView;
import top.pxczxn.community.team.application.TeamTodoView;
import top.pxczxn.platform.common.result.Result;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TeamPortalControllerTest {

    private TeamPortalService portalService;
    private CommunityAuth communityAuth;
    private TeamPortalController controller;

    @BeforeEach
    void setUp() {
        portalService = mock(TeamPortalService.class);
        communityAuth = mock(CommunityAuth.class);
        when(communityAuth.getLoginUserId()).thenReturn(100L);
        controller = new TeamPortalController(portalService, communityAuth);
    }

    @Test
    void myTeamsReturnsCurrentUserMemberships() {
        when(portalService.myTeams(100L)).thenReturn(List.of(
                new MyTeamView(1L, 10L, "Team One", "team-one", "summary", null, null,
                        "ADMIN", List.of("OVERVIEW"), List.of("SUBMISSION_REVIEW"),
                        8, 32, 4, 1200, 3, 1, null, null)));

        Result<List<MyTeamView>> result = controller.myTeams();

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData()).singleElement().satisfies(view -> {
            assertThat(view.teamId()).isEqualTo(1L);
            assertThat(view.viewerRole()).isEqualTo("ADMIN");
            assertThat(view.pendingSubmissionCount()).isEqualTo(3);
        });
        verify(portalService).myTeams(100L);
    }

    @Test
    void dashboardReturnsOverviewForTeam() {
        when(portalService.dashboard(100L, 7L)).thenReturn(new TeamDashboardView(
                null, "EDITOR", List.of("OVERVIEW"), List.of("SUBMISSION_REVIEW"),
                new TeamStatsView(5, 2, 1, 3, 8, 120, 900, 60),
                List.of(), new TeamTodoView(2, 0, 1, 1, 0), List.of()));

        Result<TeamDashboardView> result = controller.dashboard(7L);

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData().viewerRole()).isEqualTo("EDITOR");
        assertThat(result.getData().stats().publishedArticleCount()).isEqualTo(5);
        assertThat(result.getData().todos().pendingSubmissionCount()).isEqualTo(2);
        verify(portalService).dashboard(100L, 7L);
    }

    @Test
    void activitiesPassesLimitAndDelegates() {
        when(portalService.activities(100L, 7L, 15)).thenReturn(List.of(
                new TeamActivityView(1L, 7L, "MEMBER_INVITED", "TEAM_INVITATION", 42L,
                        100L, "Alice", "alice", null, null)));

        Result<List<TeamActivityView>> result = controller.activities(7L, 15);

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData()).singleElement().extracting(TeamActivityView::eventType)
                .isEqualTo("MEMBER_INVITED");
        verify(portalService).activities(100L, 7L, 15);
    }

    @Test
    void articlesFiltersByPublishStatusAndDelegates() {
        when(portalService.teamArticles(100L, 7L, "DRAFT")).thenReturn(List.of());

        Result<List<TeamArticleBriefView>> result = controller.articles(7L, "DRAFT");

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData()).isEmpty();
        verify(portalService).teamArticles(100L, 7L, "DRAFT");
    }
}

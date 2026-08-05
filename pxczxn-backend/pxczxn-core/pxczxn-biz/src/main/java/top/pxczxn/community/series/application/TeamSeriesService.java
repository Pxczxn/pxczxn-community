package top.pxczxn.community.series.application;

import java.util.List;

public interface TeamSeriesService {
    TeamSeriesView create(Long actorUserId, CreateTeamSeriesCommand command);
    TeamSeriesView update(Long actorUserId, Long seriesId, UpdateTeamSeriesCommand command);
    TeamSeriesView replaceChapters(Long actorUserId, Long seriesId, List<Long> articleIds, Integer expectedLockVersion);
    TeamSeriesView submitReview(Long actorUserId, Long seriesId, Integer expectedLockVersion);
    List<TeamSeriesView> teamSeries(Long actorUserId, Long teamId);
    List<TeamSeriesChapterView> eligibleArticles(Long actorUserId, Long teamId);
    List<TeamSeriesView> publicSeries();
    TeamSeriesView publicSeries(Long seriesId);

    /** Approved series of one team, shown on the public team portal; no membership required. */
    List<TeamSeriesView> publicTeamSeries(Long teamId);
    List<TeamSeriesView> reviewQueue();
    TeamSeriesView approve(Long adminId, Long seriesId, SeriesReviewDecisionCommand command);
    TeamSeriesView reject(Long adminId, Long seriesId, SeriesReviewDecisionCommand command);
}

package top.pxczxn.community.series.application;

import top.pxczxn.community.series.model.TeamSeries;

import java.time.LocalDateTime;
import java.util.List;

public record TeamSeriesView(Long id, Long teamId, String title, String slug, String summary, Long coverFileId,
                             String serializationStatus, String reviewStatus, String reviewComment,
                             Integer lockVersion, LocalDateTime publishedAt, LocalDateTime updatedAt,
                             List<TeamSeriesChapterView> chapters) {
    public static TeamSeriesView from(TeamSeries series, List<TeamSeriesChapterView> chapters) {
        return new TeamSeriesView(series.getId(), series.getTeamId(), series.getTitle(), series.getSlug(),
                series.getSummary(), series.getCoverFileId(), series.getSerializationStatus(), series.getReviewStatus(),
                series.getReviewComment(), series.getLockVersion(), series.getPublishedAt(), series.getUpdatedAt(), chapters);
    }
}

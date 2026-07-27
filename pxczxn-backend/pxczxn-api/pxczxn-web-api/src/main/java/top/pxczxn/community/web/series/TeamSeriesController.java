package top.pxczxn.community.web.series;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import top.pxczxn.community.series.application.CreateTeamSeriesCommand;
import top.pxczxn.community.series.application.TeamSeriesService;
import top.pxczxn.community.series.application.TeamSeriesView;
import top.pxczxn.community.series.application.UpdateTeamSeriesCommand;
import top.pxczxn.community.shared.auth.CommunityAuth;
import top.pxczxn.platform.common.result.Result;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class TeamSeriesController {
    private final TeamSeriesService seriesService;
    private final CommunityAuth communityAuth;

    @GetMapping("/series")
    public Result<List<TeamSeriesView>> publicSeries() { return Result.ok(seriesService.publicSeries()); }

    @GetMapping("/series/{seriesId}")
    public Result<TeamSeriesView> publicDetail(@PathVariable Long seriesId) { return Result.ok(seriesService.publicSeries(seriesId)); }

    @GetMapping("/teams/{teamId}/series")
    public Result<List<TeamSeriesView>> teamSeries(@PathVariable Long teamId) { return Result.ok(seriesService.teamSeries(communityAuth.getLoginUserId(), teamId)); }

    @GetMapping("/teams/{teamId}/series/articles")
    public Result<List<top.pxczxn.community.series.application.TeamSeriesChapterView>> eligibleArticles(@PathVariable Long teamId) {
        return Result.ok(seriesService.eligibleArticles(communityAuth.getLoginUserId(), teamId));
    }

    @PostMapping("/teams/{teamId}/series")
    public Result<TeamSeriesView> create(@PathVariable Long teamId, @RequestBody SeriesRequest request) {
        return Result.ok(seriesService.create(communityAuth.getLoginUserId(), new CreateTeamSeriesCommand(teamId, request.title(), request.slug(), request.summary(), request.coverFileId(), request.serializationStatus())));
    }

    @PatchMapping("/series/{seriesId}")
    public Result<TeamSeriesView> update(@PathVariable Long seriesId, @RequestBody SeriesRequest request) {
        return Result.ok(seriesService.update(communityAuth.getLoginUserId(), seriesId, new UpdateTeamSeriesCommand(request.title(), request.slug(), request.summary(), request.coverFileId(), request.serializationStatus(), request.expectedLockVersion())));
    }

    @PostMapping("/series/{seriesId}/chapters")
    public Result<TeamSeriesView> chapters(@PathVariable Long seriesId, @RequestBody ChapterRequest request) {
        return Result.ok(seriesService.replaceChapters(communityAuth.getLoginUserId(), seriesId, request.articleIds(), request.expectedLockVersion()));
    }

    @PostMapping("/series/{seriesId}/submit-review")
    public Result<TeamSeriesView> submit(@PathVariable Long seriesId, @RequestBody ReviewRequest request) {
        return Result.ok(seriesService.submitReview(communityAuth.getLoginUserId(), seriesId, request.expectedLockVersion()));
    }

    public record SeriesRequest(String title, String slug, String summary, Long coverFileId, String serializationStatus, Integer expectedLockVersion) { }
    public record ChapterRequest(List<Long> articleIds, Integer expectedLockVersion) { }
    public record ReviewRequest(Integer expectedLockVersion) { }
}

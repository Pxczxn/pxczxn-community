package top.pxczxn.community.admin.series;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.stp.StpUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import top.pxczxn.community.series.application.SeriesReviewDecisionCommand;
import top.pxczxn.community.series.application.TeamSeriesService;
import top.pxczxn.community.series.application.TeamSeriesView;
import top.pxczxn.platform.common.result.Result;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin-api/community/series")
public class AdminTeamSeriesController {
    private final TeamSeriesService seriesService;

    @GetMapping
    @SaCheckPermission("community:series:review")
    public Result<List<TeamSeriesView>> queue() { return Result.ok(seriesService.reviewQueue()); }

    @PostMapping("/{seriesId}/approve")
    @SaCheckPermission("community:series:review")
    public Result<TeamSeriesView> approve(@PathVariable Long seriesId, @RequestBody DecisionRequest request) {
        return Result.ok(seriesService.approve(StpUtil.getLoginIdAsLong(), seriesId, new SeriesReviewDecisionCommand(request.expectedLockVersion(), request.comment())));
    }

    @PostMapping("/{seriesId}/reject")
    @SaCheckPermission("community:series:review")
    public Result<TeamSeriesView> reject(@PathVariable Long seriesId, @RequestBody DecisionRequest request) {
        return Result.ok(seriesService.reject(StpUtil.getLoginIdAsLong(), seriesId, new SeriesReviewDecisionCommand(request.expectedLockVersion(), request.comment())));
    }

    public record DecisionRequest(Integer expectedLockVersion, String comment) { }
}

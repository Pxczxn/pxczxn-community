package top.pxczxn.community.web.series;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import top.pxczxn.community.series.application.CreateSeriesCommand;
import top.pxczxn.community.series.application.SeriesChapterView;
import top.pxczxn.community.series.application.SeriesReaderService;
import top.pxczxn.community.series.application.SeriesReaderStateView;
import top.pxczxn.community.series.application.ArticleSeriesContextView;
import top.pxczxn.community.series.application.SeriesService;
import top.pxczxn.community.series.application.SeriesView;
import top.pxczxn.community.series.application.UpdateSeriesCommand;
import top.pxczxn.community.shared.auth.CommunityAuth;
import top.pxczxn.community.team.model.Team;
import top.pxczxn.community.team.persistence.TeamMapper;
import top.pxczxn.platform.common.exception.BusinessException;
import top.pxczxn.platform.common.result.Result;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class SeriesController {
    private final SeriesService seriesService;
    private final SeriesReaderService seriesReaderService;
    private final TeamMapper teamMapper;
    private final CommunityAuth communityAuth;

    @GetMapping("/series")
    public Result<List<SeriesView>> publicSeries() { return Result.ok(seriesService.publicSeries(viewerUserId())); }

    @GetMapping("/series/{seriesId}")
    public Result<SeriesView> publicDetail(@PathVariable Long seriesId) {
        return Result.ok(seriesService.publicSeries(seriesId, viewerUserId()));
    }

    /** Approved series of one blog, for its public portal; no login required. */
    @GetMapping("/public/blogs/{blogId}/series")
    public Result<List<SeriesView>> publicBlogSeries(@PathVariable Long blogId) {
        return Result.ok(seriesService.publicBlogSeries(blogId, viewerUserId()));
    }

    // --- 读者侧：追更与阅读进度 ---

    /** Series the signed-in reader follows. */
    @GetMapping("/me/series/following")
    public Result<List<SeriesView>> myFollowedSeries() {
        return Result.ok(seriesService.followedSeries(communityAuth.getLoginUserId()));
    }

    /** "Continue reading" shelf for the signed-in reader. */
    @GetMapping("/me/series/reading")
    public Result<List<SeriesView>> myReadingSeries(@RequestParam(defaultValue = "10") int limit) {
        return Result.ok(seriesService.recentlyReadSeries(communityAuth.getLoginUserId(), limit));
    }

    @GetMapping("/series/{seriesId}/reader-state")
    public Result<SeriesReaderStateView> readerState(@PathVariable Long seriesId) {
        return Result.ok(seriesReaderService.state(communityAuth.getLoginUserId(), seriesId));
    }

    /**
     * Series context for one article: which public series it belongs to, the ordered published
     * chapters (previous / next navigation), and the viewer's follow / progress state. Returns
     * {@code null} data when the article is not part of any publicly visible series.
     */
    @GetMapping("/public/articles/{articleId}/series")
    public Result<ArticleSeriesContextView> articleSeries(@PathVariable Long articleId) {
        return Result.ok(seriesService.articleSeriesContext(articleId, viewerUserId()));
    }

    @PostMapping("/series/{seriesId}/follow")
    public Result<SeriesReaderStateView> followSeries(@PathVariable Long seriesId) {
        return Result.ok(seriesReaderService.follow(communityAuth.getLoginUserId(), seriesId));
    }

    @DeleteMapping("/series/{seriesId}/follow")
    public Result<SeriesReaderStateView> unfollowSeries(@PathVariable Long seriesId) {
        return Result.ok(seriesReaderService.unfollow(communityAuth.getLoginUserId(), seriesId));
    }

    /** Called when a reader opens a chapter; keeps the "continue reading" pointer fresh. */
    @PostMapping("/series/{seriesId}/progress")
    public Result<SeriesReaderStateView> recordProgress(@PathVariable Long seriesId, @RequestBody ProgressRequest request) {
        return Result.ok(seriesReaderService.recordProgress(communityAuth.getLoginUserId(), seriesId,
                request == null ? null : request.articleId()));
    }

    /** Manage list for a blog (drafts / review / published). */
    @GetMapping("/blogs/{blogId}/series/manage")
    public Result<List<SeriesView>> manage(@PathVariable Long blogId) {
        return Result.ok(seriesService.blogSeries(communityAuth.getLoginUserId(), blogId));
    }

    @GetMapping("/blogs/{blogId}/series/articles")
    public Result<List<SeriesChapterView>> eligibleArticles(@PathVariable Long blogId) {
        return Result.ok(seriesService.eligibleArticles(communityAuth.getLoginUserId(), blogId));
    }

    @PostMapping("/blogs/{blogId}/series")
    public Result<SeriesView> create(@PathVariable Long blogId, @RequestBody SeriesRequest request) {
        return Result.ok(seriesService.create(communityAuth.getLoginUserId(),
                new CreateSeriesCommand(blogId, request.title(), request.slug(), request.summary(), request.coverFileId(), request.serializationStatus())));
    }

    @PatchMapping("/series/{seriesId}")
    public Result<SeriesView> update(@PathVariable Long seriesId, @RequestBody SeriesRequest request) {
        return Result.ok(seriesService.update(communityAuth.getLoginUserId(), seriesId,
                new UpdateSeriesCommand(request.title(), request.slug(), request.summary(), request.coverFileId(), request.serializationStatus(), request.expectedLockVersion())));
    }

    @PostMapping("/series/{seriesId}/chapters")
    public Result<SeriesView> chapters(@PathVariable Long seriesId, @RequestBody ChapterRequest request) {
        return Result.ok(seriesService.replaceChapters(communityAuth.getLoginUserId(), seriesId, request.articleIds(), request.expectedLockVersion()));
    }

    @PostMapping("/series/{seriesId}/submit-review")
    public Result<SeriesView> submit(@PathVariable Long seriesId, @RequestBody ReviewRequest request) {
        return Result.ok(seriesService.submitReview(communityAuth.getLoginUserId(), seriesId, request.expectedLockVersion()));
    }

    // --- 兼容端点：旧 /teams/{teamId}/series 仍可用，内部按 team -> blog 转调 Blog Series ---
    @GetMapping("/public/teams/{teamId}/series")
    public Result<List<SeriesView>> publicTeamSeries(@PathVariable Long teamId) {
        return Result.ok(seriesService.publicBlogSeries(resolveTeamBlogId(teamId), viewerUserId()));
    }

    @GetMapping("/teams/{teamId}/series")
    public Result<List<SeriesView>> teamSeries(@PathVariable Long teamId) {
        return Result.ok(seriesService.blogSeries(communityAuth.getLoginUserId(), resolveTeamBlogId(teamId)));
    }

    @GetMapping("/teams/{teamId}/series/articles")
    public Result<List<SeriesChapterView>> teamEligibleArticles(@PathVariable Long teamId) {
        return Result.ok(seriesService.eligibleArticles(communityAuth.getLoginUserId(), resolveTeamBlogId(teamId)));
    }

    @PostMapping("/teams/{teamId}/series")
    public Result<SeriesView> createForTeam(@PathVariable Long teamId, @RequestBody SeriesRequest request) {
        return Result.ok(seriesService.create(communityAuth.getLoginUserId(),
                new CreateSeriesCommand(resolveTeamBlogId(teamId), request.title(), request.slug(), request.summary(), request.coverFileId(), request.serializationStatus())));
    }

    private Long resolveTeamBlogId(Long teamId) {
        Team team = teamMapper.selectById(teamId);
        if (team == null) throw new BusinessException(404, "团队不存在");
        return team.getBlogId();
    }

    /** Public endpoints stay readable for guests; a logged-in viewer only enriches the payload. */
    private Long viewerUserId() { return communityAuth.getOptionalLoginUserId(); }

    public record SeriesRequest(String title, String slug, String summary, Long coverFileId, String serializationStatus, Integer expectedLockVersion) { }
    public record ChapterRequest(List<Long> articleIds, Integer expectedLockVersion) { }
    public record ReviewRequest(Integer expectedLockVersion) { }
    public record ProgressRequest(Long articleId) { }
}

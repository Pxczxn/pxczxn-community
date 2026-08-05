package top.pxczxn.community.series.application;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.pxczxn.community.article.model.Article;
import top.pxczxn.community.article.persistence.ArticleMapper;
import top.pxczxn.community.series.model.TeamSeries;
import top.pxczxn.community.series.model.TeamSeriesArticle;
import top.pxczxn.community.series.persistence.TeamSeriesArticleMapper;
import top.pxczxn.community.series.persistence.TeamSeriesMapper;
import top.pxczxn.community.team.application.TeamAuthorityService;
import top.pxczxn.community.team.model.Team;
import top.pxczxn.community.team.model.TeamAuditEvent;
import top.pxczxn.community.team.persistence.TeamAuditEventMapper;
import top.pxczxn.community.team.persistence.TeamMapper;
import top.pxczxn.platform.common.exception.BusinessException;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class TeamSeriesServiceImpl implements TeamSeriesService {
    private final TeamSeriesMapper seriesMapper;
    private final TeamSeriesArticleMapper chapterMapper;
    private final TeamMapper teamMapper;
    private final ArticleMapper articleMapper;
    private final TeamAuthorityService authorityService;
    private final TeamAuditEventMapper auditMapper;

    @Override
    @Transactional
    public TeamSeriesView create(Long actorUserId, CreateTeamSeriesCommand command) {
        if (command == null || command.teamId() == null) throw bad("系列信息不完整");
        requireManager(actorUserId, command.teamId());
        String title = required(command.title(), "系列标题", 160);
        String slug = slug(command.slug(), title);
        if (seriesMapper.findByTeamAndSlug(command.teamId(), slug) != null) throw new BusinessException(409, "该团队已使用此系列标识");
        LocalDateTime now = now();
        TeamSeries series = new TeamSeries();
        series.setId(IdWorker.getId()); series.setTeamId(command.teamId()); series.setCreatedByUserId(actorUserId);
        series.setTitle(title); series.setSlug(slug); series.setSummary(optional(command.summary(), 1000)); series.setCoverFileId(command.coverFileId());
        series.setSerializationStatus(serialization(command.serializationStatus())); series.setReviewStatus("DRAFT");
        series.setLockVersion(0); series.setCreatedAt(now); series.setUpdatedAt(now);
        try { if (seriesMapper.insert(series) != 1) throw new BusinessException(500, "创建系列失败"); }
        catch (DuplicateKeyException ex) { throw new BusinessException(409, "该团队已使用此系列标识"); }
        audit(series, actorUserId, "SERIES_CREATED", null, "DRAFT");
        return view(series, false);
    }

    @Override
    @Transactional
    public TeamSeriesView update(Long actorUserId, Long seriesId, UpdateTeamSeriesCommand command) {
        TeamSeries series = editable(actorUserId, seriesId, command == null ? null : command.expectedLockVersion());
        String title = required(command.title(), "系列标题", 160); String nextSlug = slug(command.slug(), title);
        TeamSeries same = seriesMapper.findByTeamAndSlug(series.getTeamId(), nextSlug);
        if (same != null && !Objects.equals(same.getId(), series.getId())) throw new BusinessException(409, "该团队已使用此系列标识");
        LocalDateTime now = now();
        if (seriesMapper.updateDraft(series.getId(), series.getLockVersion(), title, nextSlug, optional(command.summary(), 1000), command.coverFileId(), serialization(command.serializationStatus()), now) != 1) throw collision();
        audit(series, actorUserId, "SERIES_UPDATED", series.getReviewStatus(), series.getReviewStatus());
        series.setTitle(title); series.setSlug(nextSlug); series.setSummary(optional(command.summary(), 1000)); series.setCoverFileId(command.coverFileId()); series.setSerializationStatus(serialization(command.serializationStatus())); series.setUpdatedAt(now); series.setLockVersion(series.getLockVersion() + 1);
        return view(series, false);
    }

    @Override
    @Transactional
    public TeamSeriesView replaceChapters(Long actorUserId, Long seriesId, List<Long> articleIds, Integer expectedLockVersion) {
        TeamSeries series = editable(actorUserId, seriesId, expectedLockVersion);
        List<Long> ids = articleIds == null ? List.of() : articleIds;
        if (new HashSet<>(ids).size() != ids.size() || ids.stream().anyMatch(Objects::isNull)) throw bad("章节列表包含重复或无效文章");
        Team team = activeTeam(series.getTeamId());
        for (Long articleId : ids) {
            Article article = articleMapper.selectById(articleId);
            if (article == null || article.getDeletedAt() != null || !Objects.equals(article.getBlogId(), team.getBlogId())) throw new BusinessException(403, "只能编排本团队的有效文章");
            TeamSeriesArticle membership = chapterMapper.findByArticle(articleId);
            if (membership != null && !Objects.equals(membership.getSeriesId(), series.getId())) throw new BusinessException(409, "文章已属于另一个系列");
        }
        LocalDateTime now = now();
        if (seriesMapper.touchDraft(series.getId(), series.getLockVersion(), now) != 1) throw collision();
        chapterMapper.deleteBySeries(series.getId());
        for (int index = 0; index < ids.size(); index++) {
            TeamSeriesArticle chapter = new TeamSeriesArticle(); chapter.setId(IdWorker.getId()); chapter.setSeriesId(series.getId()); chapter.setArticleId(ids.get(index)); chapter.setChapterOrder(index + 1); chapter.setAddedByUserId(actorUserId); chapter.setCreatedAt(now);
            if (chapterMapper.insert(chapter) != 1) throw new BusinessException(500, "保存章节失败");
        }
        audit(series, actorUserId, "SERIES_CHAPTERS_REORDERED", series.getReviewStatus(), series.getReviewStatus());
        series.setLockVersion(series.getLockVersion() + 1); series.setUpdatedAt(now);
        return view(series, false);
    }

    @Override
    @Transactional
    public TeamSeriesView submitReview(Long actorUserId, Long seriesId, Integer expectedLockVersion) {
        TeamSeries series = editable(actorUserId, seriesId, expectedLockVersion);
        List<TeamSeriesArticle> chapters = chapterMapper.findBySeries(seriesId);
        if (chapters.isEmpty()) throw new BusinessException(409, "系列至少需要一篇文章才能提交审核");
        for (TeamSeriesArticle chapter : chapters) {
            Article article = articleMapper.selectById(chapter.getArticleId());
            if (article == null || article.getDeletedAt() != null || !"PUBLISHED".equals(article.getPublishStatus())) {
                throw new BusinessException(409, "提交审核前，所有章节必须已经发布");
            }
        }
        LocalDateTime now = now();
        if (seriesMapper.submitReview(seriesId, series.getLockVersion(), now) != 1) throw collision();
        audit(series, actorUserId, "SERIES_SUBMITTED_FOR_REVIEW", series.getReviewStatus(), "PENDING_REVIEW");
        series.setReviewStatus("PENDING_REVIEW"); series.setLockVersion(series.getLockVersion() + 1); series.setUpdatedAt(now);
        return view(series, false);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TeamSeriesView> teamSeries(Long actorUserId, Long teamId) {
        requireManager(actorUserId, teamId);
        return seriesMapper.findByTeam(teamId).stream().map(series -> view(series, false)).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TeamSeriesChapterView> eligibleArticles(Long actorUserId, Long teamId) {
        requireManager(actorUserId, teamId);
        Team team = activeTeam(teamId);
        return articleMapper.selectList(Wrappers.<Article>lambdaQuery().eq(Article::getBlogId, team.getBlogId()).isNull(Article::getDeletedAt).orderByDesc(Article::getUpdatedAt))
                .stream().map(article -> new TeamSeriesChapterView(article.getId(), article.getTitle(), article.getSlug(), article.getPublishStatus(), 0)).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TeamSeriesView> publicSeries() { return seriesMapper.findPublic().stream().map(series -> view(series, true)).toList(); }

    @Override
    @Transactional(readOnly = true)
    public TeamSeriesView publicSeries(Long seriesId) {
        TeamSeries series = seriesMapper.findPublicById(seriesId);
        if (series == null) throw new BusinessException(404, "系列不存在或尚未公开");
        return view(series, true);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TeamSeriesView> publicTeamSeries(Long teamId) {
        activeTeam(teamId);
        return seriesMapper.findPublicByTeam(teamId).stream().map(series -> view(series, true)).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TeamSeriesView> reviewQueue() { return seriesMapper.findReviewQueue().stream().map(series -> view(series, false)).toList(); }

    @Override
    @Transactional
    public TeamSeriesView approve(Long adminId, Long seriesId, SeriesReviewDecisionCommand command) { return decide(adminId, seriesId, command, "APPROVED"); }

    @Override
    @Transactional
    public TeamSeriesView reject(Long adminId, Long seriesId, SeriesReviewDecisionCommand command) { return decide(adminId, seriesId, command, "REJECTED"); }

    private TeamSeriesView decide(Long adminId, Long seriesId, SeriesReviewDecisionCommand command, String status) {
        TeamSeries series = require(seriesId); requireLock(command == null ? null : command.expectedLockVersion(), series);
        LocalDateTime now = now(); String comment = optional(command.comment(), 1000);
        if (seriesMapper.decideReview(seriesId, series.getLockVersion(), status, adminId, comment, now) != 1) throw collision();
        audit(series, null, "SERIES_PLATFORM_" + status, series.getReviewStatus(), status);
        series.setReviewStatus(status); series.setReviewerAdminId(adminId); series.setReviewComment(comment); series.setReviewedAt(now); series.setPublishedAt("APPROVED".equals(status) ? now : series.getPublishedAt()); series.setUpdatedAt(now); series.setLockVersion(series.getLockVersion() + 1);
        return view(series, false);
    }

    private TeamSeries editable(Long actorUserId, Long id, Integer expectedLockVersion) { TeamSeries series = require(id); requireLock(expectedLockVersion, series); requireManager(actorUserId, series.getTeamId()); return series; }
    private TeamSeries require(Long id) { TeamSeries series = seriesMapper.selectById(id); if (series == null || series.getDeletedAt() != null) throw new BusinessException(404, "系列不存在"); return series; }
    private Team activeTeam(Long id) { Team team = teamMapper.selectById(id); if (team == null || team.getDeletedAt() != null || !"ACTIVE".equals(team.getStatus())) throw new BusinessException(404, "团队不存在或不可用"); return team; }
    private void requireManager(Long userId, Long teamId) { activeTeam(teamId); if (!authorityService.hasPermission(userId, teamId, "MANAGE_SERIES")) throw new BusinessException(403, "没有管理该团队系列的权限"); }
    private TeamSeriesView view(TeamSeries series, boolean publicOnly) {
        List<TeamSeriesChapterView> chapters = new ArrayList<>();
        for (TeamSeriesArticle chapter : chapterMapper.findBySeries(series.getId())) {
            Article article = articleMapper.selectById(chapter.getArticleId());
            if (article == null || article.getDeletedAt() != null || (publicOnly && !"PUBLISHED".equals(article.getPublishStatus()))) continue;
            chapters.add(new TeamSeriesChapterView(article.getId(), article.getTitle(), article.getSlug(), article.getPublishStatus(), chapter.getChapterOrder()));
        }
        return TeamSeriesView.from(series, chapters);
    }
    private void audit(TeamSeries series, Long actorUserId, String eventType, String before, String after) { TeamAuditEvent event = new TeamAuditEvent(); event.setTeamId(series.getTeamId()); event.setActorUserId(actorUserId); event.setEventType(eventType); event.setTargetType("TEAM_SERIES"); event.setTargetId(series.getId()); event.setBeforeSnapshot("{\"status\":\"" + before + "\"}"); event.setAfterSnapshot("{\"status\":\"" + after + "\"}"); event.setOccurredAt(now()); auditMapper.insert(event); }
    private static String required(String value, String label, int max) { String text = optional(value, max); if (text == null) throw bad(label + "不能为空"); return text; }
    private static String optional(String value, int max) { if (value == null) return null; String text = value.trim(); return text.isEmpty() ? null : text.substring(0, Math.min(max, text.length())); }
    private static String slug(String requested, String title) { String value = optional(requested, 160); if (value == null) value = title; value = value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9-]+", "-").replaceAll("^-+|-+$", ""); if (value.isBlank() || value.length() > 160) throw bad("系列标识格式无效"); return value; }
    private static String serialization(String value) { String status = value == null ? "ONGOING" : value.trim().toUpperCase(Locale.ROOT); if (!Set.of("ONGOING", "COMPLETED", "PAUSED").contains(status)) throw bad("连载状态无效"); return status; }
    private static void requireLock(Integer expected, TeamSeries series) { if (expected == null || !Objects.equals(expected, series.getLockVersion())) throw collision(); }
    private static LocalDateTime now() { return LocalDateTime.now(ZoneOffset.UTC); }
    private static BusinessException bad(String message) { return new BusinessException(400, message); }
    private static BusinessException collision() { return new BusinessException(409, "系列状态已变化，请刷新后重试"); }
}

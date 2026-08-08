package top.pxczxn.community.series.application;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
import top.pxczxn.community.social.persistence.CommunityFollowMapper;
import top.pxczxn.community.team.application.TeamAuthorityService;
import top.pxczxn.community.team.model.Team;
import top.pxczxn.community.team.model.TeamAuditEvent;
import top.pxczxn.community.team.persistence.TeamAuditEventMapper;
import top.pxczxn.community.team.persistence.TeamMapper;
import top.pxczxn.community.user.model.CommunityUser;
import top.pxczxn.community.user.persistence.CommunityUserMapper;
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
public class SeriesServiceImpl implements SeriesService {
    static final String FOLLOW_TARGET_SERIES = "SERIES";

    private final SeriesMapper seriesMapper;
    private final SeriesArticleMapper chapterMapper;
    private final BlogMapper blogMapper;
    private final ArticleMapper articleMapper;
    private final CommunityUserMapper userMapper;
    private final TeamAuthorityService authorityService;
    private final TeamAuditEventMapper auditMapper;
    private final TeamMapper teamMapper;
    private final CommunityFollowMapper followMapper;
    private final SeriesReadingProgressMapper progressMapper;

    @Override
    @Transactional
    public SeriesView create(Long actorUserId, CreateSeriesCommand command) {
        if (command == null || command.blogId() == null) throw bad("系列信息不完整");
        requireManagePermission(actorUserId, command.blogId());
        String title = required(command.title(), "系列标题", 160);
        long seriesId = IdWorker.getId();
        String slug = slug(command.slug(), title, seriesId);
        if (seriesMapper.findByBlogAndSlug(command.blogId(), slug) != null) throw new BusinessException(409, "该博客已使用此系列标识");
        LocalDateTime now = now();
        Series series = new Series();
        series.setId(seriesId); series.setBlogId(command.blogId()); series.setCreatedByUserId(actorUserId);
        series.setTitle(title); series.setSlug(slug); series.setSummary(optional(command.summary(), 1000)); series.setCoverFileId(command.coverFileId());
        series.setSerializationStatus(serialization(command.serializationStatus())); series.setReviewStatus("DRAFT");
        series.setLockVersion(0); series.setCreatedAt(now); series.setUpdatedAt(now);
        try { if (seriesMapper.insert(series) != 1) throw new BusinessException(500, "创建系列失败"); }
        catch (DuplicateKeyException ex) { throw new BusinessException(409, "该博客已使用此系列标识"); }
        audit(series, actorUserId, "SERIES_CREATED", null, "DRAFT");
        return view(series, false, actorUserId);
    }

    @Override
    @Transactional
    public SeriesView update(Long actorUserId, Long seriesId, UpdateSeriesCommand command) {
        Series series = editable(actorUserId, seriesId, command == null ? null : command.expectedLockVersion());
        String title = required(command.title(), "系列标题", 160); String nextSlug = slug(command.slug(), title, series.getId());
        Series same = seriesMapper.findByBlogAndSlug(series.getBlogId(), nextSlug);
        if (same != null && !Objects.equals(same.getId(), series.getId())) throw new BusinessException(409, "该博客已使用此系列标识");
        LocalDateTime now = now();
        if (seriesMapper.updateDraft(series.getId(), series.getLockVersion(), title, nextSlug, optional(command.summary(), 1000), command.coverFileId(), serialization(command.serializationStatus()), now) != 1) throw collision();
        audit(series, actorUserId, "SERIES_UPDATED", series.getReviewStatus(), series.getReviewStatus());
        series.setTitle(title); series.setSlug(nextSlug); series.setSummary(optional(command.summary(), 1000)); series.setCoverFileId(command.coverFileId()); series.setSerializationStatus(serialization(command.serializationStatus())); series.setUpdatedAt(now); series.setLockVersion(series.getLockVersion() + 1);
        return view(series, false, actorUserId);
    }

    @Override
    @Transactional
    public SeriesView replaceChapters(Long actorUserId, Long seriesId, List<Long> articleIds, Integer expectedLockVersion) {
        Series series = editable(actorUserId, seriesId, expectedLockVersion);
        List<Long> ids = articleIds == null ? List.of() : articleIds;
        if (new HashSet<>(ids).size() != ids.size() || ids.stream().anyMatch(Objects::isNull)) throw bad("章节列表包含重复或无效文章");
        for (Long articleId : ids) {
            Article article = articleMapper.selectById(articleId);
            if (article == null || article.getDeletedAt() != null || !Objects.equals(article.getBlogId(), series.getBlogId())) throw new BusinessException(403, "只能编排本博客的有效文章");
            SeriesArticle membership = chapterMapper.findByArticle(articleId);
            if (membership != null && !Objects.equals(membership.getSeriesId(), series.getId())) throw new BusinessException(409, "文章已属于另一个系列");
        }
        LocalDateTime now = now();
        if (seriesMapper.touchDraft(series.getId(), series.getLockVersion(), now) != 1) throw collision();
        chapterMapper.deleteBySeries(series.getId());
        for (int index = 0; index < ids.size(); index++) {
            SeriesArticle chapter = new SeriesArticle(); chapter.setId(IdWorker.getId()); chapter.setSeriesId(series.getId()); chapter.setBlogId(series.getBlogId()); chapter.setArticleId(ids.get(index)); chapter.setChapterOrder(index + 1); chapter.setAddedByUserId(actorUserId); chapter.setCreatedAt(now);
            if (chapterMapper.insert(chapter) != 1) throw new BusinessException(500, "保存章节失败");
        }
        audit(series, actorUserId, "SERIES_CHAPTERS_REORDERED", series.getReviewStatus(), series.getReviewStatus());
        series.setLockVersion(series.getLockVersion() + 1); series.setUpdatedAt(now);
        return view(series, false, actorUserId);
    }

    @Override
    @Transactional
    public SeriesView submitReview(Long actorUserId, Long seriesId, Integer expectedLockVersion) {
        Series series = editable(actorUserId, seriesId, expectedLockVersion);
        List<SeriesArticle> chapters = chapterMapper.findBySeries(seriesId);
        if (chapters.isEmpty()) throw new BusinessException(409, "系列至少需要一篇文章才能提交审核");
        for (SeriesArticle chapter : chapters) {
            Article article = articleMapper.selectById(chapter.getArticleId());
            if (article == null || article.getDeletedAt() != null || !"PUBLISHED".equals(article.getPublishStatus())) {
                throw new BusinessException(409, "提交审核前，所有章节必须已经发布");
            }
        }
        LocalDateTime now = now();
        if (seriesMapper.submitReview(seriesId, series.getLockVersion(), now) != 1) throw collision();
        audit(series, actorUserId, "SERIES_SUBMITTED_FOR_REVIEW", series.getReviewStatus(), "PENDING_REVIEW");
        series.setReviewStatus("PENDING_REVIEW"); series.setLockVersion(series.getLockVersion() + 1); series.setUpdatedAt(now);
        return view(series, false, actorUserId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SeriesView> blogSeries(Long actorUserId, Long blogId) {
        requireManagePermission(actorUserId, blogId);
        return seriesMapper.findByBlog(blogId).stream().map(series -> view(series, false, actorUserId)).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<SeriesChapterView> eligibleArticles(Long actorUserId, Long blogId) {
        requireManagePermission(actorUserId, blogId);
        return articleMapper.selectList(Wrappers.<Article>lambdaQuery().eq(Article::getBlogId, blogId).isNull(Article::getDeletedAt).orderByDesc(Article::getUpdatedAt))
                .stream().map(article -> new SeriesChapterView(article.getId(), article.getTitle(), article.getSlug(), article.getPublishStatus(), 0)).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<SeriesView> publicSeries(Long viewerUserId) {
        return seriesMapper.findPublic().stream().map(series -> view(series, true, viewerUserId)).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public SeriesView publicSeries(Long seriesId, Long viewerUserId) {
        Series series = seriesMapper.findPublicById(seriesId);
        if (series == null) throw new BusinessException(404, "系列不存在或尚未公开");
        return view(series, true, viewerUserId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SeriesView> publicBlogSeries(Long blogId, Long viewerUserId) {
        if (blogMapper.selectById(blogId) == null) throw new BusinessException(404, "博客不存在");
        return seriesMapper.findPublicByBlog(blogId).stream().map(series -> view(series, true, viewerUserId)).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ArticleSeriesContextView articleSeriesContext(Long articleId, Long viewerUserId) {
        SeriesArticle membership = chapterMapper.findByArticle(articleId);
        if (membership == null) return null;
        Series series = seriesMapper.findPublicById(membership.getSeriesId());
        if (series == null) return null;
        List<SeriesChapterView> chapters = new ArrayList<>();
        for (SeriesArticle chapter : chapterMapper.findBySeries(series.getId())) {
            Article article = articleMapper.selectById(chapter.getArticleId());
            if (article == null || article.getDeletedAt() != null || !"PUBLISHED".equals(article.getPublishStatus())) continue;
            chapters.add(new SeriesChapterView(article.getId(), article.getTitle(), article.getSlug(), article.getPublishStatus(), chapter.getChapterOrder()));
        }
        chapters.sort(java.util.Comparator.comparingInt(SeriesChapterView::chapterOrder));
        long followerCount = followMapper.countFollowers(FOLLOW_TARGET_SERIES, series.getId());
        boolean following = viewerUserId != null
                && followMapper.findRelation(viewerUserId, FOLLOW_TARGET_SERIES, series.getId()) != null;
        SeriesReadingProgress progress = viewerUserId == null
                ? null
                : progressMapper.findByUserAndSeries(viewerUserId, series.getId());
        return new ArticleSeriesContextView(
                series.getId(), series.getSlug(), series.getTitle(), membership.getChapterOrder(),
                followerCount, following,
                progress == null ? null : progress.getLastArticleId(),
                progress == null ? 0 : intValue(progress.getMaxChapterOrder()),
                chapters);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SeriesView> followedSeries(Long viewerUserId) {
        if (viewerUserId == null) throw new BusinessException(401, "请先登录");
        return seriesMapper.findFollowedByUser(viewerUserId).stream().map(series -> view(series, true, viewerUserId)).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<SeriesView> recentlyReadSeries(Long viewerUserId, int limit) {
        if (viewerUserId == null) throw new BusinessException(401, "请先登录");
        if (limit < 1 || limit > 50) throw bad("数量须为 1-50");
        return seriesMapper.findRecentlyReadByUser(viewerUserId, limit).stream().map(series -> view(series, true, viewerUserId)).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<SeriesView> reviewQueue() { return seriesMapper.findReviewQueue().stream().map(series -> view(series, false, null)).toList(); }

    @Override
    @Transactional
    public SeriesView approve(Long adminId, Long seriesId, SeriesReviewDecisionCommand command) { return decide(adminId, seriesId, command, "APPROVED"); }

    @Override
    @Transactional
    public SeriesView reject(Long adminId, Long seriesId, SeriesReviewDecisionCommand command) { return decide(adminId, seriesId, command, "REJECTED"); }

    private SeriesView decide(Long adminId, Long seriesId, SeriesReviewDecisionCommand command, String status) {
        Series series = require(seriesId); requireLock(command == null ? null : command.expectedLockVersion(), series);
        LocalDateTime now = now(); String comment = optional(command.comment(), 1000);
        if (seriesMapper.decideReview(seriesId, series.getLockVersion(), status, adminId, comment, now) != 1) throw collision();
        audit(series, null, "SERIES_PLATFORM_" + status, series.getReviewStatus(), status);
        series.setReviewStatus(status); series.setReviewerAdminId(adminId); series.setReviewComment(comment); series.setReviewedAt(now); series.setPublishedAt("APPROVED".equals(status) ? now : series.getPublishedAt()); series.setUpdatedAt(now); series.setLockVersion(series.getLockVersion() + 1);
        return view(series, false, null);
    }

    private Series editable(Long actorUserId, Long id, Integer expectedLockVersion) { Series series = require(id); requireLock(expectedLockVersion, series); requireManagePermission(actorUserId, series.getBlogId()); return series; }
    private Series require(Long id) { Series series = seriesMapper.selectById(id); if (series == null || series.getDeletedAt() != null) throw new BusinessException(404, "系列不存在"); return series; }

    /** Unified series manage permission: PERSONAL blog -> owner; TEAM blog -> MANAGE_SERIES authority. */
    private void requireManagePermission(Long userId, Long blogId) {
        Blog blog = blogMapper.selectById(blogId);
        if (blog == null || blog.getDeletedAt() != null) throw new BusinessException(404, "博客不存在");
        if ("PERSONAL".equals(blog.getBlogType())) {
            if (!Objects.equals(blog.getOwnerUserId(), userId)) throw new BusinessException(403, "没有管理该系列权限");
            return;
        }
        Team team = teamMapper.findByBlogId(blogId);
        if (team == null || team.getDeletedAt() != null || !"ACTIVE".equals(team.getStatus())) throw new BusinessException(404, "团队不存在或不可用");
        if (!authorityService.hasPermission(userId, team.getId(), "MANAGE_SERIES")) throw new BusinessException(403, "没有管理该系列权限");
    }

    /** Builds a view; {@code viewerUserId} may be null for anonymous visitors or admin queues. */
    private SeriesView view(Series series, boolean publicOnly, Long viewerUserId) {
        Blog blog = blogMapper.selectById(series.getBlogId());
        CommunityUser creator = userMapper.selectById(series.getCreatedByUserId());
        List<SeriesChapterView> chapters = new ArrayList<>();
        for (SeriesArticle chapter : chapterMapper.findBySeries(series.getId())) {
            Article article = articleMapper.selectById(chapter.getArticleId());
            if (article == null || article.getDeletedAt() != null || (publicOnly && !"PUBLISHED".equals(article.getPublishStatus()))) continue;
            chapters.add(new SeriesChapterView(article.getId(), article.getTitle(), article.getSlug(), article.getPublishStatus(), chapter.getChapterOrder()));
        }
        long followerCount = followMapper.countFollowers(FOLLOW_TARGET_SERIES, series.getId());
        boolean following = viewerUserId != null
                && followMapper.findRelation(viewerUserId, FOLLOW_TARGET_SERIES, series.getId()) != null;
        SeriesReadingProgress progress = viewerUserId == null
                ? null
                : progressMapper.findByUserAndSeries(viewerUserId, series.getId());
        return new SeriesView(series.getId(), series.getBlogId(),
                blog != null ? blog.getName() : null, blog != null ? blog.getSlug() : null, blog != null ? blog.getBlogType() : null,
                series.getCreatedByUserId(),
                creator != null ? creator.getDisplayName() : null, creator != null ? creator.getUsername() : null, creator != null ? creator.getAvatarFileId() : null,
                series.getTitle(), series.getSlug(), series.getSummary(), series.getCoverFileId(),
                series.getSerializationStatus(), series.getReviewStatus(), series.getReviewComment(),
                chapters.size(), followerCount, following,
                progress == null ? null : progress.getLastArticleId(),
                progress == null ? 0 : intValue(progress.getMaxChapterOrder()),
                series.getLockVersion(), series.getPublishedAt(), series.getUpdatedAt(), chapters);
    }

    private static int intValue(Integer value) { return value == null ? 0 : value; }

    private void audit(Series series, Long actorUserId, String eventType, String before, String after) {
        TeamAuditEvent event = new TeamAuditEvent();
        event.setTeamId(resolveTeamId(series.getBlogId()));
        event.setActorUserId(actorUserId); event.setEventType(eventType); event.setTargetType("SERIES"); event.setTargetId(series.getId());
        event.setBeforeSnapshot("{\"status\":\"" + before + "\"}"); event.setAfterSnapshot("{\"status\":\"" + after + "\"}");
        event.setOccurredAt(now()); auditMapper.insert(event);
    }

    private Long resolveTeamId(Long blogId) {
        Blog blog = blogMapper.selectById(blogId);
        if (blog == null || !"TEAM".equals(blog.getBlogType())) return null;
        Team team = teamMapper.findByBlogId(blogId);
        return team == null ? null : team.getId();
    }

    private static String required(String value, String label, int max) { String text = optional(value, max); if (text == null) throw bad(label + "不能为空"); return text; }
    private static String optional(String value, int max) { if (value == null) return null; String text = value.trim(); return text.isEmpty() ? null : text.substring(0, Math.min(max, text.length())); }
    /** Normalizes a slug; when the title is fully non-ASCII (e.g. Chinese) and no slug was supplied, falls back to a stable id-derived one. */
    private static String slug(String requested, String title, Long fallbackId) {
        String value = optional(requested, 160);
        boolean explicit = value != null;
        if (value == null) value = title;
        value = value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9-]+", "-").replaceAll("^-+|-+$", "");
        if (value.isBlank()) {
            if (explicit || fallbackId == null) throw bad("系列标识格式无效");
            return "series-" + Long.toString(Math.abs(fallbackId), 36);
        }
        if (value.length() > 160) throw bad("系列标识格式无效");
        return value;
    }
    private static String serialization(String value) { String status = value == null ? "ONGOING" : value.trim().toUpperCase(Locale.ROOT); if (!Set.of("ONGOING", "COMPLETED", "PAUSED").contains(status)) throw bad("连载状态无效"); return status; }
    private static void requireLock(Integer expected, Series series) { if (expected == null || !Objects.equals(expected, series.getLockVersion())) throw collision(); }
    private static LocalDateTime now() { return LocalDateTime.now(ZoneOffset.UTC); }
    private static BusinessException bad(String message) { return new BusinessException(400, message); }
    private static BusinessException collision() { return new BusinessException(409, "系列状态已变化，请刷新后重试"); }
}

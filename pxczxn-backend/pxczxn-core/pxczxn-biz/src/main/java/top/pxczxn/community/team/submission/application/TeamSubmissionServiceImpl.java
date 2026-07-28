package top.pxczxn.community.team.submission.application;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.pxczxn.community.article.application.ArticlePublicationPolicy;
import top.pxczxn.community.article.application.ArticlePublishedEvent;
import top.pxczxn.community.article.model.Article;
import top.pxczxn.community.article.model.ArticleVersion;
import top.pxczxn.community.article.persistence.ArticleMapper;
import top.pxczxn.community.article.persistence.ArticleVersionMapper;
import top.pxczxn.community.blog.model.Blog;
import top.pxczxn.community.blog.persistence.BlogMapper;
import top.pxczxn.community.notification.application.CommunityNotificationEvent;
import top.pxczxn.community.sanction.application.CommunitySanctionService;
import top.pxczxn.community.sanction.application.SanctionAction;
import top.pxczxn.community.team.application.TeamAuthorityService;
import top.pxczxn.community.team.model.Team;
import top.pxczxn.community.team.model.TeamAuditEvent;
import top.pxczxn.community.team.persistence.TeamAuditEventMapper;
import top.pxczxn.community.team.persistence.TeamMapper;
import top.pxczxn.community.team.submission.model.TeamSubmission;
import top.pxczxn.community.team.submission.persistence.TeamSubmissionMapper;
import top.pxczxn.platform.common.exception.BusinessException;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class TeamSubmissionServiceImpl implements TeamSubmissionService {
    private static final Set<String> RESUBMITTABLE = Set.of("TEAM_REVISION_REQUIRED", "TEAM_REJECTED", "PLATFORM_REVISION_REQUIRED", "PLATFORM_REJECTED");
    private final TeamSubmissionMapper submissionMapper;
    private final ArticleMapper articleMapper;
    private final ArticleVersionMapper versionMapper;
    private final TeamMapper teamMapper;
    private final BlogMapper blogMapper;
    private final TeamAuthorityService authorityService;
    private final TeamAuditEventMapper auditMapper;
    private final ApplicationEventPublisher eventPublisher;
    private final CommunitySanctionService sanctionService;

    @Override
    @Transactional
    public TeamSubmissionView submit(Long actorUserId, CreateTeamSubmissionCommand command) {
        sanctionService.requireActionAllowed(actorUserId, SanctionAction.SUBMIT);
        if (command == null || command.sourceArticleId() == null || command.targetTeamId() == null) throw new BusinessException(400, "投稿信息不完整");
        String key = idempotencyKey(command.idempotencyKey());
        TeamSubmission sameKey = submissionMapper.findByIdempotencyKey(key);
        if (sameKey != null) {
            if (Objects.equals(sameKey.getSubmittedByUserId(), actorUserId) && Objects.equals(sameKey.getSourceArticleId(), command.sourceArticleId()) && Objects.equals(sameKey.getTargetTeamId(), command.targetTeamId())) return view(sameKey);
            throw new BusinessException(409, "幂等键已用于另一条投稿");
        }
        Article source = requireSourceArticle(actorUserId, command.sourceArticleId());
        ArticleVersion fixed = requireVersion(source);
        Team team = requireActiveTeam(command.targetTeamId());
        Long supersedes = command.supersedesSubmissionId();
        if (supersedes != null) requireResubmission(actorUserId, source.getId(), team.getId(), supersedes);
        LocalDateTime now = now();
        TeamSubmission submission = new TeamSubmission();
        submission.setId(IdWorker.getId()); submission.setSourceArticleId(source.getId()); submission.setFixedSourceVersionId(fixed.getId());
        submission.setTargetTeamId(team.getId()); submission.setSubmittedByUserId(actorUserId); submission.setSupersedesSubmissionId(supersedes);
        submission.setStatus("TEAM_PENDING"); submission.setIdempotencyKey(key); submission.setLockVersion(0); submission.setCreatedAt(now); submission.setUpdatedAt(now);
        try { if (submissionMapper.insert(submission) != 1) throw new BusinessException(500, "创建投稿失败"); }
        catch (DuplicateKeyException exception) {
            TeamSubmission concurrent = submissionMapper.findByIdempotencyKey(key);
            if (concurrent != null && Objects.equals(concurrent.getSubmittedByUserId(), actorUserId)) return view(concurrent);
            throw new BusinessException(409, "该文章正在投稿至此团队");
        }
        audit(team.getId(), actorUserId, "EXTERNAL_SUBMISSION_CREATED", submission.getId(), null, submission.getStatus(), now);
        eventPublisher.publishEvent(notification("TEAM_SUBMISSION_CREATED", actorUserId, team.getOwnerUserId(), submission.getId(), "收到外部投稿", source.getTitle()));
        return view(submission);
    }

    @Override @Transactional(readOnly = true)
    public List<TeamSubmissionView> mine(Long actorUserId) { return submissionMapper.findByAuthor(actorUserId).stream().map(this::view).toList(); }

    @Override @Transactional(readOnly = true)
    public List<TeamSubmissionView> teamQueue(Long actorUserId, Long teamId) {
        requireSubmissionManager(actorUserId, teamId);
        return submissionMapper.findByTeam(teamId).stream().map(this::view).toList();
    }

    @Override @Transactional(readOnly = true)
    public List<TeamSubmissionView> platformQueue() { return submissionMapper.findPlatformQueue().stream().map(this::view).toList(); }

    @Override @Transactional public TeamSubmissionView teamApprove(Long actor, Long id, DecideTeamSubmissionCommand c) { return decideTeam(actor, id, c, "PLATFORM_PENDING", "EXTERNAL_SUBMISSION_TEAM_APPROVED", "团队审核通过，等待平台审核"); }
    @Override @Transactional public TeamSubmissionView teamRequestRevision(Long actor, Long id, DecideTeamSubmissionCommand c) { return decideTeam(actor, id, c, "TEAM_REVISION_REQUIRED", "EXTERNAL_SUBMISSION_TEAM_REVISION", "团队要求修改投稿"); }
    @Override @Transactional public TeamSubmissionView teamReject(Long actor, Long id, DecideTeamSubmissionCommand c) { return decideTeam(actor, id, c, "TEAM_REJECTED", "EXTERNAL_SUBMISSION_TEAM_REJECTED", "团队拒绝了投稿"); }

    @Override
    @Transactional
    public TeamSubmissionView platformApprove(Long adminId, Long id, DecideTeamSubmissionCommand command) {
        TeamSubmission submission = requireSubmission(id); requireLock(command, submission);
        LocalDateTime now = now(); String comment = comment(command);
        if (submissionMapper.beginPlatformPublication(id, submission.getLockVersion(), adminId, comment, now) != 1) throw collision();
        Article published = publishSnapshot(submission, now);
        if (submissionMapper.completePublication(id, safe(submission.getLockVersion()) + 1, published.getId(), now) != 1) throw collision();
        submission.setStatus("PUBLISHED"); submission.setPlatformReviewerAdminId(adminId); submission.setPlatformReviewComment(comment); submission.setPlatformReviewedAt(now); submission.setPublishedTeamArticleId(published.getId()); submission.setUpdatedAt(now); submission.setLockVersion(safe(submission.getLockVersion()) + 2);
        audit(submission.getTargetTeamId(), null, "EXTERNAL_SUBMISSION_PLATFORM_APPROVED", id, "PLATFORM_PUBLISHING", "PUBLISHED", now);
        eventPublisher.publishEvent(notification("TEAM_SUBMISSION_PUBLISHED", null, submission.getSubmittedByUserId(), id, "投稿已发布", published.getTitle()));
        return view(submission);
    }
    @Override @Transactional public TeamSubmissionView platformRequestRevision(Long admin, Long id, DecideTeamSubmissionCommand c) { return decidePlatform(admin, id, c, "PLATFORM_REVISION_REQUIRED", "投稿需要平台修改"); }
    @Override @Transactional public TeamSubmissionView platformReject(Long admin, Long id, DecideTeamSubmissionCommand c) { return decidePlatform(admin, id, c, "PLATFORM_REJECTED", "投稿被平台拒绝"); }

    private TeamSubmissionView decideTeam(Long actor, Long id, DecideTeamSubmissionCommand command, String status, String event, String notificationContent) {
        TeamSubmission s = requireSubmission(id); requireLock(command, s); requireSubmissionManager(actor, s.getTargetTeamId()); LocalDateTime now = now(); String comment = comment(command);
        if (submissionMapper.decideByTeam(id, s.getLockVersion(), status, actor, comment, now) != 1) throw collision();
        s.setStatus(status); s.setTeamReviewerUserId(actor); s.setTeamReviewComment(comment); s.setTeamReviewedAt(now); s.setUpdatedAt(now); s.setLockVersion(safe(s.getLockVersion()) + 1);
        audit(s.getTargetTeamId(), actor, event, id, "TEAM_PENDING", status, now);
        eventPublisher.publishEvent(notification("TEAM_SUBMISSION_DECISION", actor, s.getSubmittedByUserId(), id, "团队投稿审核结果", notificationContent));
        return view(s);
    }
    private TeamSubmissionView decidePlatform(Long admin, Long id, DecideTeamSubmissionCommand command, String status, String content) {
        TeamSubmission s = requireSubmission(id); requireLock(command, s); LocalDateTime now = now(); String comment = comment(command);
        if (submissionMapper.decideByPlatform(id, s.getLockVersion(), status, admin, comment, now) != 1) throw collision();
        s.setStatus(status); s.setPlatformReviewerAdminId(admin); s.setPlatformReviewComment(comment); s.setPlatformReviewedAt(now); s.setUpdatedAt(now); s.setLockVersion(safe(s.getLockVersion()) + 1);
        audit(s.getTargetTeamId(), null, "EXTERNAL_SUBMISSION_PLATFORM_DECISION", id, "PLATFORM_PENDING", status, now);
        eventPublisher.publishEvent(notification("TEAM_SUBMISSION_PLATFORM_DECISION", null, s.getSubmittedByUserId(), id, "平台投稿审核结果", content));
        return view(s);
    }
    private Article publishSnapshot(TeamSubmission s, LocalDateTime now) {
        Article source = articleMapper.selectById(s.getSourceArticleId()); ArticleVersion fixed = versionMapper.selectById(s.getFixedSourceVersionId()); Team team = requireActiveTeam(s.getTargetTeamId()); Blog teamBlog = blogMapper.selectById(team.getBlogId());
        if (source == null || fixed == null || !Objects.equals(fixed.getArticleId(), source.getId()) || teamBlog == null || !"TEAM".equals(teamBlog.getBlogType())) throw new BusinessException(409, "投稿快照或目标团队已失效");
        Long articleId = IdWorker.getId(); Long versionId = IdWorker.getId();
        Article article = new Article(); article.setId(articleId); article.setBlogId(teamBlog.getId()); article.setAuthorUserId(source.getAuthorUserId()); article.setTitle(source.getTitle()); article.setSlug(teamSlug(source.getSlug(), articleId)); article.setSummary(source.getSummary()); article.setCoverFileId(source.getCoverFileId()); article.setContentMode(fixed.getContentMode()); article.setVisibility("PUBLIC"); article.setPublishMethod("IMMEDIATE"); article.setPublishStatus("PUBLISHED"); article.setReviewStatus("APPROVED"); article.setCurrentVersionId(versionId); article.setPublishedVersionId(versionId); article.setReviewVersionId(versionId); article.setViewCount(0L); article.setLikeCount(0L); article.setFavoriteCount(0L); article.setCommentCount(0L); article.setLockVersion(0); article.setCreatedAt(now); article.setUpdatedAt(now);
        article.setCanonicalPath(ArticlePublicationPolicy.canonicalPath(teamBlog, article)); article.setPublishedAt(now);
        if (articleMapper.insert(article) != 1) throw new BusinessException(500, "创建团队文章失败");
        ArticleVersion version = new ArticleVersion(); version.setId(versionId); version.setArticleId(articleId); version.setVersionNo(1); version.setContentMode(fixed.getContentMode()); version.setRichTextJson(fixed.getRichTextJson()); version.setMarkdownContent(fixed.getMarkdownContent()); version.setRenderedHtml(fixed.getRenderedHtml()); version.setPlainText(fixed.getPlainText()); version.setTocJson(fixed.getTocJson()); version.setContentHash(fixed.getContentHash()); version.setWordCount(fixed.getWordCount()); version.setReadingTimeMinutes(fixed.getReadingTimeMinutes()); version.setCreatedByUserId(source.getAuthorUserId()); version.setCreationType("TEAM_SUBMISSION_SNAPSHOT"); version.setCreatedAt(now);
        if (versionMapper.insert(version) != 1) throw new BusinessException(500, "创建团队文章快照失败");
        eventPublisher.publishEvent(new ArticlePublishedEvent(articleId, teamBlog.getId(), source.getAuthorUserId(), null, versionId, article.getCanonicalPath(), now, "TEAM_SUBMISSION"));
        return article;
    }
    private Article requireSourceArticle(Long actor, Long articleId) { Article a = articleMapper.selectById(articleId); if (a == null || a.getDeletedAt() != null || !Objects.equals(a.getAuthorUserId(), actor)) throw new BusinessException(403, "只能投稿自己的有效文章"); Blog b = blogMapper.selectById(a.getBlogId()); if (b == null || !"PERSONAL".equals(b.getBlogType())) throw new BusinessException(409, "仅支持从个人博客投稿"); return a; }
    private ArticleVersion requireVersion(Article a) { if (a.getCurrentVersionId() == null) throw new BusinessException(409, "文章没有可投稿的版本"); ArticleVersion v = versionMapper.selectById(a.getCurrentVersionId()); if (v == null || !Objects.equals(v.getArticleId(), a.getId())) throw new BusinessException(409, "文章当前版本无效"); return v; }
    private Team requireActiveTeam(Long teamId) { Team t = teamMapper.selectById(teamId); if (t == null || !"ACTIVE".equals(t.getStatus()) || t.getDeletedAt() != null) throw new BusinessException(404, "团队不存在或已停用"); return t; }
    private void requireResubmission(Long actor, Long source, Long team, Long previousId) { TeamSubmission p = requireSubmission(previousId); if (!Objects.equals(p.getSubmittedByUserId(), actor) || !Objects.equals(p.getSourceArticleId(), source) || !Objects.equals(p.getTargetTeamId(), team) || !RESUBMITTABLE.contains(p.getStatus())) throw new BusinessException(409, "只能重投自己的已退回或已拒绝投稿"); }
    private TeamSubmission requireSubmission(Long id) { TeamSubmission s = submissionMapper.selectById(id); if (s == null) throw new BusinessException(404, "投稿不存在"); return s; }
    private void requireSubmissionManager(Long user, Long team) { if (!authorityService.hasPermission(user, team, "MANAGE_SUBMISSIONS")) throw new BusinessException(403, "没有处理该团队投稿的权限"); }
    private TeamSubmissionView view(TeamSubmission s) { Article a = articleMapper.selectById(s.getSourceArticleId()); return TeamSubmissionView.from(s, a == null ? "文章已不可用" : a.getTitle()); }
    private void audit(Long team, Long actor, String event, Long id, String before, String after, LocalDateTime now) { TeamAuditEvent e = new TeamAuditEvent(); e.setTeamId(team); e.setActorUserId(actor); e.setEventType(event); e.setTargetType("TEAM_SUBMISSION"); e.setTargetId(id); e.setBeforeSnapshot(before == null ? null : "{\"status\":\"" + before + "\"}"); e.setAfterSnapshot("{\"status\":\"" + after + "\"}"); e.setOccurredAt(now); auditMapper.insert(e); }
    private static CommunityNotificationEvent notification(String type, Long sender, Long recipient, Long target, String title, String content) { return new CommunityNotificationEvent(type, "TEAM", sender, recipient, "TEAM_SUBMISSION", target, title, content, type + ":" + target + ":" + recipient, "NORMAL"); }
    private static void requireLock(DecideTeamSubmissionCommand c, TeamSubmission s) { if (c == null || c.expectedLockVersion() == null || !Objects.equals(c.expectedLockVersion(), s.getLockVersion())) throw collision(); }
    private static String idempotencyKey(String key) { if (key == null || !key.matches("[A-Za-z0-9][A-Za-z0-9._:-]{7,79}")) throw new BusinessException(400, "幂等键格式无效"); return key; }
    private static String comment(DecideTeamSubmissionCommand c) { return c.comment() == null ? null : c.comment().trim().substring(0, Math.min(c.comment().trim().length(), 1000)); }
    private static String teamSlug(String source, Long id) { String base = source == null ? "submission" : source.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9-]+", "-").replaceAll("^-+|-+$", ""); if (base.isBlank()) base = "submission"; String suffix = "-team-" + id; return (base.length() > 160 - suffix.length() ? base.substring(0, 160 - suffix.length()) : base) + suffix; }
    private static int safe(Integer value) { return value == null ? 0 : value; }
    private static LocalDateTime now() { return LocalDateTime.now(ZoneOffset.UTC); }
    private static BusinessException collision() { return new BusinessException(409, "投稿状态已变化，请刷新后重试"); }
}

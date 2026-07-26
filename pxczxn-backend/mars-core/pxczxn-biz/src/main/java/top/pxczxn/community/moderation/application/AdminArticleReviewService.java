package top.pxczxn.community.moderation.application;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mars.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.pxczxn.community.article.model.Article;
import top.pxczxn.community.article.model.ArticleVersion;
import top.pxczxn.community.article.application.ArticlePublicationPolicy;
import top.pxczxn.community.article.application.ArticlePublishedEvent;
import top.pxczxn.community.article.permission.ArticleAction;
import top.pxczxn.community.article.permission.ArticlePlatformAccess;
import top.pxczxn.community.article.permission.ArticlePermissionService;
import top.pxczxn.community.article.persistence.ArticleMapper;
import top.pxczxn.community.article.persistence.ArticleVersionMapper;
import top.pxczxn.community.blog.model.Blog;
import top.pxczxn.community.blog.persistence.BlogMapper;
import top.pxczxn.community.moderation.model.ContentReviewTask;
import top.pxczxn.community.moderation.persistence.ContentReviewTaskMapper;
import top.pxczxn.community.notification.application.ArticleReviewDecisionNotificationEvent;
import top.pxczxn.community.user.model.CommunityUser;
import top.pxczxn.community.user.persistence.CommunityUserMapper;

import java.text.Normalizer;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminArticleReviewService {

    private static final Set<String> TASK_STATUSES = Set.of(
            "QUEUED",
            "AUTO_REVIEWING",
            "MANUAL_REVIEWING",
            "APPROVED",
            "REVISION_REQUIRED",
            "REJECTED",
            "CANCELLED",
            "EXPIRED"
    );
    private static final Set<String> RISK_LEVELS =
            Set.of("LOW", "MEDIUM", "HIGH", "CRITICAL");
    private static final Set<String> PRESERVED_PUBLIC_STATUSES =
            Set.of("PUBLISHED", "HIDDEN");

    private final ContentReviewTaskMapper taskMapper;
    private final ArticleMapper articleMapper;
    private final ArticleVersionMapper versionMapper;
    private final BlogMapper blogMapper;
    private final CommunityUserMapper userMapper;
    private final ArticlePermissionService permissionService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional(readOnly = true)
    public AdminArticleReviewPageView page(AdminArticleReviewQuery rawQuery) {
        AdminArticleReviewQuery query = normalizeQuery(rawQuery);
        LambdaQueryWrapper<ContentReviewTask> wrapper =
                Wrappers.<ContentReviewTask>lambdaQuery()
                        .eq(
                                ContentReviewTask::getSubjectType,
                                "ARTICLE"
                        )
                        .eq(
                                query.status() != null,
                                ContentReviewTask::getStatus,
                                query.status()
                        )
                        .eq(
                                query.riskLevel() != null,
                                ContentReviewTask::getRiskLevel,
                                query.riskLevel()
                        )
                        .ge(
                                query.submittedFrom() != null,
                                ContentReviewTask::getSubmittedAt,
                                query.submittedFrom()
                        )
                        .le(
                                query.submittedTo() != null,
                                ContentReviewTask::getSubmittedAt,
                                query.submittedTo()
                        )
                        .orderByDesc(ContentReviewTask::getSubmittedAt)
                        .orderByDesc(ContentReviewTask::getId);
        IPage<ContentReviewTask> result = taskMapper.selectPage(
                new Page<>(query.pageNum(), query.pageSize()),
                wrapper
        );
        List<ContentReviewTask> tasks = result.getRecords();
        RelatedData related = loadRelated(tasks);
        List<AdminArticleReviewListItemView> items = tasks.stream()
                .map(task -> toListItem(task, related))
                .toList();
        return new AdminArticleReviewPageView(
                items,
                result.getTotal(),
                result.getCurrent(),
                result.getSize()
        );
    }

    @Transactional(readOnly = true)
    public AdminArticleReviewDetailView detail(Long taskId) {
        ContentReviewTask task = requireTask(taskId);
        Article article = requireArticle(task);
        ArticleVersion version = requireFixedVersion(task, article);
        Blog blog = requireBlog(article);
        CommunityUser author = requireAuthor(article);
        return toDetail(task, article, version, blog, author);
    }

    @Transactional
    public AdminArticleReviewDetailView claim(
            Long taskId,
            Long adminId,
            ClaimArticleReviewCommand command
    ) {
        requireAdminId(adminId);
        ContentReviewTask task = requireTask(taskId);
        ArticlePlatformAccess platformAccess =
                permissionService.requirePlatformArticle(
                task.getArticleId(),
                ArticleAction.PLATFORM_REVIEW
        );
        Article article = platformAccess.article();
        if ("MANUAL_REVIEWING".equals(task.getStatus())
                && Objects.equals(task.getAssigneeAdminId(), adminId)) {
            return detail(taskId);
        }
        requireExpectedLock(task, command == null
                ? null
                : command.expectedTaskLockVersion());
        if (!"QUEUED".equals(task.getStatus())
                || task.getAssigneeAdminId() != null) {
            throw new BusinessException(409, "审核任务已被领取或状态已变化");
        }

        assertActiveTask(task, article);
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        int taskUpdated = taskMapper.update(
                null,
                Wrappers.<ContentReviewTask>update()
                        .eq("id", task.getId())
                        .eq("lock_version", safeInt(task.getLockVersion()))
                        .eq("status", "QUEUED")
                        .isNull("assignee_admin_id")
                        .set("status", "MANUAL_REVIEWING")
                        .set("assignee_admin_id", adminId)
                        .set("claimed_at", now)
                        .set("updated_at", now)
                        .setSql("lock_version = lock_version + 1")
        );
        if (taskUpdated != 1) {
            throw new BusinessException(409, "审核任务已被其他管理员领取");
        }
        int articleUpdated = articleMapper.update(
                null,
                Wrappers.<Article>update()
                        .eq("id", article.getId())
                        .eq("lock_version", safeInt(article.getLockVersion()))
                        .eq("review_version_id", task.getFixedVersionId())
                        .eq("review_status", "QUEUED")
                        .isNull("deleted_at")
                        .set("review_status", "MANUAL_REVIEWING")
                        .set("updated_at", now)
                        .setSql("lock_version = lock_version + 1")
        );
        if (articleUpdated != 1) {
            throw new BusinessException(
                    409,
                    "文章审核状态已变化，请刷新后重试"
            );
        }
        task.setStatus("MANUAL_REVIEWING");
        task.setAssigneeAdminId(adminId);
        task.setClaimedAt(now);
        task.setUpdatedAt(now);
        task.setLockVersion(safeInt(task.getLockVersion()) + 1);
        article.setReviewStatus("MANUAL_REVIEWING");
        article.setUpdatedAt(now);
        article.setLockVersion(safeInt(article.getLockVersion()) + 1);
        log.info(
                "Article review claimed: taskId={}, articleId={}, adminId={}",
                task.getId(),
                task.getArticleId(),
                adminId
        );
        return detail(taskId);
    }

    @Transactional
    public AdminArticleReviewDetailView approve(
            Long taskId,
            Long adminId,
            DecideArticleReviewCommand command
    ) {
        return decide(taskId, adminId, command, "APPROVED");
    }

    @Transactional
    public AdminArticleReviewDetailView requestRevision(
            Long taskId,
            Long adminId,
            DecideArticleReviewCommand command
    ) {
        return decide(taskId, adminId, command, "REVISION_REQUIRED");
    }

    @Transactional
    public AdminArticleReviewDetailView reject(
            Long taskId,
            Long adminId,
            DecideArticleReviewCommand command
    ) {
        return decide(taskId, adminId, command, "REJECTED");
    }

    private AdminArticleReviewDetailView decide(
            Long taskId,
            Long adminId,
            DecideArticleReviewCommand command,
            String decision
    ) {
        requireAdminId(adminId);
        if (command == null) {
            throw new BusinessException(400, "审核决定不能为空");
        }
        ContentReviewTask task = requireTask(taskId);
        requireExpectedLock(task, command.expectedTaskLockVersion());
        if (!"MANUAL_REVIEWING".equals(task.getStatus())) {
            throw new BusinessException(409, "审核任务尚未领取或已处理");
        }
        if (!Objects.equals(task.getAssigneeAdminId(), adminId)) {
            throw new BusinessException(403, "只能处理自己领取的审核任务");
        }
        ArticlePlatformAccess platformAccess =
                permissionService.requirePlatformArticle(
                task.getArticleId(),
                ArticleAction.PLATFORM_REVIEW
        );
        Article article = platformAccess.article();
        assertActiveTask(task, article);
        String reason = normalizeDecisionReason(command.reason(), decision);
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        String resultCode = switch (decision) {
            case "APPROVED" -> "MANUAL_APPROVED";
            case "REVISION_REQUIRED" -> "MANUAL_REVISION_REQUIRED";
            case "REJECTED" -> "MANUAL_REJECTED";
            default -> throw new BusinessException(400, "审核决定无效");
        };

        int taskUpdated = taskMapper.update(
                null,
                Wrappers.<ContentReviewTask>update()
                        .eq("id", task.getId())
                        .eq("lock_version", safeInt(task.getLockVersion()))
                        .eq("status", "MANUAL_REVIEWING")
                        .eq("assignee_admin_id", adminId)
                        .set("status", decision)
                        .set("result_code", resultCode)
                        .set("result_reason", reason)
                        .set("completed_at", now)
                        .set("updated_at", now)
                        .setSql("lock_version = lock_version + 1")
        );
        if (taskUpdated != 1) {
            throw new BusinessException(
                    409,
                    "审核任务状态已变化，请刷新后重试"
            );
        }

        Long previousPublishedVersionId = article.getPublishedVersionId();
        boolean publishImmediately = "APPROVED".equals(decision)
                && ArticlePublicationPolicy.publishesImmediately(article);
        String canonicalPath = publishImmediately
                ? ArticlePublicationPolicy.canonicalPath(
                        platformAccess.blog(),
                        article
                )
                : article.getCanonicalPath();
        String nextPublishStatus = publishImmediately
                ? "PUBLISHED"
                : nextPublishStatus(article, decision);
        int articleUpdated = articleMapper.update(
                null,
                Wrappers.<Article>update()
                        .eq("id", article.getId())
                        .eq("lock_version", safeInt(article.getLockVersion()))
                        .eq("review_version_id", task.getFixedVersionId())
                        .eq("review_status", "MANUAL_REVIEWING")
                        .isNull("deleted_at")
                        .set("publish_status", nextPublishStatus)
                        .set("review_status", decision)
                        .set(
                                publishImmediately,
                                "published_version_id",
                                task.getFixedVersionId()
                        )
                        .set(
                                publishImmediately,
                                "canonical_path",
                                canonicalPath
                        )
                        .set(publishImmediately, "published_at", now)
                        .set(
                                publishImmediately,
                                "scheduled_publish_at",
                                null
                        )
                        .set("updated_at", now)
                        .setSql("lock_version = lock_version + 1")
        );
        if (articleUpdated != 1) {
            throw new BusinessException(
                    409,
                    "文章审核状态已变化，请刷新后重试"
            );
        }

        task.setStatus(decision);
        task.setResultCode(resultCode);
        task.setResultReason(reason);
        task.setCompletedAt(now);
        task.setUpdatedAt(now);
        task.setLockVersion(safeInt(task.getLockVersion()) + 1);
        article.setPublishStatus(nextPublishStatus);
        article.setReviewStatus(decision);
        if (publishImmediately) {
            article.setPublishedVersionId(task.getFixedVersionId());
            article.setCanonicalPath(canonicalPath);
            article.setPublishedAt(now);
            article.setScheduledPublishAt(null);
        }
        article.setUpdatedAt(now);
        article.setLockVersion(safeInt(article.getLockVersion()) + 1);
        if (publishImmediately) {
            eventPublisher.publishEvent(new ArticlePublishedEvent(
                    article.getId(),
                    platformAccess.blog().getId(),
                    article.getAuthorUserId(),
                    previousPublishedVersionId,
                    task.getFixedVersionId(),
                    canonicalPath,
                    now,
                    "ADMIN_REVIEW"
            ));
        }
        eventPublisher.publishEvent(
                new ArticleReviewDecisionNotificationEvent(
                        task.getId(),
                        article.getId(),
                        task.getSubmittedByUserId(),
                        article.getTitle(),
                        decision,
                        reason
                )
        );
        log.info(
                "Article review decided: taskId={}, articleId={}, adminId={}, decision={}",
                task.getId(),
                article.getId(),
                adminId,
                decision
        );
        return detail(taskId);
    }

    private RelatedData loadRelated(List<ContentReviewTask> tasks) {
        if (tasks == null || tasks.isEmpty()) {
            return RelatedData.empty();
        }
        Set<Long> articleIds = tasks.stream()
                .map(ContentReviewTask::getArticleId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<Long, Article> articles = mapById(
                articleIds.isEmpty()
                        ? List.of()
                        : articleMapper.selectBatchIds(articleIds),
                Article::getId
        );
        Set<Long> blogIds = articles.values().stream()
                .map(Article::getBlogId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Set<Long> userIds = articles.values().stream()
                .map(Article::getAuthorUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<Long, Blog> blogs = mapById(
                blogIds.isEmpty()
                        ? List.of()
                        : blogMapper.selectBatchIds(blogIds),
                Blog::getId
        );
        Map<Long, CommunityUser> users = mapById(
                userIds.isEmpty()
                        ? List.of()
                        : userMapper.selectBatchIds(userIds),
                CommunityUser::getId
        );
        return new RelatedData(articles, blogs, users);
    }

    private static AdminArticleReviewListItemView toListItem(
            ContentReviewTask task,
            RelatedData related
    ) {
        Article article = related.articles().get(task.getArticleId());
        Blog blog = article == null
                ? null
                : related.blogs().get(article.getBlogId());
        CommunityUser author = article == null
                ? null
                : related.users().get(article.getAuthorUserId());
        return new AdminArticleReviewListItemView(
                task.getId(),
                task.getArticleId(),
                task.getFixedVersionId(),
                article == null ? null : article.getTitle(),
                article == null ? null : article.getAuthorUserId(),
                author == null ? null : author.getDisplayName(),
                article == null ? null : article.getBlogId(),
                blog == null ? null : blog.getName(),
                task.getStatus(),
                task.getRiskLevel(),
                task.getReviewStage(),
                task.getReviewType(),
                task.getAssigneeAdminId(),
                task.getResultCode(),
                safeInt(task.getLockVersion()),
                task.getSubmittedAt(),
                task.getClaimedAt(),
                task.getCompletedAt()
        );
    }

    private static AdminArticleReviewDetailView toDetail(
            ContentReviewTask task,
            Article article,
            ArticleVersion version,
            Blog blog,
            CommunityUser author
    ) {
        AdminArticleReviewContentView content =
                new AdminArticleReviewContentView(
                        version.getId(),
                        safeInt(version.getVersionNo()),
                        version.getContentMode(),
                        version.getRichTextJson(),
                        version.getMarkdownContent(),
                        version.getRenderedHtml(),
                        version.getPlainText(),
                        version.getTocJson(),
                        version.getContentHash(),
                        safeInt(version.getWordCount()),
                        Math.max(1, safeInt(version.getReadingTimeMinutes()))
                );
        return new AdminArticleReviewDetailView(
                task.getId(),
                article.getId(),
                task.getFixedVersionId(),
                article.getTitle(),
                article.getSummary(),
                article.getVisibility(),
                article.getPublishStatus(),
                article.getReviewStatus(),
                article.getAuthorUserId(),
                author.getUsername(),
                author.getDisplayName(),
                blog.getId(),
                blog.getName(),
                blog.getSlug(),
                task.getStatus(),
                task.getRiskLevel(),
                task.getReviewStage(),
                task.getReviewType(),
                task.getSubmittedByUserId(),
                task.getAssigneeAdminId(),
                task.getResultCode(),
                task.getResultReason(),
                safeInt(task.getLockVersion()),
                safeInt(article.getLockVersion()),
                task.getSubmittedAt(),
                task.getClaimedAt(),
                task.getCompletedAt(),
                content
        );
    }

    private ContentReviewTask requireTask(Long taskId) {
        if (taskId == null || taskId <= 0) {
            throw new BusinessException(400, "审核任务 ID 无效");
        }
        ContentReviewTask task = taskMapper.selectById(taskId);
        if (task == null || !"ARTICLE".equals(task.getSubjectType())) {
            throw new BusinessException(404, "审核任务不存在");
        }
        return task;
    }

    private Article requireArticle(ContentReviewTask task) {
        Article article = articleMapper.selectById(task.getArticleId());
        if (article == null
                || article.getDeletedAt() != null
                || "DELETED".equals(article.getPublishStatus())) {
            throw new BusinessException(404, "审核文章不存在");
        }
        return article;
    }

    private ArticleVersion requireFixedVersion(
            ContentReviewTask task,
            Article article
    ) {
        ArticleVersion version = versionMapper.selectById(
                task.getFixedVersionId()
        );
        if (version == null
                || !Objects.equals(version.getArticleId(), article.getId())) {
            throw new BusinessException(409, "审核固定版本不存在或关联错误");
        }
        return version;
    }

    private Blog requireBlog(Article article) {
        Blog blog = blogMapper.selectById(article.getBlogId());
        if (blog == null || blog.getDeletedAt() != null) {
            throw new BusinessException(404, "文章所属博客不存在");
        }
        return blog;
    }

    private CommunityUser requireAuthor(Article article) {
        CommunityUser author = userMapper.selectById(article.getAuthorUserId());
        if (author == null) {
            throw new BusinessException(404, "文章作者不存在");
        }
        return author;
    }

    private static void assertActiveTask(
            ContentReviewTask task,
            Article article
    ) {
        if (!Objects.equals(task.getFixedVersionId(), article.getReviewVersionId())
                || !Set.of("QUEUED", "MANUAL_REVIEWING")
                        .contains(article.getReviewStatus())) {
            throw new BusinessException(
                    409,
                    "审核任务与文章当前审核版本不一致"
            );
        }
    }

    private static AdminArticleReviewQuery normalizeQuery(
            AdminArticleReviewQuery query
    ) {
        if (query == null) {
            return new AdminArticleReviewQuery(null, null, null, null, 1, 20);
        }
        String status = normalizeEnum(query.status(), TASK_STATUSES, "审核状态");
        String risk = normalizeEnum(query.riskLevel(), RISK_LEVELS, "风险等级");
        if (query.submittedFrom() != null
                && query.submittedTo() != null
                && query.submittedFrom().isAfter(query.submittedTo())) {
            throw new BusinessException(400, "提交时间范围无效");
        }
        int pageNum = query.pageNum() <= 0 ? 1 : query.pageNum();
        int pageSize = query.pageSize() <= 0 ? 20 : query.pageSize();
        if (pageSize > 100) {
            throw new BusinessException(400, "每页最多查询 100 条");
        }
        return new AdminArticleReviewQuery(
                status,
                risk,
                query.submittedFrom(),
                query.submittedTo(),
                pageNum,
                pageSize
        );
    }

    private static String normalizeEnum(
            String raw,
            Set<String> allowed,
            String label
    ) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String value = raw.trim().toUpperCase();
        if (!allowed.contains(value)) {
            throw new BusinessException(400, label + "无效");
        }
        return value;
    }

    private static void requireExpectedLock(
            ContentReviewTask task,
            Integer expectedLockVersion
    ) {
        if (expectedLockVersion == null || expectedLockVersion < 0) {
            throw new BusinessException(400, "必须提交有效的审核任务锁版本");
        }
        if (safeInt(task.getLockVersion()) != expectedLockVersion) {
            throw new BusinessException(
                    409,
                    "审核任务已在其他窗口更新，请刷新后重试"
            );
        }
    }

    private static void requireAdminId(Long adminId) {
        if (adminId == null || adminId <= 0) {
            throw new BusinessException(401, "管理员未登录");
        }
    }

    private static String normalizeDecisionReason(
            String raw,
            String decision
    ) {
        String reason = raw == null
                ? ""
                : Normalizer.normalize(raw.trim(), Normalizer.Form.NFKC);
        if (!"APPROVED".equals(decision) && reason.isBlank()) {
            throw new BusinessException(400, "退修或驳回必须填写审核说明");
        }
        if (reason.length() > 1000) {
            throw new BusinessException(400, "审核说明不能超过 1000 个字符");
        }
        return reason.isBlank() ? "人工审核通过" : reason;
    }

    private static String nextPublishStatus(
            Article article,
            String decision
    ) {
        if (article.getPublishedVersionId() != null) {
            return PRESERVED_PUBLIC_STATUSES.contains(article.getPublishStatus())
                    ? article.getPublishStatus()
                    : "PUBLISHED";
        }
        return "APPROVED".equals(decision) ? "APPROVED" : "DRAFT";
    }

    private static <T> Map<Long, T> mapById(
            List<T> values,
            Function<T, Long> idExtractor
    ) {
        if (values == null || values.isEmpty()) {
            return Collections.emptyMap();
        }
        return values.stream().collect(Collectors.toMap(
                idExtractor,
                Function.identity(),
                (left, right) -> left
        ));
    }

    private static int safeInt(Integer value) {
        return value == null ? 0 : value;
    }

    private record RelatedData(
            Map<Long, Article> articles,
            Map<Long, Blog> blogs,
            Map<Long, CommunityUser> users
    ) {
        private static RelatedData empty() {
            return new RelatedData(Map.of(), Map.of(), Map.of());
        }
    }
}

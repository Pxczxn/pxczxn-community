package top.pxczxn.community.moderation.application;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import top.pxczxn.platform.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.pxczxn.community.article.model.Article;
import top.pxczxn.community.article.model.ArticleVersion;
import top.pxczxn.community.article.application.ArticlePublicationPolicy;
import top.pxczxn.community.article.application.ArticlePublishedEvent;
import top.pxczxn.community.article.persistence.ArticleMapper;
import top.pxczxn.community.article.persistence.ArticleVersionMapper;
import top.pxczxn.community.article.permission.ArticleAction;
import top.pxczxn.community.article.permission.ArticleCommunityAccess;
import top.pxczxn.community.article.permission.ArticlePermissionService;
import top.pxczxn.community.moderation.model.ContentReviewTask;
import top.pxczxn.community.moderation.persistence.ContentReviewTaskMapper;

import java.text.Normalizer;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class ArticleReviewSubmissionService {

    private static final Pattern IDEMPOTENCY_KEY_PATTERN =
            Pattern.compile("[A-Za-z0-9][A-Za-z0-9._:-]{7,79}");
    private static final Set<String> ACTIVE_TASK_STATUSES =
            Set.of("QUEUED", "AUTO_REVIEWING", "MANUAL_REVIEWING");
    private static final Set<String> PRESERVED_PUBLIC_STATUSES =
            Set.of("PUBLISHED", "HIDDEN");

    private final ArticleMapper articleMapper;
    private final ArticleVersionMapper versionMapper;
    private final ContentReviewTaskMapper taskMapper;
    private final ArticlePermissionService permissionService;
    private final ArticleKeywordReviewEngine keywordReviewEngine;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public ArticleReviewStatusView submit(
            Long articleId,
            SubmitArticleReviewCommand command
    ) {
        if (command == null) {
            throw new BusinessException(400, "审核提交信息不能为空");
        }
        String idempotencyKey = normalizeIdempotencyKey(command.idempotencyKey());
        ArticleCommunityAccess readAccess = permissionService
                .requireCommunityArticle(articleId, ArticleAction.VIEW_EDITOR);
        ContentReviewTask existing = findByIdempotencyKey(idempotencyKey);
        if (existing != null) {
            assertSameSubmission(existing, articleId, readAccess.actor().getId());
            return toStatus(readAccess.article(), existing);
        }

        ArticleCommunityAccess access = permissionService
                .requireCommunityArticle(articleId, ArticleAction.SUBMIT_REVIEW);
        Article article = access.article();
        requireExpectedLock(article, command.expectedLockVersion());
        if ("PRIVATE".equals(article.getVisibility())) {
            throw new BusinessException(409, "私密文章无需提交公开审核");
        }
        ArticleVersion fixedVersion = requireCurrentVersion(article);
        if (fixedVersion.getPlainText() == null
                || fixedVersion.getPlainText().isBlank()) {
            throw new BusinessException(409, "文章正文不能为空，不能提交审核");
        }

        KeywordReviewOutcome outcome = keywordReviewEngine.review(
                article.getTitle(),
                article.getSummary(),
                fixedVersion.getPlainText()
        );
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        ContentReviewTask task = createTask(
                article,
                fixedVersion,
                access.actor().getId(),
                idempotencyKey,
                outcome,
                now
        );
        try {
            if (taskMapper.insert(task) != 1) {
                throw new BusinessException(500, "创建审核任务失败");
            }
        } catch (DuplicateKeyException exception) {
            ContentReviewTask concurrent = findByIdempotencyKey(idempotencyKey);
            if (concurrent != null
                    && sameSubmission(
                            concurrent,
                            articleId,
                            access.actor().getId()
                    )) {
                Article latest = articleMapper.selectById(articleId);
                return toStatus(latest == null ? article : latest, concurrent);
            }
            throw new BusinessException(409, "文章已提交审核，请勿重复操作");
        }

        Long previousPublishedVersionId = article.getPublishedVersionId();
        boolean publishImmediately = outcome.decision()
                == KeywordReviewOutcome.Decision.AUTO_APPROVE
                && ArticlePublicationPolicy.publishesImmediately(article);
        String canonicalPath = publishImmediately
                ? ArticlePublicationPolicy.canonicalPath(access.blog(), article)
                : article.getCanonicalPath();
        String nextPublishStatus = publishImmediately
                ? "PUBLISHED"
                : nextSubmitPublishStatus(article, outcome);
        String nextReviewStatus = task.getStatus();
        int updated = articleMapper.update(
                null,
                Wrappers.<Article>update()
                        .eq("id", article.getId())
                        .eq("current_version_id", fixedVersion.getId())
                        .eq("lock_version", command.expectedLockVersion())
                        .isNull("deleted_at")
                        .set("publish_status", nextPublishStatus)
                        .set("review_status", nextReviewStatus)
                        .set("review_version_id", fixedVersion.getId())
                        .set(
                                publishImmediately,
                                "published_version_id",
                                fixedVersion.getId()
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
        if (updated != 1) {
            throw new BusinessException(409, "文章已在其他窗口更新，请刷新后重试");
        }

        article.setPublishStatus(nextPublishStatus);
        article.setReviewStatus(nextReviewStatus);
        article.setReviewVersionId(fixedVersion.getId());
        if (publishImmediately) {
            article.setPublishedVersionId(fixedVersion.getId());
            article.setCanonicalPath(canonicalPath);
            article.setPublishedAt(now);
            article.setScheduledPublishAt(null);
            eventPublisher.publishEvent(new ArticlePublishedEvent(
                    article.getId(),
                    access.blog().getId(),
                    article.getAuthorUserId(),
                    previousPublishedVersionId,
                    fixedVersion.getId(),
                    canonicalPath,
                    now,
                    "AUTO_REVIEW"
            ));
        }
        article.setUpdatedAt(now);
        article.setLockVersion(safeInt(article.getLockVersion()) + 1);
        log.info(
                "Article review submitted: articleId={}, taskId={}, result={}, risk={}",
                article.getId(),
                task.getId(),
                task.getResultCode(),
                task.getRiskLevel()
        );
        return toStatus(article, task);
    }

    @Transactional
    public ArticleReviewStatusView withdraw(
            Long articleId,
            WithdrawArticleReviewCommand command
    ) {
        if (command == null) {
            throw new BusinessException(400, "审核撤回信息不能为空");
        }
        ArticleCommunityAccess access = permissionService
                .requireCommunityArticle(articleId, ArticleAction.WITHDRAW_REVIEW);
        Article article = access.article();
        requireExpectedLock(article, command.expectedLockVersion());
        ContentReviewTask task = findActiveTask(articleId);
        if (task == null
                || !Objects.equals(task.getFixedVersionId(), article.getReviewVersionId())) {
            throw new BusinessException(409, "文章当前没有可撤回的审核任务");
        }

        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        String reason = normalizeWithdrawReason(command.reason());
        int taskUpdated = taskMapper.update(
                null,
                Wrappers.<ContentReviewTask>update()
                        .eq("id", task.getId())
                        .eq("lock_version", safeInt(task.getLockVersion()))
                        .in("status", ACTIVE_TASK_STATUSES)
                        .set("status", "CANCELLED")
                        .set("result_code", "USER_WITHDRAWN")
                        .set("result_reason", reason)
                        .set("completed_at", now)
                        .set("updated_at", now)
                        .setSql("lock_version = lock_version + 1")
        );
        if (taskUpdated != 1) {
            throw new BusinessException(409, "审核任务状态已变化，请刷新后重试");
        }

        String nextPublishStatus = article.getPublishedVersionId() == null
                ? "DRAFT"
                : preservedPublicStatus(article);
        int articleUpdated = articleMapper.update(
                null,
                Wrappers.<Article>update()
                        .eq("id", article.getId())
                        .eq("review_version_id", task.getFixedVersionId())
                        .eq("lock_version", command.expectedLockVersion())
                        .isNull("deleted_at")
                        .set("publish_status", nextPublishStatus)
                        .set("review_status", "CANCELLED")
                        .set("review_version_id", null)
                        .set("updated_at", now)
                        .setSql("lock_version = lock_version + 1")
        );
        if (articleUpdated != 1) {
            throw new BusinessException(409, "文章已在其他窗口更新，请刷新后重试");
        }

        task.setStatus("CANCELLED");
        task.setResultCode("USER_WITHDRAWN");
        task.setResultReason(reason);
        task.setCompletedAt(now);
        task.setUpdatedAt(now);
        task.setLockVersion(safeInt(task.getLockVersion()) + 1);
        article.setPublishStatus(nextPublishStatus);
        article.setReviewStatus("CANCELLED");
        article.setReviewVersionId(null);
        article.setUpdatedAt(now);
        article.setLockVersion(safeInt(article.getLockVersion()) + 1);
        log.info(
                "Article review withdrawn: articleId={}, taskId={}",
                article.getId(),
                task.getId()
        );
        return toStatus(article, task);
    }

    @Transactional(readOnly = true)
    public ArticleReviewStatusView getStatus(Long articleId) {
        Article article = permissionService
                .requireCommunityArticle(articleId, ArticleAction.VIEW_EDITOR)
                .article();
        return toStatus(article, findLatestTask(articleId));
    }

    private ContentReviewTask createTask(
            Article article,
            ArticleVersion fixedVersion,
            Long submittedByUserId,
            String idempotencyKey,
            KeywordReviewOutcome outcome,
            LocalDateTime now
    ) {
        ContentReviewTask task = new ContentReviewTask();
        task.setId(IdWorker.getId());
        task.setSubjectType("ARTICLE");
        task.setSubjectId(article.getId());
        task.setArticleId(article.getId());
        task.setFixedVersionId(fixedVersion.getId());
        task.setReviewStage(
                outcome.decision() == KeywordReviewOutcome.Decision.MANUAL_REVIEW
                        ? "PLATFORM_MANUAL"
                        : "PLATFORM_AUTO"
        );
        task.setReviewType(
                outcome.decision() == KeywordReviewOutcome.Decision.MANUAL_REVIEW
                        ? "MANUAL"
                        : "AUTO"
        );
        task.setStatus(switch (outcome.decision()) {
            case AUTO_APPROVE -> "APPROVED";
            case MANUAL_REVIEW -> "QUEUED";
            case BLOCK -> "REJECTED";
        });
        task.setRiskLevel(outcome.riskLevel());
        task.setIdempotencyKey(idempotencyKey);
        task.setSubmittedByUserId(submittedByUserId);
        task.setResultCode(outcome.resultCode());
        task.setResultReason(outcome.resultReason());
        task.setSubmittedAt(now);
        if (!"QUEUED".equals(task.getStatus())) {
            task.setCompletedAt(now);
        }
        task.setLockVersion(0);
        task.setCreatedAt(now);
        task.setUpdatedAt(now);
        return task;
    }

    private ArticleVersion requireCurrentVersion(Article article) {
        if (article.getCurrentVersionId() == null) {
            throw new BusinessException(409, "文章当前版本不存在");
        }
        ArticleVersion version = versionMapper.selectOne(
                Wrappers.<ArticleVersion>lambdaQuery()
                        .eq(ArticleVersion::getId, article.getCurrentVersionId())
                        .eq(ArticleVersion::getArticleId, article.getId())
                        .last("LIMIT 1")
        );
        if (version == null) {
            throw new BusinessException(409, "文章当前版本不存在");
        }
        return version;
    }

    private ContentReviewTask findByIdempotencyKey(String idempotencyKey) {
        return taskMapper.selectOne(
                Wrappers.<ContentReviewTask>lambdaQuery()
                        .eq(ContentReviewTask::getIdempotencyKey, idempotencyKey)
                        .last("LIMIT 1")
        );
    }

    private ContentReviewTask findActiveTask(Long articleId) {
        return taskMapper.selectOne(
                Wrappers.<ContentReviewTask>lambdaQuery()
                        .eq(ContentReviewTask::getArticleId, articleId)
                        .in(ContentReviewTask::getStatus, ACTIVE_TASK_STATUSES)
                        .orderByDesc(ContentReviewTask::getSubmittedAt)
                        .orderByDesc(ContentReviewTask::getId)
                        .last("LIMIT 1")
        );
    }

    private ContentReviewTask findLatestTask(Long articleId) {
        return taskMapper.selectOne(
                Wrappers.<ContentReviewTask>lambdaQuery()
                        .eq(ContentReviewTask::getArticleId, articleId)
                        .orderByDesc(ContentReviewTask::getSubmittedAt)
                        .orderByDesc(ContentReviewTask::getId)
                        .last("LIMIT 1")
        );
    }

    private static String nextSubmitPublishStatus(
            Article article,
            KeywordReviewOutcome outcome
    ) {
        if (article.getPublishedVersionId() != null) {
            return preservedPublicStatus(article);
        }
        return switch (outcome.decision()) {
            case AUTO_APPROVE -> "APPROVED";
            case MANUAL_REVIEW -> "PENDING_REVIEW";
            case BLOCK -> "DRAFT";
        };
    }

    private static String preservedPublicStatus(Article article) {
        return PRESERVED_PUBLIC_STATUSES.contains(article.getPublishStatus())
                ? article.getPublishStatus()
                : "PUBLISHED";
    }

    private static void requireExpectedLock(
            Article article,
            Integer expectedLockVersion
    ) {
        if (expectedLockVersion == null || expectedLockVersion < 0) {
            throw new BusinessException(400, "必须提交有效的文章锁版本");
        }
        if (!Integer.valueOf(safeInt(article.getLockVersion()))
                .equals(expectedLockVersion)) {
            throw new BusinessException(409, "文章已在其他窗口更新，请刷新后重试");
        }
    }

    private static String normalizeIdempotencyKey(String raw) {
        String value = raw == null
                ? ""
                : Normalizer.normalize(raw.trim(), Normalizer.Form.NFKC);
        if (!IDEMPOTENCY_KEY_PATTERN.matcher(value).matches()) {
            throw new BusinessException(
                    400,
                    "幂等键须为 8-80 位字母、数字、点、下划线、冒号或连字符"
            );
        }
        return value;
    }

    private static String normalizeWithdrawReason(String raw) {
        if (raw == null || raw.isBlank()) {
            return "作者主动撤回审核";
        }
        String value = Normalizer.normalize(raw.trim(), Normalizer.Form.NFKC);
        if (value.length() > 300) {
            throw new BusinessException(400, "撤回原因不能超过 300 个字符");
        }
        return value;
    }

    private static void assertSameSubmission(
            ContentReviewTask task,
            Long articleId,
            Long userId
    ) {
        if (!sameSubmission(task, articleId, userId)) {
            throw new BusinessException(409, "幂等键已被其他审核提交占用");
        }
    }

    private static boolean sameSubmission(
            ContentReviewTask task,
            Long articleId,
            Long userId
    ) {
        return Objects.equals(task.getArticleId(), articleId)
                && Objects.equals(task.getSubmittedByUserId(), userId);
    }

    private static ArticleReviewStatusView toStatus(
            Article article,
            ContentReviewTask task
    ) {
        if (article == null) {
            throw new BusinessException(404, "文章不存在");
        }
        return new ArticleReviewStatusView(
                article.getId(),
                article.getPublishStatus(),
                article.getReviewStatus(),
                article.getCurrentVersionId(),
                article.getReviewVersionId(),
                safeInt(article.getLockVersion()),
                task == null ? null : toTaskView(task)
        );
    }

    private static ArticleReviewTaskView toTaskView(ContentReviewTask task) {
        return new ArticleReviewTaskView(
                task.getId(),
                task.getFixedVersionId(),
                task.getReviewStage(),
                task.getReviewType(),
                task.getStatus(),
                task.getRiskLevel(),
                task.getResultCode(),
                task.getResultReason(),
                safeInt(task.getLockVersion()),
                task.getSubmittedAt(),
                task.getClaimedAt(),
                task.getCompletedAt()
        );
    }

    private static int safeInt(Integer value) {
        return value == null ? 0 : value;
    }
}

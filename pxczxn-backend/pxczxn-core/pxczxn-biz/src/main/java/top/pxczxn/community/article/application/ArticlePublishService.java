package top.pxczxn.community.article.application;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import top.pxczxn.platform.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.pxczxn.community.article.model.Article;
import top.pxczxn.community.article.model.ArticleVersion;
import top.pxczxn.community.article.permission.ArticleAction;
import top.pxczxn.community.article.permission.ArticleCommunityAccess;
import top.pxczxn.community.article.permission.ArticlePermissionService;
import top.pxczxn.community.article.persistence.ArticleMapper;
import top.pxczxn.community.article.persistence.ArticleVersionMapper;
import top.pxczxn.community.blog.model.Blog;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class ArticlePublishService {

    private final ArticleMapper articleMapper;
    private final ArticleVersionMapper versionMapper;
    private final ArticlePermissionService permissionService;
    private final ArticlePublishTaskManager taskManager;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public ArticlePublishView publish(
            Long articleId,
            PublishArticleCommand command
    ) {
        if (command == null) {
            throw new BusinessException(400, "发布请求不能为空");
        }
        ArticleCommunityAccess readAccess = permissionService
                .requireCommunityArticle(articleId, ArticleAction.VIEW_EDITOR);
        Article article = readAccess.article();

        if (command.cancelScheduledRequested()) {
            return cancelScheduled(readAccess, command);
        }
        if (isCompletedPublication(article)) {
            return toView(article, article.getPublishedVersionId(), true);
        }
        if ("SCHEDULED".equals(article.getPublishMethod())) {
            return schedule(articleId, article, command);
        }
        if (command.scheduledPublishAt() != null) {
            throw new BusinessException(400, "非定时发布文章不能设置计划发布时间");
        }

        ArticleCommunityAccess access = permissionService
                .requireCommunityArticle(articleId, ArticleAction.PUBLISH);
        article = access.article();
        requireExpectedLock(article, command.expectedLockVersion());
        Long reviewVersionId = requireApprovedVersion(article);
        requireVersionExists(article, reviewVersionId);

        Blog blog = access.blog();
        String canonicalPath =
                ArticlePublicationPolicy.canonicalPath(blog, article);
        Long previousPublishedVersionId = article.getPublishedVersionId();
        LocalDateTime now = now();
        int updated = articleMapper.update(
                null,
                Wrappers.<Article>update()
                        .eq("id", article.getId())
                        .eq("lock_version", command.expectedLockVersion())
                        .eq("current_version_id", reviewVersionId)
                        .eq("review_version_id", reviewVersionId)
                        .eq("review_status", "APPROVED")
                        .isNull("deleted_at")
                        .set("published_version_id", reviewVersionId)
                        .set("publish_status", "PUBLISHED")
                        .set("canonical_path", canonicalPath)
                        .set("published_at", now)
                        .set("scheduled_publish_at", null)
                        .set("updated_at", now)
                        .setSql("lock_version = lock_version + 1")
        );
        if (updated != 1) {
            throw publicationCollision();
        }

        applyPublishedState(article, reviewVersionId, canonicalPath, now);
        eventPublisher.publishEvent(new ArticlePublishedEvent(
                article.getId(),
                blog.getId(),
                article.getAuthorUserId(),
                previousPublishedVersionId,
                reviewVersionId,
                canonicalPath,
                now,
                "USER"
        ));
        log.info(
                "Article published: articleId={}, versionId={}, trigger={}",
                article.getId(),
                reviewVersionId,
                "USER"
        );
        return toView(article, previousPublishedVersionId, false);
    }

    private ArticlePublishView schedule(
            Long articleId,
            Article initialArticle,
            PublishArticleCommand command
    ) {
        LocalDateTime requestedAt = command.scheduledPublishAt();
        if (requestedAt == null) {
            throw new BusinessException(400, "定时发布必须设置计划发布时间");
        }
        if ("SCHEDULED".equals(initialArticle.getPublishStatus())
                && Objects.equals(
                        requestedAt,
                        initialArticle.getScheduledPublishAt()
                )) {
            return toView(
                    initialArticle,
                    initialArticle.getPublishedVersionId(),
                    true
            );
        }

        ArticleCommunityAccess access = permissionService
                .requireCommunityArticle(articleId, ArticleAction.PUBLISH);
        Article article = access.article();
        requireExpectedLock(article, command.expectedLockVersion());
        LocalDateTime now = now();
        if (!requestedAt.isAfter(now)) {
            throw new BusinessException(400, "计划发布时间必须晚于当前时间");
        }
        Long reviewVersionId = requireApprovedVersion(article);
        requireVersionExists(article, reviewVersionId);

        int updated = articleMapper.update(
                null,
                Wrappers.<Article>update()
                        .eq("id", article.getId())
                        .eq("lock_version", command.expectedLockVersion())
                        .eq("current_version_id", reviewVersionId)
                        .eq("review_version_id", reviewVersionId)
                        .eq("review_status", "APPROVED")
                        .in(
                                "publish_status",
                                "APPROVED",
                                "PUBLISHED",
                                "HIDDEN",
                                "SCHEDULED",
                                "PUBLISH_FAILED"
                        )
                        .isNull("deleted_at")
                        .set("publish_status", "SCHEDULED")
                        .set("scheduled_publish_at", requestedAt)
                        .set("updated_at", now)
                        .setSql("lock_version = lock_version + 1")
        );
        if (updated != 1) {
            throw new BusinessException(
                    409,
                    "定时发布已被修改，请刷新后重试"
            );
        }

        taskManager.replaceActiveTask(
                article,
                reviewVersionId,
                requestedAt,
                now
        );
        article.setPublishStatus("SCHEDULED");
        article.setScheduledPublishAt(requestedAt);
        article.setUpdatedAt(now);
        article.setLockVersion(safeInt(article.getLockVersion()) + 1);
        log.info(
                "Article publication scheduled: articleId={}, versionId={}, scheduledAt={}",
                article.getId(),
                reviewVersionId,
                requestedAt
        );
        return toView(article, article.getPublishedVersionId(), false);
    }

    private ArticlePublishView cancelScheduled(
            ArticleCommunityAccess access,
            PublishArticleCommand command
    ) {
        Article article = access.article();
        if (!"SCHEDULED".equals(article.getPublishMethod())) {
            throw new BusinessException(409, "该文章不是定时发布文章");
        }
        if (!"SCHEDULED".equals(article.getPublishStatus())
                && article.getScheduledPublishAt() == null) {
            return toView(article, article.getPublishedVersionId(), true);
        }
        requireExpectedLock(article, command.expectedLockVersion());
        String restoredStatus =
                article.getPublishedVersionId() == null
                        ? "APPROVED"
                        : "PUBLISHED";
        LocalDateTime now = now();
        int updated = articleMapper.update(
                null,
                Wrappers.<Article>update()
                        .eq("id", article.getId())
                        .eq("lock_version", command.expectedLockVersion())
                        .eq("publish_status", "SCHEDULED")
                        .isNull("deleted_at")
                        .set("publish_status", restoredStatus)
                        .set("scheduled_publish_at", null)
                        .set("updated_at", now)
                        .setSql("lock_version = lock_version + 1")
        );
        if (updated != 1) {
            throw new BusinessException(
                    409,
                    "定时发布已执行或被修改，请刷新后重试"
            );
        }

        taskManager.cancelActiveTask(
                article.getId(),
                "USER_CANCELLED",
                "Scheduled publication was cancelled",
                now
        );
        article.setPublishStatus(restoredStatus);
        article.setScheduledPublishAt(null);
        article.setUpdatedAt(now);
        article.setLockVersion(safeInt(article.getLockVersion()) + 1);
        log.info(
                "Article scheduled publication cancelled: articleId={}",
                article.getId()
        );
        return toView(article, article.getPublishedVersionId(), false);
    }

    private Long requireApprovedVersion(Article article) {
        Long reviewVersionId = article.getReviewVersionId();
        if (reviewVersionId == null
                || !Objects.equals(
                        reviewVersionId,
                        article.getCurrentVersionId()
                )) {
            throw new BusinessException(409, "审核版本与当前版本不一致");
        }
        return reviewVersionId;
    }

    private void requireVersionExists(Article article, Long versionId) {
        ArticleVersion version = versionMapper.selectById(versionId);
        if (version == null
                || !Objects.equals(version.getArticleId(), article.getId())) {
            throw new BusinessException(409, "审核通过的文章版本不存在");
        }
    }

    private static boolean isCompletedPublication(Article article) {
        return article != null
                && "PUBLISHED".equals(article.getPublishStatus())
                && article.getReviewVersionId() != null
                && Objects.equals(
                        article.getPublishedVersionId(),
                        article.getReviewVersionId()
                )
                && article.getCanonicalPath() != null
                && !article.getCanonicalPath().isBlank()
                && article.getPublishedAt() != null;
    }

    private static void requireExpectedLock(
            Article article,
            Integer expectedLockVersion
    ) {
        if (expectedLockVersion == null || expectedLockVersion < 0) {
            throw new BusinessException(400, "必须提交有效的文章锁版本");
        }
        if (safeInt(article.getLockVersion()) != expectedLockVersion) {
            throw new BusinessException(
                    409,
                    "文章已在其他窗口更新，请刷新后重试"
            );
        }
    }

    private static void applyPublishedState(
            Article article,
            Long versionId,
            String canonicalPath,
            LocalDateTime now
    ) {
        article.setPublishedVersionId(versionId);
        article.setPublishStatus("PUBLISHED");
        article.setCanonicalPath(canonicalPath);
        article.setPublishedAt(now);
        article.setScheduledPublishAt(null);
        article.setUpdatedAt(now);
        article.setLockVersion(safeInt(article.getLockVersion()) + 1);
    }

    private static BusinessException publicationCollision() {
        return new BusinessException(
                409,
                "文章已在其他窗口发布或更新，请刷新后重试"
        );
    }

    private static ArticlePublishView toView(
            Article article,
            Long previousPublishedVersionId,
            boolean idempotentReplay
    ) {
        return new ArticlePublishView(
                article.getId(),
                previousPublishedVersionId,
                article.getPublishedVersionId(),
                article.getPublishStatus(),
                article.getReviewStatus(),
                article.getCanonicalPath(),
                article.getScheduledPublishAt(),
                article.getPublishedAt(),
                safeInt(article.getLockVersion()),
                idempotentReplay
        );
    }

    private static LocalDateTime now() {
        return LocalDateTime.now(ZoneOffset.UTC);
    }

    private static int safeInt(Integer value) {
        return value == null ? 0 : value;
    }
}

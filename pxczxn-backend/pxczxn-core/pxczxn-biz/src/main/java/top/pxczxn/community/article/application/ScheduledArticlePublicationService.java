package top.pxczxn.community.article.application;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import top.pxczxn.community.article.model.Article;
import top.pxczxn.community.article.model.ArticlePublishTask;
import top.pxczxn.community.article.model.ArticleVersion;
import top.pxczxn.community.article.persistence.ArticleMapper;
import top.pxczxn.community.article.persistence.ArticlePublishTaskMapper;
import top.pxczxn.community.article.persistence.ArticleVersionMapper;
import top.pxczxn.community.blog.model.Blog;
import top.pxczxn.community.blog.persistence.BlogMapper;
import top.pxczxn.community.user.model.CommunityUser;
import top.pxczxn.community.user.persistence.CommunityUserMapper;

import java.time.LocalDateTime;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduledArticlePublicationService {

    private final ArticlePublishTaskMapper taskMapper;
    private final ArticleMapper articleMapper;
    private final ArticleVersionMapper versionMapper;
    private final BlogMapper blogMapper;
    private final CommunityUserMapper userMapper;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ScheduledPublishResult publishClaimed(
            Long taskId,
            LocalDateTime now
    ) {
        ArticlePublishTask task = taskMapper.selectById(taskId);
        if (task == null || !"RUNNING".equals(task.getStatus())) {
            return result(task, taskId, "SKIPPED", "TASK_NOT_RUNNING");
        }
        Article article = articleMapper.selectById(task.getArticleId());
        if (!isCurrentSchedule(article, task)) {
            completeTask(
                    task,
                    "CANCELLED",
                    "STATE_CHANGED",
                    "Article schedule no longer matches this task",
                    now
            );
            return result(task, taskId, "CANCELLED", "STATE_CHANGED");
        }
        if (task.getScheduledPublishAt().isAfter(now)) {
            return retryWithoutError(task, now);
        }

        String failureCode = validate(article, task);
        if (failureCode != null) {
            return failPermanently(article, task, failureCode, now);
        }

        ArticleVersion version =
                versionMapper.selectById(task.getArticleVersionId());
        if (version == null
                || !Objects.equals(
                        version.getArticleId(),
                        article.getId()
                )) {
            return failPermanently(
                    article,
                    task,
                    "VERSION_NOT_FOUND",
                    now
            );
        }
        Blog blog = blogMapper.selectById(article.getBlogId());
        if (blog == null
                || blog.getDeletedAt() != null
                || !"ACTIVE".equals(blog.getStatus())) {
            return failPermanently(
                    article,
                    task,
                    "BLOG_NOT_PUBLISHABLE",
                    now
            );
        }
        CommunityUser author =
                userMapper.selectById(article.getAuthorUserId());
        if (author == null || !"NORMAL".equals(author.getStatus())) {
            return failPermanently(
                    article,
                    task,
                    "AUTHOR_NOT_PUBLISHABLE",
                    now
            );
        }

        String canonicalPath =
                ArticlePublicationPolicy.canonicalPath(blog, article);
        Long previousPublishedVersionId = article.getPublishedVersionId();
        int articleUpdated = articleMapper.update(
                null,
                Wrappers.<Article>update()
                        .eq("id", article.getId())
                        .eq("lock_version", safeInt(article.getLockVersion()))
                        .eq("publish_method", "SCHEDULED")
                        .eq("publish_status", "SCHEDULED")
                        .eq("review_status", "APPROVED")
                        .eq(
                                "current_version_id",
                                task.getArticleVersionId()
                        )
                        .eq(
                                "review_version_id",
                                task.getArticleVersionId()
                        )
                        .eq(
                                "scheduled_publish_at",
                                task.getScheduledPublishAt()
                        )
                        .le("scheduled_publish_at", now)
                        .isNull("deleted_at")
                        .set(
                                "published_version_id",
                                task.getArticleVersionId()
                        )
                        .set("publish_status", "PUBLISHED")
                        .set("canonical_path", canonicalPath)
                        .set("published_at", now)
                        .set("scheduled_publish_at", null)
                        .set("updated_at", now)
                        .setSql("lock_version = lock_version + 1")
        );
        if (articleUpdated != 1) {
            Article latest = articleMapper.selectById(article.getId());
            if (isAlreadyPublished(latest, task)) {
                completeTask(
                        task,
                        "SUCCEEDED",
                        null,
                        null,
                        now
                );
                return result(
                        task,
                        taskId,
                        "IDEMPOTENT",
                        "ALREADY_PUBLISHED"
                );
            }
            completeTask(
                    task,
                    "CANCELLED",
                    "STATE_CHANGED",
                    "Article state changed before publication",
                    now
            );
            return result(task, taskId, "CANCELLED", "STATE_CHANGED");
        }

        completeTask(task, "SUCCEEDED", null, null, now);
        eventPublisher.publishEvent(new ArticlePublishedEvent(
                article.getId(),
                blog.getId(),
                article.getAuthorUserId(),
                previousPublishedVersionId,
                task.getArticleVersionId(),
                canonicalPath,
                now,
                "QUARTZ"
        ));
        log.info(
                "Scheduled article published: taskId={}, articleId={}, versionId={}, trigger={}",
                task.getId(),
                article.getId(),
                task.getArticleVersionId(),
                "QUARTZ"
        );
        return result(task, taskId, "PUBLISHED", null);
    }

    private String validate(
            Article article,
            ArticlePublishTask task
    ) {
        if (!"SCHEDULED".equals(article.getPublishMethod())) {
            return "PUBLISH_METHOD_CHANGED";
        }
        if (!"APPROVED".equals(article.getReviewStatus())) {
            return "REVIEW_NOT_APPROVED";
        }
        if (!Objects.equals(
                article.getCurrentVersionId(),
                task.getArticleVersionId()
        ) || !Objects.equals(
                article.getReviewVersionId(),
                task.getArticleVersionId()
        )) {
            return "VERSION_CHANGED";
        }
        return null;
    }

    private ScheduledPublishResult failPermanently(
            Article article,
            ArticlePublishTask task,
            String reasonCode,
            LocalDateTime now
    ) {
        int updated = articleMapper.update(
                null,
                Wrappers.<Article>update()
                        .eq("id", article.getId())
                        .eq("lock_version", safeInt(article.getLockVersion()))
                        .eq("publish_status", "SCHEDULED")
                        .eq(
                                "scheduled_publish_at",
                                task.getScheduledPublishAt()
                        )
                        .isNull("deleted_at")
                        .set("publish_status", "PUBLISH_FAILED")
                        .set("scheduled_publish_at", null)
                        .set("updated_at", now)
                        .setSql("lock_version = lock_version + 1")
        );
        if (updated != 1) {
            completeTask(
                    task,
                    "CANCELLED",
                    "STATE_CHANGED",
                    "Article state changed while recording failure",
                    now
            );
            return result(
                    task,
                    task.getId(),
                    "CANCELLED",
                    "STATE_CHANGED"
            );
        }
        completeTask(
                task,
                "FAILED",
                reasonCode,
                failureMessage(reasonCode),
                now
        );
        log.warn(
                "Scheduled publication business failure: taskId={}, articleId={}, reasonCode={}",
                task.getId(),
                task.getArticleId(),
                reasonCode
        );
        return result(task, task.getId(), "FAILED", reasonCode);
    }

    private ScheduledPublishResult retryWithoutError(
            ArticlePublishTask task,
            LocalDateTime now
    ) {
        int updated = taskMapper.update(
                null,
                Wrappers.<ArticlePublishTask>update()
                        .eq("id", task.getId())
                        .eq("status", "RUNNING")
                        .eq("lock_version", safeInt(task.getLockVersion()))
                        .set("status", "WAITING")
                        .set("next_attempt_at", task.getScheduledPublishAt())
                        .set("updated_at", now)
                        .setSql("lock_version = lock_version + 1")
        );
        return result(
                task,
                task.getId(),
                updated == 1 ? "WAITING" : "SKIPPED",
                updated == 1 ? "NOT_DUE" : "TASK_COLLISION"
        );
    }

    private void completeTask(
            ArticlePublishTask task,
            String status,
            String reasonCode,
            String reasonMessage,
            LocalDateTime now
    ) {
        int updated = taskMapper.update(
                null,
                Wrappers.<ArticlePublishTask>update()
                        .eq("id", task.getId())
                        .eq("status", "RUNNING")
                        .eq("lock_version", safeInt(task.getLockVersion()))
                        .set("status", status)
                        .set("last_error_code", reasonCode)
                        .set("last_error_message", reasonMessage)
                        .set("completed_at", now)
                        .set("updated_at", now)
                        .setSql("lock_version = lock_version + 1")
        );
        if (updated != 1) {
            throw new IllegalStateException(
                    "定时发布任务状态冲突"
            );
        }
    }

    private static boolean isCurrentSchedule(
            Article article,
            ArticlePublishTask task
    ) {
        return article != null
                && article.getDeletedAt() == null
                && "SCHEDULED".equals(article.getPublishStatus())
                && Objects.equals(
                        article.getScheduledPublishAt(),
                        task.getScheduledPublishAt()
                );
    }

    private static boolean isAlreadyPublished(
            Article article,
            ArticlePublishTask task
    ) {
        return article != null
                && "PUBLISHED".equals(article.getPublishStatus())
                && Objects.equals(
                        article.getPublishedVersionId(),
                        task.getArticleVersionId()
                )
                && article.getPublishedAt() != null;
    }

    private static String failureMessage(String code) {
        return switch (code) {
            case "PUBLISH_METHOD_CHANGED" ->
                    "Article publish method is no longer scheduled";
            case "REVIEW_NOT_APPROVED" ->
                    "Article review is no longer approved";
            case "VERSION_CHANGED" ->
                    "Approved article version changed";
            case "VERSION_NOT_FOUND" ->
                    "Approved article version is unavailable";
            case "BLOG_NOT_PUBLISHABLE" ->
                    "Blog is not publishable";
            case "AUTHOR_NOT_PUBLISHABLE" ->
                    "Author is not publishable";
            default -> "Scheduled publication failed validation";
        };
    }

    private static ScheduledPublishResult result(
            ArticlePublishTask task,
            Long taskId,
            String status,
            String reasonCode
    ) {
        return new ScheduledPublishResult(
                taskId,
                task == null ? null : task.getArticleId(),
                status,
                reasonCode
        );
    }

    private static int safeInt(Integer value) {
        return value == null ? 0 : value;
    }
}

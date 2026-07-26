package top.pxczxn.community.article.application;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import top.pxczxn.community.article.model.Article;
import top.pxczxn.community.article.model.ArticlePublishTask;
import top.pxczxn.community.article.persistence.ArticleMapper;
import top.pxczxn.community.article.persistence.ArticlePublishTaskMapper;

import java.time.LocalDateTime;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduledArticleTaskStateService {

    private static final int STALE_RUNNING_MINUTES = 5;

    private final ArticlePublishTaskMapper taskMapper;
    private final ArticleMapper articleMapper;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ArticlePublishTask claim(Long taskId, LocalDateTime now) {
        ArticlePublishTask task = taskMapper.selectById(taskId);
        if (task == null || !isRunnable(task, now)) {
            return null;
        }
        int nextAttemptCount = "RUNNING".equals(task.getStatus())
                ? safeInt(task.getAttemptCount())
                : safeInt(task.getAttemptCount()) + 1;

        var update = Wrappers.<ArticlePublishTask>update()
                .eq("id", task.getId())
                .eq("status", task.getStatus())
                .eq("lock_version", safeInt(task.getLockVersion()));
        if ("RUNNING".equals(task.getStatus())) {
            update.le("updated_at", now.minusMinutes(STALE_RUNNING_MINUTES));
        } else {
            update.le("next_attempt_at", now);
        }
        update.set("status", "RUNNING")
                .set("attempt_count", nextAttemptCount)
                .set("last_error_code", null)
                .set("last_error_message", null)
                .set("updated_at", now)
                .setSql("lock_version = lock_version + 1");

        if (taskMapper.update(null, update) != 1) {
            return null;
        }
        task.setStatus("RUNNING");
        task.setAttemptCount(nextAttemptCount);
        task.setLastErrorCode(null);
        task.setLastErrorMessage(null);
        task.setUpdatedAt(now);
        task.setLockVersion(safeInt(task.getLockVersion()) + 1);
        return task;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ScheduledPublishResult recordTransientFailure(
            Long taskId,
            LocalDateTime now
    ) {
        ArticlePublishTask task = taskMapper.selectById(taskId);
        if (task == null || !"RUNNING".equals(task.getStatus())) {
            return new ScheduledPublishResult(
                    taskId,
                    task == null ? null : task.getArticleId(),
                    "SKIPPED",
                    "TASK_NOT_RUNNING"
            );
        }
        Article article = articleMapper.selectById(task.getArticleId());
        if (!isCurrentSchedule(article, task)) {
            completeTask(
                    task,
                    "CANCELLED",
                    "STATE_CHANGED",
                    "Article schedule changed while retrying",
                    now
            );
            return result(task, "CANCELLED", "STATE_CHANGED");
        }

        int attempts = safeInt(task.getAttemptCount());
        int maxAttempts = Math.max(1, safeInt(task.getMaxAttempts()));
        if (attempts < maxAttempts) {
            LocalDateTime retryAt = now.plusMinutes(
                    Math.min(15, 1L << Math.max(0, attempts - 1))
            );
            int updated = taskMapper.update(
                    null,
                    Wrappers.<ArticlePublishTask>update()
                            .eq("id", task.getId())
                            .eq("status", "RUNNING")
                            .eq(
                                    "lock_version",
                                    safeInt(task.getLockVersion())
                            )
                            .set("status", "RETRY_WAIT")
                            .set("next_attempt_at", retryAt)
                            .set("last_error_code", "TRANSIENT_FAILURE")
                            .set(
                                    "last_error_message",
                                    "Temporary infrastructure failure"
                            )
                            .set("updated_at", now)
                            .setSql("lock_version = lock_version + 1")
            );
            if (updated == 1) {
                log.warn(
                        "Scheduled publication retry queued: taskId={}, articleId={}, attempt={}, retryAt={}, reasonCode={}",
                        task.getId(),
                        task.getArticleId(),
                        attempts,
                        retryAt,
                        "TRANSIENT_FAILURE"
                );
                return result(task, "RETRY_WAIT", "TRANSIENT_FAILURE");
            }
            return result(task, "SKIPPED", "TASK_COLLISION");
        }

        int articleUpdated = articleMapper.update(
                null,
                Wrappers.<Article>update()
                        .eq("id", article.getId())
                        .eq("lock_version", safeInt(article.getLockVersion()))
                        .eq("publish_status", "SCHEDULED")
                        .eq("review_version_id", task.getArticleVersionId())
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
        if (articleUpdated != 1) {
            completeTask(
                    task,
                    "CANCELLED",
                    "STATE_CHANGED",
                    "Article schedule changed while finalizing retry",
                    now
            );
            return result(task, "CANCELLED", "STATE_CHANGED");
        }
        completeTask(
                task,
                "FAILED",
                "RETRY_EXHAUSTED",
                "Scheduled publication retry limit reached",
                now
        );
        log.error(
                "Scheduled publication permanently failed: taskId={}, articleId={}, attempt={}, reasonCode={}",
                task.getId(),
                task.getArticleId(),
                attempts,
                "RETRY_EXHAUSTED"
        );
        return result(task, "FAILED", "RETRY_EXHAUSTED");
    }

    private boolean isRunnable(
            ArticlePublishTask task,
            LocalDateTime now
    ) {
        if ("RUNNING".equals(task.getStatus())) {
            return task.getUpdatedAt() != null
                    && !task.getUpdatedAt().isAfter(
                            now.minusMinutes(STALE_RUNNING_MINUTES)
                    );
        }
        return ("WAITING".equals(task.getStatus())
                || "RETRY_WAIT".equals(task.getStatus()))
                && task.getNextAttemptAt() != null
                && !task.getNextAttemptAt().isAfter(now);
    }

    private boolean isCurrentSchedule(
            Article article,
            ArticlePublishTask task
    ) {
        return article != null
                && article.getDeletedAt() == null
                && "SCHEDULED".equals(article.getPublishStatus())
                && Objects.equals(
                        article.getReviewVersionId(),
                        task.getArticleVersionId()
                )
                && Objects.equals(
                        article.getScheduledPublishAt(),
                        task.getScheduledPublishAt()
                );
    }

    private void completeTask(
            ArticlePublishTask task,
            String status,
            String reasonCode,
            String reasonMessage,
            LocalDateTime now
    ) {
        taskMapper.update(
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
    }

    private static ScheduledPublishResult result(
            ArticlePublishTask task,
            String status,
            String reasonCode
    ) {
        return new ScheduledPublishResult(
                task.getId(),
                task.getArticleId(),
                status,
                reasonCode
        );
    }

    private static int safeInt(Integer value) {
        return value == null ? 0 : value;
    }
}

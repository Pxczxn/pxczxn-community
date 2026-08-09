package top.pxczxn.community.article.application;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import top.pxczxn.community.article.model.Article;
import top.pxczxn.community.article.model.ArticlePublishTask;
import top.pxczxn.community.article.persistence.ArticlePublishTaskMapper;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ArticlePublishTaskManager {

    static final int DEFAULT_MAX_ATTEMPTS = 3;

    private final ArticlePublishTaskMapper taskMapper;

    public void replaceActiveTask(
            Article article,
            Long versionId,
            LocalDateTime scheduledPublishAt,
            LocalDateTime now
    ) {
        cancelActiveTask(
                article.getId(),
                "RESCHEDULED",
                "Scheduled publication was replaced",
                now
        );

        ArticlePublishTask task = new ArticlePublishTask();
        task.setId(IdWorker.getId());
        task.setArticleId(article.getId());
        task.setArticleVersionId(versionId);
        task.setScheduledPublishAt(scheduledPublishAt);
        task.setStatus("WAITING");
        task.setAttemptCount(0);
        task.setMaxAttempts(DEFAULT_MAX_ATTEMPTS);
        task.setNextAttemptAt(scheduledPublishAt);
        task.setLockVersion(0);
        task.setCreatedAt(now);
        task.setUpdatedAt(now);
        if (taskMapper.insert(task) != 1) {
            throw new IllegalStateException("创建定时发布任务失败");
        }
    }

    public void cancelActiveTask(
            Long articleId,
            String reasonCode,
            String reasonMessage,
            LocalDateTime now
    ) {
        taskMapper.update(
                null,
                Wrappers.<ArticlePublishTask>update()
                        .eq("article_id", articleId)
                        .in("status", "WAITING", "RUNNING", "RETRY_WAIT")
                        .set("status", "CANCELLED")
                        .set("last_error_code", reasonCode)
                        .set("last_error_message", reasonMessage)
                        .set("completed_at", now)
                        .set("updated_at", now)
                        .setSql("lock_version = lock_version + 1")
        );
    }
}

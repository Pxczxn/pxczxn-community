package top.pxczxn.community.article.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import top.pxczxn.community.article.model.ArticlePublishTask;
import top.pxczxn.community.article.persistence.ArticlePublishTaskMapper;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduledArticlePublishBatchService {

    private static final int BATCH_SIZE = 100;
    private static final int STALE_RUNNING_MINUTES = 5;

    private final ArticlePublishTaskMapper taskMapper;
    private final ScheduledArticleTaskStateService taskStateService;
    private final ScheduledArticlePublicationService publicationService;

    public void runDueBatch() {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        List<ArticlePublishTask> tasks = taskMapper.selectRunnable(
                now,
                now.minusMinutes(STALE_RUNNING_MINUTES),
                BATCH_SIZE
        );
        int published = 0;
        int failed = 0;
        int retrying = 0;
        int skipped = 0;
        for (ArticlePublishTask candidate : tasks) {
            ArticlePublishTask claimed =
                    taskStateService.claim(candidate.getId(), now);
            if (claimed == null) {
                skipped++;
                continue;
            }
            try {
                ScheduledPublishResult result =
                        publicationService.publishClaimed(
                                claimed.getId(),
                                now
                        );
                switch (result.status()) {
                    case "PUBLISHED", "IDEMPOTENT" -> published++;
                    case "FAILED" -> failed++;
                    default -> skipped++;
                }
            } catch (RuntimeException exception) {
                log.error(
                        "Scheduled publication transient failure: taskId={}, articleId={}, reasonCode={}",
                        claimed.getId(),
                        claimed.getArticleId(),
                        "TRANSIENT_FAILURE",
                        exception
                );
                ScheduledPublishResult retryResult =
                        taskStateService.recordTransientFailure(
                                claimed.getId(),
                                now
                        );
                if ("RETRY_WAIT".equals(retryResult.status())) {
                    retrying++;
                } else if ("FAILED".equals(retryResult.status())) {
                    failed++;
                } else {
                    skipped++;
                }
            }
        }
        log.info(
                "Scheduled publication batch completed: selected={}, published={}, retrying={}, failed={}, skipped={}",
                tasks.size(),
                published,
                retrying,
                failed,
                skipped
        );
    }
}

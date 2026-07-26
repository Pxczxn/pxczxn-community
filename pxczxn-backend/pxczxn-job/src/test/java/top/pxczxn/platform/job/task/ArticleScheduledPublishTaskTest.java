package top.pxczxn.platform.job.task;

import org.junit.jupiter.api.Test;
import top.pxczxn.community.article.application.ScheduledArticlePublishBatchService;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ArticleScheduledPublishTaskTest {

    @Test
    void quartzTaskDelegatesToBusinessBatch() {
        ScheduledArticlePublishBatchService batchService =
                mock(ScheduledArticlePublishBatchService.class);
        ArticleScheduledPublishTask task =
                new ArticleScheduledPublishTask(batchService);

        task.runDueBatch();

        verify(batchService).runDueBatch();
    }
}

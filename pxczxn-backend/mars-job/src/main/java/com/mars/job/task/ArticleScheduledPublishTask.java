package com.mars.job.task;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import top.pxczxn.community.article.application.ScheduledArticlePublishBatchService;

@Slf4j
@Component("articleScheduledPublishTask")
@RequiredArgsConstructor
public class ArticleScheduledPublishTask {

    private final ScheduledArticlePublishBatchService batchService;

    public void runDueBatch() {
        log.debug("Starting scheduled article publication batch");
        batchService.runDueBatch();
    }
}

package top.pxczxn.community.article.application;

import java.time.LocalDateTime;

public record PublishArticleCommand(
        Integer expectedLockVersion,
        LocalDateTime scheduledPublishAt,
        Boolean cancelScheduled
) {

    public PublishArticleCommand(Integer expectedLockVersion) {
        this(expectedLockVersion, null, false);
    }

    public boolean cancelScheduledRequested() {
        return Boolean.TRUE.equals(cancelScheduled);
    }
}

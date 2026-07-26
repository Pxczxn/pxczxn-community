package top.pxczxn.community.web.article;

import java.time.LocalDateTime;

public record PublishArticleRequest(
        Integer expectedLockVersion,
        LocalDateTime scheduledPublishAt,
        Boolean cancelScheduled
) {

    public PublishArticleRequest(Integer expectedLockVersion) {
        this(expectedLockVersion, null, false);
    }
}

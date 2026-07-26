package top.pxczxn.community.article.application;

public record ScheduledPublishResult(
        Long taskId,
        Long articleId,
        String status,
        String reasonCode
) {
}

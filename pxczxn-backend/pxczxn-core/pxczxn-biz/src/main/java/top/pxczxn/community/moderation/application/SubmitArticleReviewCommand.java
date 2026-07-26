package top.pxczxn.community.moderation.application;

public record SubmitArticleReviewCommand(
        String idempotencyKey,
        Integer expectedLockVersion
) {
}

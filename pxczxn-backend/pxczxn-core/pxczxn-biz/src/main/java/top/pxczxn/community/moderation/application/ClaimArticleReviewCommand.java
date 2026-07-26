package top.pxczxn.community.moderation.application;

public record ClaimArticleReviewCommand(
        Integer expectedTaskLockVersion
) {
}

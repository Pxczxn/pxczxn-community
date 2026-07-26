package top.pxczxn.community.moderation.application;

public record DecideArticleReviewCommand(
        Integer expectedTaskLockVersion,
        String reason
) {
}

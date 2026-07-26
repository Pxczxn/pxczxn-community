package top.pxczxn.community.moderation.application;

public record WithdrawArticleReviewCommand(
        Integer expectedLockVersion,
        String reason
) {
}

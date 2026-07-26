package top.pxczxn.community.admin.moderation;

public record DecideAdminArticleReviewRequest(
        Integer expectedTaskLockVersion,
        String reason
) {
}

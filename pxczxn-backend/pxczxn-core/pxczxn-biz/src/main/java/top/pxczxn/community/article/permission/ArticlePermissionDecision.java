package top.pxczxn.community.article.permission;

import top.pxczxn.platform.common.exception.BusinessException;

public record ArticlePermissionDecision(
        boolean allowed,
        ArticlePermissionFailure failure,
        String message
) {

    public static ArticlePermissionDecision grant() {
        return new ArticlePermissionDecision(true, null, null);
    }

    public static ArticlePermissionDecision deny(
            ArticlePermissionFailure failure,
            String message
    ) {
        if (failure == null) {
            throw new IllegalArgumentException("拒绝原因不能为空");
        }
        return new ArticlePermissionDecision(false, failure, message);
    }

    public void assertAllowed() {
        if (!allowed) {
            throw new BusinessException(failure.httpStatus(), message);
        }
    }
}

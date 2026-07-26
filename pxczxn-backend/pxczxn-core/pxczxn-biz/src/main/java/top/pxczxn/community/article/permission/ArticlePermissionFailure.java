package top.pxczxn.community.article.permission;

public enum ArticlePermissionFailure {
    UNAUTHENTICATED(401),
    FORBIDDEN(403),
    NOT_FOUND(404),
    CONFLICT(409);

    private final int httpStatus;

    ArticlePermissionFailure(int httpStatus) {
        this.httpStatus = httpStatus;
    }

    public int httpStatus() {
        return httpStatus;
    }
}

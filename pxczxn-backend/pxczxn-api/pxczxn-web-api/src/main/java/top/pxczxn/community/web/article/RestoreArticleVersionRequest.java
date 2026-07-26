package top.pxczxn.community.web.article;

public record RestoreArticleVersionRequest(
        Integer expectedLockVersion
) {
}

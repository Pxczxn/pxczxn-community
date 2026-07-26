package top.pxczxn.community.article.permission;

/**
 * Article actions guarded by the centralized permission service.
 */
public enum ArticleAction {
    VIEW_EDITOR,
    VIEW_DETAIL,
    LIST_PUBLIC,
    EDIT,
    DELETE,
    SUBMIT_REVIEW,
    WITHDRAW_REVIEW,
    PUBLISH,
    PLATFORM_REVIEW
}

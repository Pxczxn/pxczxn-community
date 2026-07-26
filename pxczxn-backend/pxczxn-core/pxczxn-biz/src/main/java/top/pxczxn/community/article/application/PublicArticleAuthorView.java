package top.pxczxn.community.article.application;

public record PublicArticleAuthorView(
        Long userId,
        String username,
        String displayName,
        String bio,
        Long avatarFileId
) {
}

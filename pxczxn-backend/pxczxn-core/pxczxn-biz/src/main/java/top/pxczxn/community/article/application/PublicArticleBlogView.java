package top.pxczxn.community.article.application;

public record PublicArticleBlogView(
        Long blogId,
        String blogType,
        String name,
        String slug,
        String summary,
        Long avatarFileId,
        Long backgroundFileId,
        String themeKey,
        String themeConfigJson
) {
}

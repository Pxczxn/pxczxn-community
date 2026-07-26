package top.pxczxn.community.blog.application;

public record PublicBlogProfile(
        Long blogId,
        String blogType,
        String name,
        String slug,
        String summary,
        Long avatarFileId,
        Long backgroundFileId,
        long articleCount,
        long followerCount,
        String ownerUsername,
        String ownerDisplayName,
        String ownerBio,
        Long ownerAvatarFileId,
        String themeKey,
        String themeConfigJson,
        String seoTitle,
        String seoDescription
) {
}

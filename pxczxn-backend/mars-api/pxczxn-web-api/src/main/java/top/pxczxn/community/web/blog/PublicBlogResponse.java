package top.pxczxn.community.web.blog;

import top.pxczxn.community.blog.application.PublicBlogProfile;

public record PublicBlogResponse(
        String blogId,
        String blogType,
        String name,
        String slug,
        String summary,
        String avatarFileId,
        String backgroundFileId,
        long articleCount,
        long followerCount,
        String ownerUsername,
        String ownerDisplayName,
        String ownerBio,
        String ownerAvatarFileId,
        String themeKey,
        String themeConfigJson,
        String seoTitle,
        String seoDescription
) {
    static PublicBlogResponse from(PublicBlogProfile profile) {
        return new PublicBlogResponse(
                profile.blogId().toString(),
                profile.blogType(),
                profile.name(),
                profile.slug(),
                profile.summary(),
                stringId(profile.avatarFileId()),
                stringId(profile.backgroundFileId()),
                profile.articleCount(),
                profile.followerCount(),
                profile.ownerUsername(),
                profile.ownerDisplayName(),
                profile.ownerBio(),
                stringId(profile.ownerAvatarFileId()),
                profile.themeKey(),
                profile.themeConfigJson(),
                profile.seoTitle(),
                profile.seoDescription()
        );
    }

    private static String stringId(Long id) {
        return id == null ? null : id.toString();
    }
}

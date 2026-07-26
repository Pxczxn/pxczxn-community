package top.pxczxn.community.web.blog;

import top.pxczxn.community.blog.application.PersonalBlogProfile;

public record PersonalBlogResponse(
        String blogId,
        String name,
        String slug,
        String summary,
        String avatarFileId,
        String backgroundFileId,
        String status,
        long articleCount,
        long followerCount,
        BlogSettingsResponse settings
) {
    static PersonalBlogResponse from(PersonalBlogProfile profile) {
        return new PersonalBlogResponse(
                profile.blogId().toString(),
                profile.name(),
                profile.slug(),
                profile.summary(),
                stringId(profile.avatarFileId()),
                stringId(profile.backgroundFileId()),
                profile.status(),
                profile.articleCount(),
                profile.followerCount(),
                BlogSettingsResponse.from(profile.settings())
        );
    }

    private static String stringId(Long id) {
        return id == null ? null : id.toString();
    }
}

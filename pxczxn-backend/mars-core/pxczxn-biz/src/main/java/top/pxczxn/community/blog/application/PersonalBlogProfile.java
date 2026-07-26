package top.pxczxn.community.blog.application;

public record PersonalBlogProfile(
        Long blogId,
        String name,
        String slug,
        String summary,
        Long avatarFileId,
        Long backgroundFileId,
        String status,
        long articleCount,
        long followerCount,
        BlogSettingsView settings
) {
}

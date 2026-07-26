package top.pxczxn.community.user.application;

public record CurrentCommunityUser(
        Long userId,
        String username,
        String displayName,
        String bio,
        Long avatarFileId,
        String email,
        String status,
        String verificationStatus,
        Long personalBlogId,
        String blogName,
        String blogSlug
) {
}

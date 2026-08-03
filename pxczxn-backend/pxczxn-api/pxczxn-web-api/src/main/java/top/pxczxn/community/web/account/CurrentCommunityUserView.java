package top.pxczxn.community.web.account;

public record CurrentCommunityUserView(
        String userId,
        String username,
        String displayName,
        String bio,
        String avatarFileId,
        String email,
        String status,
        String verificationStatus,
        boolean forcePasswordChange,
        String personalBlogId,
        String blogName,
        String blogSlug
) {
}

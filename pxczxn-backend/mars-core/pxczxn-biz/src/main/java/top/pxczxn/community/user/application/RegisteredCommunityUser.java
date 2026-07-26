package top.pxczxn.community.user.application;

public record RegisteredCommunityUser(
        Long userId,
        Long blogId,
        String username,
        String blogSlug
) {
}

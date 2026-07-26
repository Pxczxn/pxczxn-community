package top.pxczxn.community.web.auth;

public record CommunityRegistrationView(
        String userId,
        String blogId,
        String username,
        String blogSlug
) {
}

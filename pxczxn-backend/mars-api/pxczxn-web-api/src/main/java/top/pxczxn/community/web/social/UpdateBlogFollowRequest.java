package top.pxczxn.community.web.social;

public record UpdateBlogFollowRequest(
        String notificationLevel,
        Boolean specialFollow
) {
}

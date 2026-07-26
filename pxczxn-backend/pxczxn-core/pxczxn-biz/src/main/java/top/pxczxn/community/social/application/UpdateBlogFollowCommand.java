package top.pxczxn.community.social.application;

public record UpdateBlogFollowCommand(
        String notificationLevel,
        Boolean specialFollow
) {
}

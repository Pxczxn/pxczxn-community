package top.pxczxn.community.social.application;

public record BlogFollowRelationshipView(
        Long blogId,
        boolean following,
        boolean followedBy,
        boolean mutual,
        boolean specialFollow,
        String notificationLevel,
        long followerCount
) {
}

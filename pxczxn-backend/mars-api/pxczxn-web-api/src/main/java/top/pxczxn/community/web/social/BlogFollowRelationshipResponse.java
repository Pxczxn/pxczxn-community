package top.pxczxn.community.web.social;

import top.pxczxn.community.social.application.BlogFollowRelationshipView;

public record BlogFollowRelationshipResponse(
        String blogId,
        boolean following,
        boolean followedBy,
        boolean mutual,
        boolean specialFollow,
        String notificationLevel,
        long followerCount
) {

    static BlogFollowRelationshipResponse from(BlogFollowRelationshipView view) {
        return new BlogFollowRelationshipResponse(
                id(view.blogId()),
                view.following(),
                view.followedBy(),
                view.mutual(),
                view.specialFollow(),
                view.notificationLevel(),
                view.followerCount()
        );
    }

    static String id(Long value) {
        return value == null ? null : value.toString();
    }
}

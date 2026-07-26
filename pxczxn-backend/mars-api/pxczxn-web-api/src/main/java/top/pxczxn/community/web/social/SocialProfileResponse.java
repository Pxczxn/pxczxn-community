package top.pxczxn.community.web.social;

import top.pxczxn.community.social.application.SocialProfileView;

import java.time.LocalDateTime;

public record SocialProfileResponse(
        String userId,
        String username,
        String displayName,
        String bio,
        String avatarFileId,
        String blogId,
        String blogName,
        String blogSlug,
        boolean following,
        boolean followedBy,
        boolean mutual,
        boolean specialFollow,
        String notificationLevel,
        LocalDateTime followedAt
) {

    static SocialProfileResponse from(SocialProfileView view) {
        return new SocialProfileResponse(
                BlogFollowRelationshipResponse.id(view.userId()),
                view.username(),
                view.displayName(),
                view.bio(),
                BlogFollowRelationshipResponse.id(view.avatarFileId()),
                BlogFollowRelationshipResponse.id(view.blogId()),
                view.blogName(),
                view.blogSlug(),
                view.following(),
                view.followedBy(),
                view.mutual(),
                view.specialFollow(),
                view.notificationLevel(),
                view.followedAt()
        );
    }
}

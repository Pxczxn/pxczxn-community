package top.pxczxn.community.social.application;

import java.time.LocalDateTime;

public record SocialProfileView(
        Long userId,
        String username,
        String displayName,
        String bio,
        Long avatarFileId,
        Long blogId,
        String blogName,
        String blogSlug,
        boolean following,
        boolean followedBy,
        boolean mutual,
        boolean specialFollow,
        String notificationLevel,
        LocalDateTime followedAt
) {
}

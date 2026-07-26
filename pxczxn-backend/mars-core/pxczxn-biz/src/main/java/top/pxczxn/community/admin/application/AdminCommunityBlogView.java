package top.pxczxn.community.admin.application;

import java.time.LocalDateTime;

public record AdminCommunityBlogView(
        Long id,
        String blogType,
        Long ownerUserId,
        String ownerUsername,
        String name,
        String slug,
        String summary,
        String status,
        long articleCount,
        long followerCount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}

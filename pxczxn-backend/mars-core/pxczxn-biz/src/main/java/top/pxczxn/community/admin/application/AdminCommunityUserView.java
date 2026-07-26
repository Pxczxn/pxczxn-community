package top.pxczxn.community.admin.application;

import java.time.LocalDateTime;

public record AdminCommunityUserView(
        Long id,
        String username,
        String displayName,
        String email,
        String status,
        String verificationStatus,
        Long personalBlogId,
        String personalBlogName,
        LocalDateTime lastLoginAt,
        LocalDateTime createdAt
) {
}

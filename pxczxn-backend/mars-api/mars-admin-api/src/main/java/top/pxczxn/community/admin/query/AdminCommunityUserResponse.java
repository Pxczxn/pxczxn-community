package top.pxczxn.community.admin.query;

import top.pxczxn.community.admin.application.AdminCommunityUserView;

import java.time.LocalDateTime;

public record AdminCommunityUserResponse(
        String id,
        String username,
        String displayName,
        String email,
        String status,
        String verificationStatus,
        String personalBlogId,
        String personalBlogName,
        LocalDateTime lastLoginAt,
        LocalDateTime createdAt
) {

    static AdminCommunityUserResponse from(AdminCommunityUserView view) {
        return new AdminCommunityUserResponse(
                id(view.id()),
                view.username(),
                view.displayName(),
                view.email(),
                view.status(),
                view.verificationStatus(),
                id(view.personalBlogId()),
                view.personalBlogName(),
                view.lastLoginAt(),
                view.createdAt()
        );
    }

    private static String id(Long value) {
        return value == null ? null : value.toString();
    }
}

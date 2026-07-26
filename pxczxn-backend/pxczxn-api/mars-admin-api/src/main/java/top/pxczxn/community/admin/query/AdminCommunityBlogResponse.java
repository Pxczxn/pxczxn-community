package top.pxczxn.community.admin.query;

import top.pxczxn.community.admin.application.AdminCommunityBlogView;

import java.time.LocalDateTime;

public record AdminCommunityBlogResponse(
        String id,
        String blogType,
        String ownerUserId,
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

    static AdminCommunityBlogResponse from(AdminCommunityBlogView view) {
        return new AdminCommunityBlogResponse(
                id(view.id()),
                view.blogType(),
                id(view.ownerUserId()),
                view.ownerUsername(),
                view.name(),
                view.slug(),
                view.summary(),
                view.status(),
                view.articleCount(),
                view.followerCount(),
                view.createdAt(),
                view.updatedAt()
        );
    }

    private static String id(Long value) {
        return value == null ? null : value.toString();
    }
}

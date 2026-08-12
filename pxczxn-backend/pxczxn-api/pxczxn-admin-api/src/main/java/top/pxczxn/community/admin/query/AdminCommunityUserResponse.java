package top.pxczxn.community.admin.query;

import io.swagger.v3.oas.annotations.media.Schema;
import top.pxczxn.community.admin.application.AdminCommunityUserView;

import java.time.LocalDateTime;

@Schema(description = "管理端社区用户响应")
public record AdminCommunityUserResponse(
        @Schema(description = "用户ID", example = "1234567890123456789", format = "snowflake")
        String id,

        @Schema(description = "用户名", example = "zhangsan")
        String username,

        @Schema(description = "显示名称", example = "张三")
        String displayName,

        @Schema(description = "邮箱（可空）", example = "zhangsan@example.com", nullable = true)
        String email,

        @Schema(description = "账号状态", example = "NORMAL", allowableValues = {"NORMAL", "LIMITED", "FROZEN", "BANNED", "DEACTIVATED", "DELETED"})
        String status,

        @Schema(description = "认证状态", example = "VERIFIED", allowableValues = {"UNVERIFIED", "VERIFIED"})
        String verificationStatus,

        @Schema(description = "个人博客ID（可空）", example = "9876543210987654321", format = "snowflake", nullable = true)
        String personalBlogId,

        @Schema(description = "个人博客名称（可空）", example = "张三的博客", nullable = true)
        String personalBlogName,

        @Schema(description = "最后登录时间（可空）", example = "2026-08-12 10:30:45", format = "date-time", nullable = true)
        LocalDateTime lastLoginAt,

        @Schema(description = "创建时间", example = "2026-01-15 08:00:00", format = "date-time")
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

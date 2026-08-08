package top.pxczxn.community.web.social;

import jakarta.validation.constraints.NotBlank;

public record PublishMomentRequest(
        String blogId,
        @NotBlank(message = "动态类型不能为空")
        String momentType,
        String textContent,
        String linkUrl,
        String articleId,
        String repostMomentId,
        String visibility
) {
}

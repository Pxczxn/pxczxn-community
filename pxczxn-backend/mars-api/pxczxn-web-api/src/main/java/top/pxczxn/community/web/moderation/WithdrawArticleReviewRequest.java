package top.pxczxn.community.web.moderation;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record WithdrawArticleReviewRequest(
        @NotNull(message = "文章锁版本不能为空")
        @Min(value = 0, message = "文章锁版本无效")
        Integer expectedLockVersion,

        @Size(max = 300, message = "撤回原因不能超过 300 个字符")
        String reason
) {
}

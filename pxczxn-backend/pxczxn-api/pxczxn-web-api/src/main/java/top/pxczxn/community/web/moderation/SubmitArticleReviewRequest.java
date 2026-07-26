package top.pxczxn.community.web.moderation;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record SubmitArticleReviewRequest(
        @NotBlank(message = "幂等键不能为空")
        @Pattern(
                regexp = "^[A-Za-z0-9][A-Za-z0-9._:-]{7,79}$",
                message = "幂等键格式无效"
        )
        String idempotencyKey,

        @NotNull(message = "文章锁版本不能为空")
        @Min(value = 0, message = "文章锁版本无效")
        Integer expectedLockVersion
) {
}

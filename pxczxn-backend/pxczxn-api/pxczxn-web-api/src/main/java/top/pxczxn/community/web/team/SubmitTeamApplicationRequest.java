package top.pxczxn.community.web.team;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Request for submitting a team application.
 */
public record SubmitTeamApplicationRequest(
        @NotBlank(message = "团队名称不能为空")
        @Size(max = 50, message = "团队名称不能超过 50 个字符")
        String teamName,
        @NotBlank(message = "团队地址不能为空")
        @Pattern(regexp = "^[a-z][a-z0-9-]{0,30}[a-z0-9]$", message = "团队地址须为 2-32 位小写字母开头，以字母或数字结尾")
        String teamSlug,
        @Size(max = 500, message = "团队描述不能超过 500 个字符")
        String description,
        String idempotencyKey
) {
}

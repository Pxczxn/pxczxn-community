package top.pxczxn.community.web.account;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CommunityProfileUpdateRequest(
        @NotBlank(message = "显示名称不能为空")
        @Size(max = 80, message = "显示名称不能超过 80 个字符")
        String displayName,
        @Size(max = 1000, message = "个人简介不能超过 1000 个字符")
        String bio
) {
}

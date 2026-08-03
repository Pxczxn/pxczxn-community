package top.pxczxn.community.web.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CommunityRegistrationRequest(
        @NotBlank(message = "用户名不能为空")
        @Pattern(
                regexp = "^[A-Za-z0-9][A-Za-z0-9_-]{2,31}$",
                message = "用户名须为 3-32 位字母、数字、下划线或连字符"
        )
        String username,

        @NotBlank(message = "邮箱不能为空")
        @Email(message = "邮箱格式不正确")
        @Pattern(
                regexp = "^[A-Za-z0-9.!#$%&'*+/=?^_`{|}~-]+@[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?(?:\\.[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?)+$",
                message = "邮箱格式不正确"
        )
        @Size(max = 320, message = "邮箱不能超过 320 个字符")
        String email,

        @NotBlank(message = "密码不能为空")
        @Size(min = 12, max = 72, message = "密码长度须为 12-72 个字符")
        @Pattern(
                regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9\\s])\\S{12,72}$",
                message = "密码须包含大写、小写、数字和特殊字符，且不能含空格"
        )
        String password,

        @Size(max = 80, message = "显示名称不能超过 80 个字符")
        String displayName
) {
}

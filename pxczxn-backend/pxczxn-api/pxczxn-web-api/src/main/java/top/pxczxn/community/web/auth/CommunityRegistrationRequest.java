package top.pxczxn.community.web.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CommunityRegistrationRequest(
        @NotBlank(message = "个人空间地址不能为空")
        @Pattern(
                regexp = "^[A-Za-z][A-Za-z0-9_-]{0,30}[A-Za-z0-9]$",
                message = "个人空间地址须为 2-32 位，以字母开头，并以字母或数字结尾"
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

        @NotBlank(message = "姓名不能为空")
        @Size(max = 80, message = "姓名不能超过 80 个字符")
        String displayName
) {
}

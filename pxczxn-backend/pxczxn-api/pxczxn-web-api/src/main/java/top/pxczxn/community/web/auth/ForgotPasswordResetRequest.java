package top.pxczxn.community.web.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ForgotPasswordResetRequest(
        @NotBlank(message = "邮箱不能为空")
        @Email(message = "邮箱格式不正确")
        @Size(max = 320, message = "邮箱不能超过 320 个字符")
        String email,

        @NotBlank(message = "验证码不能为空")
        @Pattern(regexp = "^\\d{6}$", message = "验证码为 6 位数字")
        String code,

        @NotBlank(message = "新密码不能为空")
        @Pattern(
                regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9\\s])\\S{12,72}$",
                message = "新密码须为 12-72 位，并包含大写、小写、数字和特殊字符，且不能含空格"
        )
        String newPassword
) {
}

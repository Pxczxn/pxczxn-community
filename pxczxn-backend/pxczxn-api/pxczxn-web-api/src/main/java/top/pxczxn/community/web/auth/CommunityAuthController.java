package top.pxczxn.community.web.auth;

import top.pxczxn.platform.common.result.Result;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import top.pxczxn.community.user.application.CommunityPasswordResetService;
import top.pxczxn.community.user.application.CommunityRegistrationService;
import top.pxczxn.community.user.application.CommunityLoginCommand;
import top.pxczxn.community.user.application.CommunityLoginSession;
import top.pxczxn.community.user.application.CommunitySessionService;
import top.pxczxn.community.user.application.RegisterCommunityUserCommand;
import top.pxczxn.community.user.application.RegisteredCommunityUser;
import top.pxczxn.community.abuse.application.CommunityAbuseGuard;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
public class CommunityAuthController {

    private final CommunityRegistrationService registrationService;
    private final CommunitySessionService sessionService;
    private final CommunityPasswordResetService passwordResetService;
    private final CommunityAbuseGuard abuseGuard;
    private final CanaryRegistrationGate canaryRegistrationGate;

    @GetMapping("/check-username")
    public Result<AvailabilityView> checkUsername(
            @RequestParam
            @NotBlank(message = "个人空间地址不能为空")
            @Pattern(
                    regexp = "^[A-Za-z][A-Za-z0-9_-]{0,30}[A-Za-z0-9]$",
                    message = "个人空间地址须为 2-32 位，以字母开头，并以字母或数字结尾"
            )
            String username
    ) {
        return Result.ok(new AvailabilityView(
                registrationService.isUsernameAvailable(username)
        ));
    }

    @GetMapping("/check-email")
    public Result<AvailabilityView> checkEmail(
            @RequestParam
            @NotBlank(message = "邮箱不能为空")
            @Email(message = "邮箱格式不正确")
            @Size(max = 320, message = "邮箱不能超过 320 个字符")
            String email
    ) {
        return Result.ok(new AvailabilityView(
                registrationService.isEmailAvailable(email)
        ));
    }

    @PostMapping("/register")
    public Result<CommunityRegistrationView> register(
            HttpServletRequest servletRequest,
            @Valid @RequestBody CommunityRegistrationRequest request
    ) {
        abuseGuard.check(clientActor(servletRequest), "REGISTER", 10, 3600);
        RegisteredCommunityUser registered = canaryRegistrationGate.register(() -> registrationService.register(
                new RegisterCommunityUserCommand(request.username(), request.email(), request.password(), request.displayName())
        ));
        return Result.ok(new CommunityRegistrationView(
                registered.userId().toString(),
                registered.blogId().toString(),
                registered.username(),
                registered.blogSlug()
        ));
    }

    @PostMapping("/login")
    public Result<CommunityLoginView> login(
            HttpServletRequest servletRequest,
            @Valid @RequestBody CommunityLoginRequest request
    ) {
        abuseGuard.check(clientActor(servletRequest), "LOGIN", 30, 900);
        CommunityLoginSession session = sessionService.login(
                new CommunityLoginCommand(request.email(), request.password())
        );
        return Result.ok(new CommunityLoginView(
                session.tokenName(),
                session.tokenValue(),
                session.expiresIn(),
                session.userId().toString(),
                session.username(),
                session.forcePasswordChange()
        ));
    }

    @PostMapping("/logout")
    public Result<Void> logout() {
        sessionService.logout();
        return Result.ok();
    }

    @PostMapping("/forgot-password/send-code")
    public Result<Void> forgotPasswordSendCode(
            HttpServletRequest servletRequest,
            @Valid @RequestBody ForgotPasswordSendCodeRequest request
    ) {
        abuseGuard.check(clientActor(servletRequest), "PASSWORD_RESET_SEND", 5, 3600);
        passwordResetService.sendResetCode(request.email());
        return Result.ok();
    }

    @PostMapping("/forgot-password/reset")
    public Result<Void> forgotPasswordReset(
            HttpServletRequest servletRequest,
            @Valid @RequestBody ForgotPasswordResetRequest request
    ) {
        abuseGuard.check(clientActor(servletRequest), "PASSWORD_RESET_RESET", 10, 3600);
        passwordResetService.resetPassword(request.email(), request.code(), request.newPassword());
        return Result.ok();
    }

    private static String clientActor(HttpServletRequest request) {
        String remoteAddress = request == null ? null : request.getRemoteAddr();
        if (remoteAddress == null || remoteAddress.isBlank()) {
            return "IP:UNKNOWN";
        }
        return "IP:" + remoteAddress.trim();
    }
}

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
    private final CommunityAbuseGuard abuseGuard;

    @GetMapping("/check-username")
    public Result<AvailabilityView> checkUsername(
            @RequestParam
            @NotBlank(message = "用户名不能为空")
            @Pattern(
                    regexp = "^[A-Za-z0-9][A-Za-z0-9_-]{2,31}$",
                    message = "用户名须为 3-32 位字母、数字、下划线或连字符"
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
        RegisteredCommunityUser registered = registrationService.register(
                new RegisterCommunityUserCommand(
                        request.username(),
                        request.email(),
                        request.password(),
                        request.displayName()
                )
        );
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
                session.username()
        ));
    }

    @PostMapping("/logout")
    public Result<Void> logout() {
        sessionService.logout();
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

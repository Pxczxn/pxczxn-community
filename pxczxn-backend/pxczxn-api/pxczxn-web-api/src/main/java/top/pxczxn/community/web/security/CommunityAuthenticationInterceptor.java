package top.pxczxn.community.web.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import top.pxczxn.community.shared.auth.CommunityAuth;
import top.pxczxn.community.user.application.CommunitySessionService;
import top.pxczxn.platform.common.exception.BusinessException;

/**
 * Enforces the community Sa-Token session at the HTTP boundary.
 */
@Component
@RequiredArgsConstructor
public class CommunityAuthenticationInterceptor implements HandlerInterceptor {

    private final CommunityAuth communityAuth;
    private final CommunityPublicRoutePolicy publicRoutePolicy;
    private final CommunitySessionService sessionService;

    @Override
    public boolean preHandle(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler
    ) {
        if (!publicRoutePolicy.isPublic(request.getMethod(), request.getRequestURI())) {
            communityAuth.checkLogin();
            String accountStatus = sessionService.getCurrentUser().status();
            if ("FROZEN".equals(accountStatus) && !frozenAccountRoute(request.getRequestURI())) {
                throw new BusinessException(403, "账号已冻结，仅可查看账号措施并提交申诉");
            }
            if (sessionService.requiresPasswordChange(communityAuth.getLoginUserId())
                    && !passwordChangeRoute(request.getRequestURI())) {
                throw new BusinessException(403, "请先修改临时密码后再继续操作");
            }
        }
        return true;
    }

    private static boolean passwordChangeRoute(String path) {
        return "/api/v1/account/me".equals(path)
                || "/api/v1/account/password".equals(path)
                || "/api/v1/auth/logout".equals(path);
    }

    private static boolean frozenAccountRoute(String path) {
        return path.startsWith("/api/v1/account-enforcements")
                || "/api/v1/account/me".equals(path)
                || "/api/v1/account/password".equals(path)
                || "/api/v1/auth/logout".equals(path);
    }
}

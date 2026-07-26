package top.pxczxn.community.web.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import top.pxczxn.community.shared.auth.CommunityAuth;

/**
 * Enforces the community Sa-Token session at the HTTP boundary.
 */
@Component
@RequiredArgsConstructor
public class CommunityAuthenticationInterceptor implements HandlerInterceptor {

    private final CommunityAuth communityAuth;
    private final CommunityPublicRoutePolicy publicRoutePolicy;

    @Override
    public boolean preHandle(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler
    ) {
        if (!publicRoutePolicy.isPublic(request.getMethod(), request.getRequestURI())) {
            communityAuth.checkLogin();
        }
        return true;
    }
}

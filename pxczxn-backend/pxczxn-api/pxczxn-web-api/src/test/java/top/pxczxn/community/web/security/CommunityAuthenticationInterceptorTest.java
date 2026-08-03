package top.pxczxn.community.web.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import top.pxczxn.community.shared.auth.CommunityAuth;
import top.pxczxn.community.user.application.CommunitySessionService;
import top.pxczxn.community.user.application.CurrentCommunityUser;
import top.pxczxn.platform.common.exception.BusinessException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CommunityAuthenticationInterceptorTest {

    private CommunitySessionService sessionService;
    private HttpServletRequest request;
    private CommunityAuthenticationInterceptor interceptor;

    @BeforeEach
    void setUp() {
        CommunityAuth auth = mock(CommunityAuth.class);
        CommunityPublicRoutePolicy publicRoutes = mock(CommunityPublicRoutePolicy.class);
        sessionService = mock(CommunitySessionService.class);
        request = mock(HttpServletRequest.class);
        when(request.getMethod()).thenReturn("GET");
        when(publicRoutes.isPublic(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString())).thenReturn(false);
        when(auth.getLoginUserId()).thenReturn(100L);
        when(sessionService.requiresPasswordChange(100L)).thenReturn(false);
        when(sessionService.getCurrentUser()).thenReturn(user("FROZEN"));
        interceptor = new CommunityAuthenticationInterceptor(auth, publicRoutes, sessionService);
    }

    @Test
    void frozenUserCanOpenAccountAppealEndpoints() {
        when(request.getRequestURI()).thenReturn("/api/v1/account-enforcements/me");

        assertThat(interceptor.preHandle(request, mock(HttpServletResponse.class), new Object())).isTrue();
    }

    @Test
    void frozenUserCannotUseOtherCommunityEndpoints() {
        when(request.getRequestURI()).thenReturn("/api/v1/articles/me");

        assertThatThrownBy(() -> interceptor.preHandle(request, mock(HttpServletResponse.class), new Object()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("仅可查看账号措施并提交申诉");
    }

    private static CurrentCommunityUser user(String status) {
        return new CurrentCommunityUser(100L, "alice", "Alice", null, null, "alice@example.com",
                status, "UNVERIFIED", false, null, null, null);
    }
}

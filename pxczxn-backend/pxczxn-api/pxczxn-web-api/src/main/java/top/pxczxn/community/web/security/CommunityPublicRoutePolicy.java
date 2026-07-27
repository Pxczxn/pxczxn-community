package top.pxczxn.community.web.security;

import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;

import java.util.List;

/**
 * The intentionally public portion of the community API.
 *
 * <p>Every route not listed here requires a community session before its
 * controller or service executes. Resource ownership and visibility checks
 * remain in the application services.</p>
 */
@Component
public class CommunityPublicRoutePolicy {

    private static final List<String> PUBLIC_GET_PATTERNS = List.of(
            "/api/v1/health",
            "/api/v1/auth/check-username",
            "/api/v1/auth/check-email",
            "/api/v1/public/**",
            "/api/v1/tags",
            "/api/v1/series",
            "/api/v1/series/*",
            "/api/v1/interactions/*/*/comments",
            "/api/v1/comments/*/replies",
            "/api/v1/interactions/*/*/like",
            "/api/v1/users/*/likes",
            "/api/v1/users/*/favorite-folders",
            "/api/v1/favorite-folders/*/items",
            "/api/v1/interactions/*/*/favoritors",
            "/api/v1/moments",
            "/api/v1/moments/*",
            "/api/v1/blogs/*/moments"
    );

    private static final List<String> PUBLIC_POST_PATTERNS = List.of(
            "/api/v1/auth/register",
            "/api/v1/auth/login",
            "/api/v1/moments/*/share-link"
    );

    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    public boolean isPublic(String method, String path) {
        if (HttpMethod.OPTIONS.matches(method)) {
            return true;
        }
        if (HttpMethod.GET.matches(method)) {
            return matchesAny(PUBLIC_GET_PATTERNS, path);
        }
        if (HttpMethod.POST.matches(method)) {
            return matchesAny(PUBLIC_POST_PATTERNS, path);
        }
        return false;
    }

    private boolean matchesAny(List<String> patterns, String path) {
        return patterns.stream().anyMatch(pattern -> pathMatcher.match(pattern, path));
    }
}

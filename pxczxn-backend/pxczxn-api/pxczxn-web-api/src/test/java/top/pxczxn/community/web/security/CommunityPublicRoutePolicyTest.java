package top.pxczxn.community.web.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CommunityPublicRoutePolicyTest {

    private final CommunityPublicRoutePolicy policy = new CommunityPublicRoutePolicy();

    @Test
    void permitsOnlyExplicitPublicReadsAndAuthenticationEntryPoints() {
        assertThat(policy.isPublic("GET", "/api/v1/health")).isTrue();
        assertThat(policy.isPublic("GET", "/api/v1/public/articles/42")).isTrue();
        assertThat(policy.isPublic("GET", "/api/v1/series")).isTrue();
        assertThat(policy.isPublic("GET", "/api/v1/series/42")).isTrue();
        assertThat(policy.isPublic("GET", "/api/v1/teams")).isTrue();
        assertThat(policy.isPublic("GET", "/api/v1/teams/slug/team-a")).isTrue();
        assertThat(policy.isPublic("POST", "/api/v1/series")).isFalse();
        assertThat(policy.isPublic("GET", "/api/v1/interactions/ARTICLE/42/comments")).isTrue();
        assertThat(policy.isPublic("GET", "/api/v1/moments/42")).isTrue();
        assertThat(policy.isPublic("POST", "/api/v1/auth/login")).isTrue();
        assertThat(policy.isPublic("POST", "/api/v1/moments/42/share-link")).isTrue();
        assertThat(policy.isPublic("OPTIONS", "/api/v1/articles")).isTrue();
    }

    @Test
    void requiresAuthenticationForMutationsAndPrivateReads() {
        assertThat(policy.isPublic("POST", "/api/v1/moments")).isFalse();
        assertThat(policy.isPublic("POST", "/api/v1/moments/42/share")).isFalse();
        assertThat(policy.isPublic("DELETE", "/api/v1/comments/42")).isFalse();
        assertThat(policy.isPublic("GET", "/api/v1/account/me")).isFalse();
        assertThat(policy.isPublic("GET", "/api/v1/social/me/following")).isFalse();
        assertThat(policy.isPublic("GET", "/api/v1/articles/42/editor")).isFalse();
        assertThat(policy.isPublic("GET", "/api/v1/teams/42/workspace")).isFalse();
        assertThat(policy.isPublic("POST", "/api/v1/auth/logout")).isFalse();
    }
}

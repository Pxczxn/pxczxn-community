package top.pxczxn.community.web.auth;

public record CommunityLoginView(
        String tokenName,
        String tokenValue,
        long expiresIn,
        String userId,
        String username,
        boolean forcePasswordChange
) {
}

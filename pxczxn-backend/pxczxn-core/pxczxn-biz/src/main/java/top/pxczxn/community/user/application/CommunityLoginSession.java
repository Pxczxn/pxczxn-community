package top.pxczxn.community.user.application;

public record CommunityLoginSession(
        String tokenName,
        String tokenValue,
        long expiresIn,
        Long userId,
        String username
) {
}

package top.pxczxn.community.user.application;

public record RegisterCommunityUserCommand(
        String username,
        String email,
        String password,
        String displayName
) {
}

package top.pxczxn.community.user.application;

public record CommunityLoginCommand(String email, String password, Boolean rememberMe) {
}
